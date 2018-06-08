package com.monsanto.arch.awsutil.testkit

import java.util.Date

import akka.util.ByteString
import com.monsanto.arch.awsutil.auth.policy._
import com.monsanto.arch.awsutil.identitymanagement.model.{Path, RoleArn, SamlProviderArn, UserArn}
import com.monsanto.arch.awsutil.partitions.Partition
import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.securitytoken.model.AssumedRoleArn
import com.monsanto.arch.awsutil.{Account, AccountArn, Arn}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen, Shrink}

/** Provides ScalaCheck support for core ''aws2scala'' objects. */
object CoreScalaCheckImplicits {
  implicit lazy val arbPartition: Arbitrary[Partition] = Arbitrary(Gen.oneOf(Partition.values))

  implicit lazy val arbAccount: Arbitrary[Account] =
    Arbitrary{
      for {
        partition ← arbitrary[Partition]
        id ← CoreGen.accountId
      } yield Account(id, partition)
    }

  implicit lazy val arbAccountArn: Arbitrary[AccountArn] = Arbitrary(Gen.resultOf(AccountArn.apply(_: Account)))

  implicit lazy val arbArnNamespace: Arbitrary[Arn.Namespace] = Arbitrary(Gen.oneOf(Arn.Namespace.values))

  implicit lazy val arbRegion: Arbitrary[Region] = Arbitrary(Gen.oneOf(Region.values))

  implicit lazy val arbPolicy: Arbitrary[Policy] = {
    val idGen = Gen.option(Gen.nonEmptyListOf(UtilGen.asciiChar).map(_.mkString))
    Arbitrary {
      for {
        version ← arbitrary[Option[Policy.Version]]
        id ← idGen
        statements ← UtilGen.nonEmptyListOfSqrtN(arbitrary[Statement])
      } yield Policy(version, id, statements)
    }
  }

  implicit lazy val shrinkPolicy: Shrink[Policy] =
    Shrink { policy ⇒
      Shrink.shrink(policy.id).filter(_.forall(_.nonEmpty)).map(x ⇒ policy.copy(id = x)) append
        Shrink.shrink(policy.statements).filter(_.nonEmpty).map(x ⇒ policy.copy(statements = x))
    }

  implicit lazy val arbStatement: Arbitrary[Statement] =
    Arbitrary {
      for {
        id ← Gen.option(Gen.identifier)
        principals ← arbitrary[Set[Principal]]
        effect ← arbitrary[Statement.Effect]
        actions ← arbitrary[Seq[Action]]
        resources ← arbitrary[Seq[Resource]]
        conditions ← arbitrary[Set[Condition]]
      } yield Statement(id, principals, effect, actions, resources, conditions)
    }

  implicit lazy val shrinkStatement: Shrink[Statement] =
    Shrink { statement ⇒
      Shrink.shrink(statement.id).filter(_.forall(_.nonEmpty)).map(x ⇒ statement.copy(id = x)) append
        Shrink.shrink(statement.principals).map(x ⇒ statement.copy(principals = x)) append
        Shrink.shrink(statement.actions).map(x ⇒ statement.copy(actions = x)) append
        Shrink.shrink(statement.resources).map(x ⇒ statement.copy(resources = x)) append
        Shrink.shrink(statement.conditions).map(x ⇒ statement.copy(conditions = x))
    }

  implicit val arbStatements: Arbitrary[Seq[Statement]] =
    Arbitrary(UtilGen.nonEmptyListOfSqrtN(arbitrary[Statement]))

  implicit val shrinkStatements: Shrink[Seq[Statement]] =
    Shrink(statements ⇒ Shrink.shrinkContainer[Seq,Statement].shrink(statements).filter(_.nonEmpty))

  implicit lazy val arbStatementEffect: Arbitrary[Statement.Effect] =
    Arbitrary(Gen.oneOf(Statement.Effect.values))

  implicit lazy val arbPrincipal: Arbitrary[Principal] =
    Arbitrary {
      val constantPrincipals = Gen.oneOf(Principal.allServices, Principal.allWebProviders, Principal.allUsers)
      Gen.oneOf(
        constantPrincipals,
        arbitrary[Principal.AccountPrincipal],
        arbitrary[Principal.ServicePrincipal],
        arbitrary[Principal.WebProviderPrincipal],
        arbitrary[Principal.SamlProviderPrincipal],
        arbitrary[Principal.IamUserPrincipal],
        arbitrary[Principal.IamRolePrincipal],
        arbitrary[Principal.StsAssumedRolePrincipal])
    }

  implicit lazy val arbPrincipals: Arbitrary[Set[Principal]] =
    Arbitrary {
      Gen.frequency(
        1 → Gen.const(Statement.allPrincipals),
        19 → UtilGen.listOfSqrtN(arbitrary[Principal]).map(_.toSet)
      )
    }

  implicit lazy val arbAccountPrincipal: Arbitrary[Principal.AccountPrincipal] =
    Arbitrary(Gen.resultOf(Principal.AccountPrincipal.apply _))

  implicit lazy val arbServicePrincipal: Arbitrary[Principal.ServicePrincipal] =
    Arbitrary(Gen.resultOf(Principal.ServicePrincipal.apply _))

  implicit lazy val arbWebProviderPrincipal: Arbitrary[Principal.WebProviderPrincipal] =
    Arbitrary(Gen.resultOf(Principal.WebProviderPrincipal.apply _))

  implicit lazy val arbSamlProviderPrincipal: Arbitrary[Principal.SamlProviderPrincipal] =
    Arbitrary(Gen.resultOf(Principal.SamlProviderPrincipal(_: SamlProviderArn)))

  implicit lazy val shrinkSamlProviderPrincipal: Shrink[Principal.SamlProviderPrincipal] =
    Shrink.xmap(Principal.SamlProviderPrincipal.apply, Principal.SamlProviderPrincipal.unapply(_).get)

  implicit lazy val arbIamUserPrincipal: Arbitrary[Principal.IamUserPrincipal] =
    Arbitrary(Gen.resultOf(Principal.IamUserPrincipal.apply _))

  implicit lazy val shrinkIamUserPrincipal: Shrink[Principal.IamUserPrincipal] =
    Shrink.xmap(Principal.IamUserPrincipal.apply, Principal.IamUserPrincipal.unapply(_).get)

  implicit lazy val arbIamRolePrincipal: Arbitrary[Principal.IamRolePrincipal] =
    Arbitrary(Gen.resultOf(Principal.IamRolePrincipal.apply _))

  implicit lazy val shrinkIamRolePrincipal: Shrink[Principal.IamRolePrincipal] =
    Shrink.xmap(Principal.IamRolePrincipal.apply, Principal.IamRolePrincipal.unapply(_).get)

  implicit lazy val arbIamAssumedRolePrincipal: Arbitrary[Principal.StsAssumedRolePrincipal] =
    Arbitrary(Gen.resultOf(Principal.StsAssumedRolePrincipal.apply _))

  implicit lazy val shrinkIamAssumedRolePrincipal: Shrink[Principal.StsAssumedRolePrincipal] =
    Shrink.xmap(Principal.StsAssumedRolePrincipal.apply, Principal.StsAssumedRolePrincipal.unapply(_).get)

  implicit lazy val shrinkPrincipal: Shrink[Principal] =
    Shrink {
      case samlProvider: Principal.SamlProviderPrincipal ⇒ Shrink.shrink(samlProvider)
      case userPrincipal: Principal.IamUserPrincipal ⇒ Shrink.shrink(userPrincipal)
      case rolePrincipal: Principal.IamRolePrincipal ⇒ Shrink.shrink(rolePrincipal)
      case assumedRolePrincipal: Principal.StsAssumedRolePrincipal ⇒ Shrink.shrink(assumedRolePrincipal)
      case _ ⇒ Stream.empty
    }

  implicit lazy val arbPrincipalService: Arbitrary[Principal.Service] =
    Arbitrary(Gen.oneOf(Principal.Service.values))

  implicit lazy val arbPrincipalWebIdentityProvider: Arbitrary[Principal.WebIdentityProvider] =
    Arbitrary(Gen.oneOf(Principal.WebIdentityProvider.values))

  implicit lazy val arbCondition: Arbitrary[Condition] =
    Arbitrary {
      Gen.oneOf(
        arbitrary[Condition.ArnCondition],
        arbitrary[Condition.BinaryCondition],
        arbitrary[Condition.BooleanCondition],
        arbitrary[Condition.DateCondition],
        arbitrary[Condition.IpAddressCondition],
        arbitrary[Condition.NullCondition],
        arbitrary[Condition.NumericCondition],
        arbitrary[Condition.StringCondition],
        arbitrary[Condition.MultipleKeyValueCondition]
      )
    }

  implicit lazy val arbArnCondition: Arbitrary[Condition.ArnCondition] =
    Arbitrary {
      val arn =
        for {
          vendor ← arbitrary[Option[Arn.Namespace]]
          region ← arbitrary[Option[Region]]
          namespace ← Gen.option(CoreGen.accountId)
          relativeId ← Gen.option(Gen.identifier)
        } yield s"arn:aws:${vendor.getOrElse("*")}:${region.map(_.name).getOrElse("*")}:${namespace.getOrElse("*")}:${relativeId.getOrElse("*")}"
      for {
        key ← Gen.oneOf(Gen.const("aws:SourceArn"), Gen.identifier)
        comparisonType ← arbitrary[Condition.ArnComparisonType]
        comparisonValues ← UtilGen.nonEmptyListOfSqrtN(arn)
        ifExists ← arbitrary[Boolean]
      } yield Condition.ArnCondition(key, comparisonType, comparisonValues.distinct, ifExists)
    }

  implicit lazy val arbBinaryCondition: Arbitrary[Condition.BinaryCondition] =
    Arbitrary {
      for {
        key ← Gen.identifier
        values ← UtilGen.nonEmptyListOfSqrtN(arbitrary[Array[Byte]].map(ByteString(_)))
        ifExists ← arbitrary[Boolean]
      } yield Condition.BinaryCondition(key, values.distinct, ifExists)
    }

  implicit lazy val arbBooleanCondition: Arbitrary[Condition.BooleanCondition] =
    Arbitrary {
      for {
        key ← Gen.identifier
        value ← arbitrary[Boolean]
        ifExists ← arbitrary[Boolean]
      } yield Condition.BooleanCondition(key, value, ifExists)
    }

  implicit lazy val arbDateCondition: Arbitrary[Condition.DateCondition] =
    Arbitrary {
      for {
        key ← Gen.identifier
        comparisonType ← arbitrary[Condition.DateComparisonType]
        values ← UtilGen.nonEmptyListOfSqrtN(arbitrary[Date])
        ifExists ← arbitrary[Boolean]
      } yield Condition.DateCondition(key, comparisonType, values.distinct, ifExists)
    }

  implicit lazy val arbIpAddressCondition: Arbitrary[Condition.IpAddressCondition] =
    Arbitrary {
      val cidrBlock =
        for {
          address ← Gen.listOfN(4, Gen.choose(0, 255)).map(_.mkString("."))
          size ← Gen.choose(0,32)
        } yield s"$address/$size"
      for {
        key ← Gen.identifier
        comparisonType ← arbitrary[Condition.IpAddressComparisonType]
        cidrBlocks ← UtilGen.nonEmptyListOfSqrtN(cidrBlock)
        ifExists ← arbitrary[Boolean]
      } yield Condition.IpAddressCondition(key, comparisonType, cidrBlocks.distinct, ifExists)
    }

  implicit lazy val arbNullCondition: Arbitrary[Condition.NullCondition] =
    Arbitrary {
      for {
        key ← Gen.identifier
        value ← arbitrary[Boolean]
      } yield Condition.NullCondition(key, value)
    }

  implicit lazy val arbNumericCondition: Arbitrary[Condition.NumericCondition] =
    Arbitrary {
      for {
        key ← Gen.identifier
        comparisonType ← arbitrary[Condition.NumericComparisonType]
        values ← UtilGen.nonEmptyListOfSqrtN(arbitrary[Double])
        ifExists ← arbitrary[Boolean]
      } yield Condition.NumericCondition(key, comparisonType, values.distinct, ifExists)
    }

  implicit lazy val arbStringCondition: Arbitrary[Condition.StringCondition] =
    Arbitrary {
      for {
        key ← Gen.identifier
        comparisonType ← arbitrary[Condition.StringComparisonType]
        values ← UtilGen.nonEmptyListOfSqrtN(arbitrary[String].suchThat(_.forall(_ != '\uffff')))
        ifExists ← arbitrary[Boolean]
      } yield Condition.StringCondition(key, comparisonType, values.distinct, ifExists)
    }

  implicit lazy val arbSetOperation: Arbitrary[Condition.SetOperation] =
    Arbitrary(Gen.oneOf(Condition.SetOperation.values))

  implicit lazy val arbMultipleKeyValueCondition: Arbitrary[Condition.MultipleKeyValueCondition] =
    Arbitrary {
      val innerConditionGen =
        Gen.oneOf(
          arbitrary[Condition.ArnCondition],
          arbitrary[Condition.BinaryCondition],
          arbitrary[Condition.BooleanCondition],
          arbitrary[Condition.DateCondition],
          arbitrary[Condition.IpAddressCondition],
          arbitrary[Condition.NullCondition],
          arbitrary[Condition.NumericCondition],
          arbitrary[Condition.StringCondition]
        )
      for {
        innerCondition ← innerConditionGen
        setOperation ← arbitrary[Condition.SetOperation]
      } yield Condition.MultipleKeyValueCondition(setOperation, innerCondition)
    }

  implicit lazy val arbConditions: Arbitrary[Set[Condition]] =
    Arbitrary {
      for (conditions ← UtilGen.listOfSqrtN(arbitrary[Condition])) yield {
        conditions.foldLeft(Set.empty[Condition]) { (set, condition) ⇒
          set.find(c ⇒ c.comparisonType == condition.comparisonType && c.key == condition.key) match {
            case Some(_) ⇒ set
            case None    ⇒ set + condition
          }
        }
      }
    }

  implicit lazy val arbArnComparisonType: Arbitrary[Condition.ArnComparisonType] =
    Arbitrary(Gen.oneOf(Condition.ArnComparisonType.values))

  implicit lazy val arbDateComparisonType: Arbitrary[Condition.DateComparisonType] =
    Arbitrary(Gen.oneOf(Condition.DateComparisonType.values))

  implicit lazy val arbIpAddressComparisonType: Arbitrary[Condition.IpAddressComparisonType] =
    Arbitrary(Gen.oneOf(Condition.IpAddressComparisonType.values))

  implicit lazy val arbNumericComparisonType: Arbitrary[Condition.NumericComparisonType] =
    Arbitrary(Gen.oneOf(Condition.NumericComparisonType.values))

  implicit lazy val arbStringComparisonType: Arbitrary[Condition.StringComparisonType] =
    Arbitrary(Gen.oneOf(Condition.StringComparisonType.values))

  implicit lazy val arbResource: Arbitrary[Resource] =
    Arbitrary {
      for {
        partition ← arbitrary[Partition]
        account ← Gen.option(CoreGen.accountId).map(_.map(id ⇒ Account(id, partition)))
        region ← arbitrary[Option[Region]]
        namespace ← arbitrary[Arn.Namespace]
        resourceStr ← Gen.identifier
      } yield {
        val arn =
          new Arn(partition, namespace, region, account) {
            override val resource = resourceStr
          }
        Resource(arn.arnString)
      }
    }

  implicit lazy val arbResources: Arbitrary[Seq[Resource]] =
    Arbitrary {
      Gen.frequency(
        1 → Gen.const(Statement.allResources),
        19 → UtilGen.nonEmptyListOfSqrtN(arbitrary[Resource])
      )
    }

  implicit def arbAction: Arbitrary[Action] =
    Arbitrary(Gen.oneOf(if (Action.toAwsConversions.size > 1) (Action.toAwsConversions - Action.AllActions).keys.toList else Statement.allActions))

  implicit def arbActions: Arbitrary[Seq[Action]] =
    Arbitrary {
      Gen.frequency(
        1 → Gen.const(Statement.allActions),
        19 → UtilGen.listOfSqrtN(arbitrary[Action]).map(_.distinct))
    }

  implicit lazy val arbPath: Arbitrary[Path] = {
    val elementChar: Gen[Char] = Gen.oneOf(((0x21 to 0x2e) ++ (0x30 to 0x7f)).map(_.toChar))
    val element = UtilGen.stringOf(elementChar, 1, 512)
    Arbitrary {
      Gen.sized { n ⇒
        val max = Math.sqrt(n).toInt
        for {
          n ← Gen.choose(0, max)
          elements ← Gen.listOfN(n, element)
        } yield Path(elements)
      }
    }
  }

  implicit lazy val shrinkPath: Shrink[Path] =
    Shrink { path ⇒
      Shrink.shrink(path.elements).filter(_.forall(_.nonEmpty)).map(Path.apply)
    }

  implicit lazy val arbSamlProviderArn: Arbitrary[SamlProviderArn] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        name ← CoreGen.samlProviderName
      } yield SamlProviderArn(account, name)
    }

  implicit lazy val shrinkSamlProviderArn: Shrink[SamlProviderArn] =
    Shrink { arn ⇒
      Shrink.shrink(arn.account).map(x ⇒ arn.copy(account = x)) append
        Shrink.shrink(arn.name).filter(_.nonEmpty).map(x ⇒ arn.copy(name = x))
    }

  implicit lazy val arbUserArn: Arbitrary[UserArn] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        name ← CoreGen.iamName
        path ← arbitrary[Path]
      } yield UserArn(account, name, path)
    }

  implicit lazy val shrinkUserArn: Shrink[UserArn] =
    Shrink { arn ⇒
      Shrink.shrink(arn.account).map(x ⇒ arn.copy(account = x)) append
        Shrink.shrink(arn.name).filter(_.nonEmpty).map(x ⇒ arn.copy(name = x)) append
        Shrink.shrink(arn.path).map(x ⇒ arn.copy(path = x))
    }

  implicit lazy val arbRoleArn: Arbitrary[RoleArn] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        name ← CoreGen.iamName
        path ← arbitrary[Path]
      } yield RoleArn(account, name, path)
    }

  implicit lazy val shrinkRoleArn: Shrink[RoleArn] =
    Shrink { arn ⇒
      Shrink.shrink(arn.account).map(x ⇒ arn.copy(account = x)) append
        Shrink.shrink(arn.name).filter(_.nonEmpty).map(x ⇒ arn.copy(name = x)) append
        Shrink.shrink(arn.path).map(x ⇒ arn.copy(path = x))
    }

  implicit lazy val arbAssumedRoleArn: Arbitrary[AssumedRoleArn] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        roleName ← CoreGen.iamName
        sessionName ← CoreGen.assumedRoleSessionName
      } yield AssumedRoleArn(account, roleName, sessionName)
    }

  implicit lazy val shrinkAssumedRoleArn: Shrink[AssumedRoleArn] =
    Shrink { arn ⇒
      Shrink.shrink(arn.account).map(x ⇒ arn.copy(account = x)) append
        Shrink.shrink(arn.roleName).filter(_.nonEmpty).map(x ⇒ arn.copy(roleName = x)) append
        Shrink.shrink(arn.sessionName).filter(_.length > 1).map(x ⇒ arn.copy(sessionName = x))
    }

  implicit lazy val arbPolicyVersion: Arbitrary[Policy.Version] =
    Arbitrary(
      Gen.frequency(
        19 → Gen.const(Policy.Version.`2012-10-17`),
        1 → Gen.const(Policy.Version.`2008-10-17`)
      )
    )
}
