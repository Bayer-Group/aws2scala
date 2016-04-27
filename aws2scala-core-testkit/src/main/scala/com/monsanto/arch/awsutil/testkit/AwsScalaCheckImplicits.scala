package com.monsanto.arch.awsutil.testkit

import java.util.Date

import akka.util.ByteString
import com.monsanto.arch.awsutil.auth.policy.{Condition, Policy, Principal, Statement}
import com.monsanto.arch.awsutil.partitions.Partition
import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.{Account, AccountArn, Arn}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen, Shrink}

object AwsScalaCheckImplicits {
  implicit lazy val arbPartition: Arbitrary[Partition] = Arbitrary(Gen.oneOf(Partition.values))

  implicit lazy val arbAccount: Arbitrary[Account] =
    Arbitrary{
      for {
        partition ← arbitrary[Partition]
        id ← AwsGen.accountId
      } yield Account(id, partition)
    }

  implicit lazy val arbAccountArn: Arbitrary[AccountArn] = Arbitrary(Gen.resultOf(AccountArn.apply _))

  implicit lazy val arbArnNamespace: Arbitrary[Arn.Namespace] = Arbitrary(Gen.oneOf(Arn.Namespace.values))

  implicit lazy val arbRegion: Arbitrary[Region] = Arbitrary(Gen.oneOf(Region.values))

  implicit lazy val arbPolicy: Arbitrary[Policy] = {
    val idGen = Gen.option(Gen.nonEmptyListOf(UtilGen.asciiChar).map(_.mkString))
    Arbitrary {
      for {
        id ← idGen
        statements ← Gen.nonEmptyListOf(arbitrary[Statement])
      } yield Policy(id, statements)
    }
  }

  implicit lazy val shrinkPolicy: Shrink[Policy] =
    Shrink { policy ⇒
      Shrink.shrink(policy.id)
        .filter(_.forall(_.nonEmpty))
        .map(Policy(_, policy.statements)) append
        Shrink.shrink(policy.statements)
          .filter(_.nonEmpty)
          .map(Policy(policy.id, _))
    }

  implicit lazy val arbStatement: Arbitrary[Statement] = {
    Arbitrary {
      for {
        sid ← Gen.option(AwsGen.statementId)
        effect ← arbitrary[Statement.Effect]
      } yield Statement(sid, Seq.empty, effect, Seq.empty, Seq.empty, Seq.empty)
    }
  }

  implicit lazy val shrinkStatement: Shrink[Statement] =
    Shrink { s ⇒
      Shrink.shrink(s.id)
        .filter(_.forall(_.nonEmpty))
        .map(Statement(_, s.principals, s.effect, s.actions, s.resources, s.conditions))
    }

  implicit lazy val arbStatementEffect: Arbitrary[Statement.Effect] =
    Arbitrary(Gen.oneOf(Statement.Effect.values))

  implicit lazy val arbPrincipal: Arbitrary[Principal] =
    Arbitrary {
      val constantPrincipals = Gen.oneOf(Principal.all, Principal.allServices, Principal.allWebProviders, Principal.allUsers)
      Gen.oneOf(
        constantPrincipals,
        arbitrary[Principal.AccountPrincipal],
        arbitrary[Principal.ServicePrincipal],
        arbitrary[Principal.WebProviderPrincipal],
        arbitrary[Principal.SamlProviderPrincipal],
        arbitrary[Principal.IamUserPrincipal],
        arbitrary[Principal.IamRolePrincipal],
        arbitrary[Principal.IamAssumedRolePrincipal])
    }

  implicit lazy val arbAccountPrincipal: Arbitrary[Principal.AccountPrincipal] =
    Arbitrary(Gen.resultOf(Principal.AccountPrincipal.apply _))

  implicit lazy val arbServicePrincipal: Arbitrary[Principal.ServicePrincipal] =
    Arbitrary(Gen.resultOf(Principal.ServicePrincipal.apply _))

  implicit lazy val arbWebProviderPrincipal: Arbitrary[Principal.WebProviderPrincipal] =
    Arbitrary(Gen.resultOf(Principal.WebProviderPrincipal.apply _))

  implicit lazy val arbSamlProviderPrincipal: Arbitrary[Principal.SamlProviderPrincipal] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        name ← AwsGen.iamName
      } yield Principal.SamlProviderPrincipal(account, name)
    }

  implicit lazy val shrinkSamlProviderPrincipal: Shrink[Principal.SamlProviderPrincipal] =
    Shrink { principal ⇒
      Shrink.shrink(principal.account).map(x ⇒ principal.copy(account = x)) append
        Shrink.shrink(principal.name).filter(_.nonEmpty).map(n ⇒ principal.copy(name = n))
    }

  implicit lazy val arbIamUserPrincipal: Arbitrary[Principal.IamUserPrincipal] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        user ← AwsGen.iamName
        path ←  iamPath
      } yield Principal.IamUserPrincipal(account, user, path)
    }

  implicit lazy val shrinkIamUserPrincipal: Shrink[Principal.IamUserPrincipal] =
    Shrink { principal ⇒
      Shrink.shrink(principal.account).map(x ⇒ principal.copy(account = x)) append
        Shrink.shrink(principal.name).filter(_.nonEmpty).map(x ⇒ principal.copy(name = x)) append
        shrinkIamPath(principal.path).map(x ⇒ principal.copy(path = x))
    }

  implicit lazy val arbIamRolePrincipal: Arbitrary[Principal.IamRolePrincipal] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        user ← AwsGen.iamName
        path ←  iamPath
      } yield Principal.IamRolePrincipal(account, user, path)
    }

  implicit lazy val shrinkIamRolePrincipal: Shrink[Principal.IamRolePrincipal] =
    Shrink { principal ⇒
      Shrink.shrink(principal.account).map(x ⇒ principal.copy(account = x)) append
        Shrink.shrink(principal.name).filter(_.nonEmpty).map(x ⇒ principal.copy(name = x)) append
        shrinkIamPath(principal.path).map(x ⇒ principal.copy(path = x))
    }

  implicit lazy val arbIamAssumedRolePrincipal: Arbitrary[Principal.IamAssumedRolePrincipal] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        roleName ← AwsGen.iamName
        sessionName ←  AwsGen.iamName
      } yield Principal.IamAssumedRolePrincipal(account, roleName, sessionName)
    }

  implicit lazy val shrinkIamAssumedRolePrincipal: Shrink[Principal.IamAssumedRolePrincipal] =
    Shrink { principal ⇒
      Shrink.shrink(principal.account).map(x ⇒ principal.copy(account = x)) append
        Shrink.shrink(principal.roleName).filter(_.nonEmpty).map(x ⇒ principal.copy(roleName = x)) append
        Shrink.shrink(principal.sessionName).filter(_.nonEmpty).map(x ⇒ principal.copy(sessionName = x))
    }

  implicit lazy val shrinkPrincipal: Shrink[Principal] =
    Shrink {
      case samlProvider: Principal.SamlProviderPrincipal ⇒ Shrink.shrink(samlProvider)
      case userPrincipal: Principal.IamUserPrincipal ⇒ Shrink.shrink(userPrincipal)
      case rolePrincipal: Principal.IamRolePrincipal ⇒ Shrink.shrink(rolePrincipal)
      case assumedRolePrincipal: Principal.IamAssumedRolePrincipal ⇒ Shrink.shrink(assumedRolePrincipal)
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
        arbitrary[Condition.NumericCondition],
        arbitrary[Condition.StringCondition]
      )
    }

  implicit lazy val arbArnCondition: Arbitrary[Condition.ArnCondition] =
    Arbitrary {
      val arn =
        for {
          vendor ← arbitrary[Option[Arn.Namespace]]
          region ← arbitrary[Option[Region]]
          namespace ← Gen.option(AwsGen.accountId)
          relativeId ← Gen.option(Gen.identifier)
        } yield s"arn:aws:${vendor.getOrElse("*")}:${region.getOrElse("*")}:${namespace.getOrElse("*")}:${relativeId.getOrElse("*")}"
      for {
        key ← Gen.oneOf(Gen.const("aws:SourceArn"), Gen.identifier)
        comparisonType ← arbitrary[Condition.ArnComparisonType]
        comparisonValues ← UtilGen.nonEmptyListOfSqrtN(arn)
        ifExists ← arbitrary[Boolean]
      } yield Condition.ArnCondition(key, comparisonType, comparisonValues, ifExists)
    }

  implicit lazy val arbBinaryCondition: Arbitrary[Condition.BinaryCondition] =
    Arbitrary {
      for {
        key ← Gen.identifier
        values ← UtilGen.nonEmptyListOfSqrtN(arbitrary[Array[Byte]].map(ByteString(_)))
        ifExists ← arbitrary[Boolean]
      } yield Condition.BinaryCondition(key, values, ifExists)
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
      } yield Condition.DateCondition(key, comparisonType, values, ifExists)
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
      } yield Condition.IpAddressCondition(key, comparisonType, cidrBlocks, ifExists)
    }

  implicit lazy val arbNumericCondition: Arbitrary[Condition.NumericCondition] =
    Arbitrary {
      for {
        key ← Gen.identifier
        comparisonType ← arbitrary[Condition.NumericComparisonType]
        values ← UtilGen.nonEmptyListOfSqrtN(arbitrary[Double])
        ifExists ← arbitrary[Boolean]
      } yield Condition.NumericCondition(key, comparisonType, values, ifExists)
    }

  implicit lazy val arbStringCondition: Arbitrary[Condition.StringCondition] =
    Arbitrary {
      for {
        key ← Gen.identifier
        comparisonType ← arbitrary[Condition.StringComparisonType]
        values ← UtilGen.nonEmptyListOfSqrtN(arbitrary[String])
        ifExists ← arbitrary[Boolean]
      } yield Condition.StringCondition(key, comparisonType, values, ifExists)
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

  private val iamPath: Gen[Option[String]] = {
    val elementChar: Gen[Char] = Gen.oneOf(((0x21 to 0x2e) ++ (0x30 to 0x7f)).map(_.toChar))
    val element = UtilGen.stringOf(elementChar, 1, 512)
    Gen.option(UtilGen.nonEmptyListOfSqrtN(element).map(_.mkString("/","/","/")))
  }

  private def shrinkIamPath(maybePath: Option[String]): Stream[Option[String]] =
    maybePath match {
      case Some(path) ⇒
        path.split("/").toList match {
          case Nil ⇒ Stream.empty
          case "" :: elements ⇒
            Shrink.shrink(elements)
              .filter(_.forall(_.nonEmpty))
              .map { elements ⇒
                if (elements.isEmpty) None
                else Some(elements.mkString("/","/","/"))
              }
          case _ ⇒ throw new IllegalStateException("This should not happen")
        }
      case None ⇒
        Stream.empty
    }
}
