package com.monsanto.arch.awsutil

import java.util.Date

import com.amazonaws.auth.{policy ⇒ awsauth}
import com.monsanto.arch.awsutil.identitymanagement.model.{CreateRoleRequest, Role, User}
import com.monsanto.arch.awsutil.securitytoken.model.{AssumeRoleRequest, AssumeRoleResult, AssumedRoleUser, Credentials}
import com.monsanto.arch.awsutil.util.AwsEnumeration
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Shrink.shrink
import org.scalacheck.{Arbitrary, Gen, Shrink}

import scala.collection.JavaConverters._
import scala.concurrent.duration.{DurationInt, FiniteDuration}

/** A suite of generators for various types of AWS types. */
object AwsGen {
  /** Wrapper for an AWS account identifier. */
  case class Account(value: String) {
    override def toString = value
  }

  object Account {
    implicit lazy val arbAccount: Arbitrary[Account] =
      Arbitrary {
        for(n ← Gen.choose(100000000000L, 999999999999L)) yield Account(n.toString)
      }
  }

  /** Base type for all ARN types. */
  abstract class Arn(val value: String) {
    override def toString = value
  }

  /** Wrapper for AWS policy. */
  case class Policy(id: Option[Policy.Id],
                    statements: Seq[Policy.Statement]) {
    /** Returns the AWS model object for this policy. */
    def toAws: awsauth.Policy = {
      val policy = new awsauth.Policy()
      id.foreach(id ⇒ policy.setId(id.id))
      policy.setStatements(statements.map(_.toAws).asJavaCollection)
      policy
    }

    /** Generates the JSON serialisation of this policy. */
    def toJson: String = toAws.toJson
  }

  object Policy {
    /** Wraps a policy ID value. */
    case class Id(id: String)
    object Id {
      implicit lazy val arbId: Arbitrary[Id] = Arbitrary(stringOf(extendedWordChar, 1, 64).map(Id.apply))

      implicit lazy val shrinkId: Shrink[Id] =
        Shrink { id ⇒
          shrink(id.id).filter(_.nonEmpty).map(Id.apply)
        }
    }

    /** Wraps a policy statement value.
      *
      * TODO: support principals, actions, resources, and conditions
      */
    case class Statement(id: Option[Statement.Id],
                         principals: Seq[Statement.Principal],
                         effect: Statement.Effect,
                         actions: Seq[Statement.Action],
                         resources: Seq[Statement.Resource],
                         conditions: Seq[Statement.Condition]) {
      def toAws: awsauth.Statement = {
        val statement = new awsauth.Statement(effect.toAws)
        id.foreach(id ⇒ statement.setId(id.value))
        statement.setPrincipals(principals.map(_.toAws).asJavaCollection)
        statement.setActions(actions.map(_.toAws).asJavaCollection)
        statement.setResources(resources.map(_.toAws).asJavaCollection)
        statement.setConditions(conditions.map(_.toAws).asJava)
        statement
      }
    }

    object Statement {
      /** Enumerated type for a statement effect. */
      sealed abstract class Effect(val toAws: awsauth.Statement.Effect) extends AwsEnumeration[awsauth.Statement.Effect]
      object Effect {
        case object Allow extends Effect(awsauth.Statement.Effect.Allow)
        case object Deny extends Effect(awsauth.Statement.Effect.Deny)

        implicit lazy val arbEffect: Arbitrary[Effect] = Arbitrary(Gen.oneOf(Allow, Deny))
      }

      /** Wrapper for a statement identifier. */
      case class Id(value: String)
      object Id {
        implicit lazy val arbId: Arbitrary[Id] = Arbitrary(stringOf(Gen.alphaNumChar, 1, 64).map(Id.apply))
        implicit lazy val shrinkId: Shrink[Id] =
          Shrink { id ⇒
            shrink(id.value).filter(_.nonEmpty).map(Id.apply)
          }
      }

      /// TODO: Placeholder traits
      trait Principal {
        def toAws: awsauth.Principal
      }
      trait Action {
        def toAws: awsauth.Action
      }
      trait Resource {
        def toAws: awsauth.Resource
      }
      trait Condition {
        def toAws: awsauth.Condition
      }

      // TODO: proper generation
      implicit lazy val arbStatement: Arbitrary[Statement] =
        Arbitrary(Gen.resultOf(Statement(_: Option[Id], Seq.empty, _: Effect, Seq.empty, Seq.empty, Seq.empty)))

      implicit lazy val shrinkStatement: Shrink[Statement] =
        Shrink.xmap((Statement.apply(_: Option[Id], Seq.empty, _: Effect, Seq.empty, Seq.empty, Seq.empty)).tupled, s ⇒ (s.id, s.effect))
    }

    implicit lazy val arbPolicy: Arbitrary[Policy] =
      Arbitrary {
        Gen.sized { n ⇒
          val maxStatements = Math.sqrt(n).toInt
          for {
            numStatements ← Gen.choose(1, maxStatements)
            statements ← Gen.listOfN(numStatements, arbitrary[Statement])
            policy ← Gen.resultOf(Policy(_: Option[Id], statements))
          } yield policy
        }
      }

    implicit lazy val shrinkPolicy: Shrink[Policy] =
      Shrink { policy ⇒
        shrink((policy.id, policy.statements))
          .map((Policy.apply _).tupled)
          .filter(_.statements.nonEmpty)
      }
  }

  /** Holds all IAM-specific types. */
  object IAM {
    /** Wraps an ARN for an account. */
    case class AccountArn(account: Account) extends Arn(s"arn:aws:iam::$account:root")

    object AccountArn {
      implicit lazy val arbAccountArn: Arbitrary[AccountArn] = Arbitrary(Gen.resultOf(AccountArn.apply _))
    }

    /** Wrapper for an IAM user or role name. */
    case class Name(value: String) {
      override def toString = value
    }

    object Name {
      implicit lazy val arbName: Arbitrary[Name] =
        Arbitrary(stringOf(extendedWordChar, 1, 64).map(Name.apply))

      implicit lazy val shrinkName: Shrink[Name] =
        Shrink { name ⇒
          shrink(name.value).filter(_.length >= 1).map(Name.apply)
        }
    }

    /** Wrapper for a role ARN. */
    case class RoleArn(account: Account, path: Path, roleName: Name) extends Arn(s"arn:aws:iam::$account:role$path$roleName}")

    object RoleArn {
      implicit lazy val arbRoleArn: Arbitrary[RoleArn] = Arbitrary(Gen.resultOf(RoleArn.apply _))

      implicit lazy val shrinkRoleArn: Shrink[RoleArn] =
        Shrink.xmap((RoleArn.apply _).tupled, RoleArn.unapply(_).get)
    }

    /** Wrapper for a SAML provider name. */
    case class SamlProviderName(value: String)

    object SamlProviderName {
      implicit lazy val arbSAMLProviderName: Arbitrary[SamlProviderName] = Arbitrary {
        val char = Gen.frequency(62 → Gen.alphaNumChar, 2 → Gen.oneOf('_', '.', '-'))
        val name = stringOf(char, 1, 128)
        name.map(SamlProviderName(_))
      }

      implicit lazy val shrinkSAMLProviderName: Shrink[SamlProviderName] =
        Shrink { samlProviderName ⇒
          shrink(samlProviderName.value).filter(_.nonEmpty).map(SamlProviderName(_))
        }
    }

    /** Type for a SAML provider ARN. */
    case class SamlProviderArn(account: Account, name: SamlProviderName) extends Arn(s"arn:aws:iam::$account:saml-provider/$name")

    object SamlProviderArn {
      implicit lazy val arbSAMLProviderArn: Arbitrary[SamlProviderArn] =
        Arbitrary(Gen.resultOf(SamlProviderArn.apply _))

      implicit lazy val shrinkSAMLProviderArn: Shrink[SamlProviderArn] =
        Shrink { arn ⇒
          for (newName ← shrink(arn.name)) yield SamlProviderArn(arn.account, newName)
        }
    }

    /** Arguments used to create a `User` object. */
    case class UserArgs(account: Account, path: Path, name: Name, id: Id, created: Date, passwordLastUsed: Option[Date]) {
      lazy val arn = UserArn(account, path, name)

      def toUser: User = User(path.value, name.value, id.value, arn.value, created, passwordLastUsed)
    }

    object UserArgs {
      implicit lazy val arbUser: Arbitrary[UserArgs] = Arbitrary(Gen.resultOf(UserArgs.apply _))

      implicit lazy val shrinkUser: Shrink[UserArgs] =
          Shrink.xmap((UserArgs.apply _).tupled, UserArgs.unapply(_).get)
    }

    /** Type for the ARN of a user in an account. */
    case class UserArn(account: Account, path: Path, user: Name) extends Arn(s"arn:aws:iam::$account:user$path$user")

    object UserArn {
      implicit lazy val arbUserArn: Arbitrary[UserArn] = Arbitrary(Gen.resultOf(UserArn.apply _))

      implicit lazy val shrinkUserArn: Shrink[UserArn] =
        Shrink.xmap((UserArn.apply _).tupled, UserArn.unapply(_).get)
    }

    /** Represents an IAM object’s path as a sequence of path elements. */
    case class Path(elements: Seq[Path.Element]) {
      val value = if (elements.isEmpty) "/" else elements.mkString("/", "/", "/")
      override val toString = value
    }

    object Path {
      /** Wrapper for a path element. */
      case class Element(value: String) {
        override val toString = value
      }

      object Element {
        implicit lazy val arbUserPathElement: Arbitrary[Element] =
          Arbitrary {
            for (value ← stringOf(wordChar, 1, 64)) yield Element(value)
          }

        implicit lazy val shrinkUserPathElement: Shrink[Element] =
          Shrink { e ⇒
            shrink(e.value).filter(_.nonEmpty).map(Element(_))
          }
      }

      implicit lazy val arbPath: Arbitrary[Path] =
        Arbitrary {
          Gen.sized { n ⇒
            val max = Math.sqrt(n).toInt
            for {
              n ← Gen.choose(0, max)
              elements ← Gen.listOfN(n, arbitrary[Element])
            } yield Path(elements)
          }.retryUntil(_.toString.length <= 512)
        }

      implicit lazy val shrinkPath: Shrink[Path] = Shrink.xmap(Path.apply, _.elements)
    }

    /** Creates arguments for building a `Role` object. */
    case class RoleArgs(account: Account,
                        name: Name,
                        path: Path,
                        id: Id,
                        assumeRolePolicyDocument: Policy,
                        created: Date) {

      /** Gets the ARN, which is derived from the account and name. */
      val arn: RoleArn = RoleArn(account, path, name)

      def toRole: Role =
        Role(arn.value, name.value, path.value, id.value, assumeRolePolicyDocument.toJson, created)
    }

    object RoleArgs {
      implicit lazy val arbRoleArgs: Arbitrary[RoleArgs] = Arbitrary(Gen.resultOf(RoleArgs.apply _))
      implicit lazy val shrinkRoleArgs: Shrink[RoleArgs] =
        Shrink.xmap((RoleArgs.apply _).tupled, RoleArgs.unapply(_).get)
    }

    case class Id(value: String)

    object Id {
      private val idChar = Gen.oneOf(('A' to 'Z') ++ ('0' to '9'))
      implicit lazy val arbId: Arbitrary[Id] = Arbitrary(stringOf(idChar, 16, 32).map(Id.apply))
      implicit lazy val shrinkId: Shrink[Id] =
        Shrink { id ⇒
          shrink(id.value).filter(_.length >= 16).map(Id.apply)
        }
    }

    case class CreateRoleRequestArgs(name: Name,
                                     assumeRolePolicy: Policy,
                                     path: Option[Path]) {
      def toRequest = CreateRoleRequest(name.value, assumeRolePolicy.toJson, path.map(_.value))
    }

    object CreateRoleRequestArgs {
      implicit lazy val arbCreateRoleRequestArgs: Arbitrary[CreateRoleRequestArgs] =
        Arbitrary(Gen.resultOf(CreateRoleRequestArgs.apply _))
      implicit lazy val shrinkCreateRoleRequestArgs: Shrink[CreateRoleRequestArgs] =
        Shrink.xmap((CreateRoleRequestArgs.apply _).tupled, CreateRoleRequestArgs.unapply(_).get)
    }

    /** Type for the ARN of a user in an account. */
    case class PolicyArn(account: Account, path: Path, name: Name) extends Arn(s"arn:aws:iam::$account:policy$path$name")

    object PolicyArn {
      implicit lazy val arbPolicyArn: Arbitrary[PolicyArn] = Arbitrary(Gen.resultOf(PolicyArn.apply _))

      implicit lazy val shrinkPolicyArn: Shrink[PolicyArn] =
        Shrink.xmap((PolicyArn.apply _).tupled, PolicyArn.unapply(_).get)
    }
  }

  /** Contains all types specific to the security token service. */
  object STS {
    /** Wraps an external identifier. */
    case class ExternalId(value: String) {
      override def toString = value
    }

    object ExternalId {
      val ExternalIdChars = WordChars ++ Seq('_', '+', '=', ',', '.', '@', ':', '\\', '/', '-')

      implicit lazy val arbExternalId: Arbitrary[ExternalId] =
        Arbitrary {
          stringOf(Gen.oneOf(ExternalIdChars), 2, 1224).map(ExternalId.apply)
        }

      implicit lazy val shrinkExternalId: Shrink[ExternalId] =
        Shrink { id ⇒
          shrink(id.value).filter(_.length > 1).map(ExternalId.apply)
        }
    }

    /** Wraps a role session name. */
    case class RoleSessionName(value: String) {
      override def toString = value
    }

    object RoleSessionName {
      implicit lazy val arbRoleSessionName: Arbitrary[RoleSessionName] =
        Arbitrary {
          stringOf(extendedWordChar, 2, 64).map(RoleSessionName.apply)
        }

      implicit lazy val shrinkRoleSessionName: Shrink[RoleSessionName] =
        Shrink { name ⇒
          shrink(name.value).filter(_.length > 1).map(RoleSessionName.apply)
        }
    }

    /** Type for the ARN of an assumed role session. */
    case class AssumedRoleArn(account: Account, roleName: IAM.Name, roleSessionName: RoleSessionName)
      extends Arn(s"arn:aws:iam::$account:assumed-role/$roleName/$roleSessionName")

    object AssumedRoleArn {
      implicit lazy val arbAssumedRoleArn: Arbitrary[AssumedRoleArn] =
        Arbitrary(Gen.resultOf(AssumedRoleArn(_: Account, _: IAM.Name, _: RoleSessionName)))

      implicit lazy val shrinkAssumedRoleArn: Shrink[AssumedRoleArn] =
        Shrink.xmap((AssumedRoleArn.apply _).tupled, AssumedRoleArn.unapply(_).get)
    }

    /** Wrapper for session durations. */
    case class SessionDuration(value: FiniteDuration)

    object SessionDuration {
      implicit lazy val arbSessionDuration: Arbitrary[SessionDuration] =
        Arbitrary {
          Gen.choose(900, 3600).map(s ⇒ SessionDuration(s.seconds))
        }
    }

    /** Generator for MFA tokens. */
    implicit val arbMFA: Arbitrary[AssumeRoleRequest.MFA] =
      Arbitrary {
        for {
          serial ← Gen.listOfN(20, Gen.alphaNumChar).map(_.mkString)
          token ← Gen.listOfN(6, Gen.numChar).map(_.mkString)
        } yield AssumeRoleRequest.MFA(serial, token)
      }

    /** Provides the arguments necessary for generating `AssumeRoleRequest` instances. */
    case class AssumeRoleRequestArgs(roleArn: IAM.RoleArn,
                                     sessionName: RoleSessionName,
                                     duration: Option[SessionDuration],
                                     externalId: Option[ExternalId],
                                     policy: Option[Policy],
                                     mfa: Option[AssumeRoleRequest.MFA]) {
      /** Creates a request from the generated arguments. */
      def toRequest: AssumeRoleRequest =
        AssumeRoleRequest(roleArn.value, sessionName.value, duration.map(_.value), externalId.map(_.value),
          policy.map(_.toJson), mfa)
    }

    object AssumeRoleRequestArgs {
      implicit lazy val arbAssumeRoleRequestArgs: Arbitrary[AssumeRoleRequestArgs] =
        Arbitrary(Gen.resultOf(AssumeRoleRequestArgs.apply _))

      implicit lazy val shrinkAssumeRoleRequestArgs: Shrink[AssumeRoleRequestArgs] =
        Shrink.xmap((AssumeRoleRequestArgs.apply _).tupled, AssumeRoleRequestArgs.unapply(_).get)
    }

    /** Provides the arguments necessary to generate a `AssumedRoleUser`. */
    case class AssumedRoleUserArgs(account: Account,
                                   roleId: IAM.Id,
                                   roleName: IAM.Name,
                                   sessionName: RoleSessionName) {
      def arn = AssumedRoleArn(account, roleName, sessionName).value
      def id = s"$roleId:$sessionName"
      def toAssumedRoleUser = AssumedRoleUser(arn, id)
    }

    object AssumedRoleUserArgs {
      implicit lazy val arbAssumedRoleUserArgs: Arbitrary[AssumedRoleUserArgs] =
        Arbitrary(Gen.resultOf(AssumedRoleUserArgs.apply _))
      implicit lazy val shrinkAssumedRoleUserArgs: Shrink[AssumedRoleUserArgs] =
        Shrink.xmap((AssumedRoleUserArgs.apply _).tupled, AssumedRoleUserArgs.unapply(_).get)
    }

    case class CredentialsArgs(accessKeyId: IAM.Id,
                               secretAccessKey: CredentialsArgs.SecretAccessKey,
                               sessionToken: CredentialsArgs.SessionToken,
                               expiration: Date) {
      def toCredentials = Credentials(accessKeyId.value, secretAccessKey.value, sessionToken.value, expiration)
    }

    object CredentialsArgs {
      implicit lazy val arbCredentialsArgs: Arbitrary[CredentialsArgs] =
        Arbitrary(Gen.resultOf(CredentialsArgs.apply _))
      implicit lazy val shrinkCredentialsArgs: Shrink[CredentialsArgs] =
        Shrink.xmap((CredentialsArgs.apply _).tupled, CredentialsArgs.unapply(_).get)


      case class SecretAccessKey(value: String)
      object SecretAccessKey {
        implicit lazy val arbSecretAccessKey: Arbitrary[SecretAccessKey] =
          Arbitrary(stringOf(extendedWordChar, 32, 48).map(SecretAccessKey.apply))
      }

      case class SessionToken(value: String)
      object SessionToken {
        implicit lazy val arbSessionToken: Arbitrary[SessionToken] =
          Arbitrary(stringOf(extendedWordChar, 64, 1024).map(SessionToken.apply))
      }
    }

    case class AssumeRoleResultArgs(assumedRoleUserArgs: AssumedRoleUserArgs,
                                    credentialsArgs: CredentialsArgs,
                                    packedPolicySize: Option[AssumeRoleResultArgs.PackedPolicySize]) {
      def assumedRoleUser: AssumedRoleUser = assumedRoleUserArgs.toAssumedRoleUser
      def credentials: Credentials = credentialsArgs.toCredentials
      def toResult: AssumeRoleResult =
        AssumeRoleResult(
          assumedRoleUserArgs.toAssumedRoleUser,
          credentialsArgs.toCredentials,
          packedPolicySize.map(_.value))
    }

    object AssumeRoleResultArgs {
      implicit lazy val arbAssumeRoleResultArgs: Arbitrary[AssumeRoleResultArgs] =
        Arbitrary(Gen.resultOf(AssumeRoleResultArgs.apply _))
      implicit lazy val shrinkAssumeRoleResultArgs: Shrink[AssumeRoleResultArgs] =
        Shrink.xmap((AssumeRoleResultArgs.apply _).tupled, AssumeRoleResultArgs.unapply(_).get)

      def forRequest(requestArgs: AssumeRoleRequestArgs): Gen[AssumeRoleResultArgs] = {
        Gen.resultOf { (roleId: IAM.Id, credentials: CredentialsArgs, packedPolicySize: Option[PackedPolicySize]) ⇒
          AwsGen.STS.AssumeRoleResultArgs(
            AwsGen.STS.AssumedRoleUserArgs(
              requestArgs.roleArn.account,
              roleId,
              requestArgs.roleArn.roleName,
              requestArgs.sessionName),
            credentials,
            packedPolicySize)
        }
      }

      case class PackedPolicySize(value: Int)
      object PackedPolicySize {
        implicit lazy val arbPackedPolicySize: Arbitrary[PackedPolicySize] =
          Arbitrary(Gen.choose(1, 100).map(PackedPolicySize.apply))
        implicit lazy val shrinkPackedPolicySize: Shrink[PackedPolicySize] =
          Shrink { packedPolicySize ⇒
            shrink(packedPolicySize.value).filter(v ⇒ (v >= 1) && (v <= 100)).map(PackedPolicySize.apply)
          }
      }
    }
  }

  /** All word characters. */
  private val WordChars = ('a' to 'z') ++ ('A' to 'Z') :+ '_'

  /** Generates a word character. */
  val wordChar: Gen[Char] = Gen.oneOf(WordChars)

  /** A generator for an extended character set. */
  val extendedWordChar: Gen[Char] = Gen.oneOf(WordChars ++ Seq('+', '=', ',', '.', '@', '-'))

  /** Creates a sized string from a character generator. */
  def stringOf(charGen: Gen[Char], minSize: Int, maxSize: Int): Gen[String] =
    Gen.sized { n ⇒
      val sizedMax = minSize + ((0.01 * (maxSize - minSize)) * n.min(100)).toInt
      for {
        n ← Gen.choose(minSize, sizedMax)
        chars ← Gen.listOfN(n, charGen)
      } yield chars.mkString
    }
}
