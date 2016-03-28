package com.monsanto.arch.awsutil.testkit

import com.monsanto.arch.awsutil.auth.policy.{Policy, Statement}
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
}
