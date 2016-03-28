package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class RoleArnSpec extends FreeSpec {
  "a RoleArn should" - {
    "provide the correct resource" in {
      forAll(
        arbitrary[Account] → "account",
        arbitrary[Name] → "roleName",
        arbitrary[Path] → "path"
      ) { (account, roleName, path) ⇒
        val arn = RoleArn(account, roleName, path)
        arn.resource shouldBe s"role$path$roleName"
      }
    }

    "produce the correct ARN" in {
      forAll(
        arbitrary[Account] → "account",
        arbitrary[Name] → "roleName",
        arbitrary[Path] → "path"
      ) { (account, roleName, path) ⇒
        val arn = RoleArn(account, roleName, path)
        arn.value shouldBe s"arn:${account.partition}:iam::$account:role$path$roleName"
      }
    }

    "can round-trip via an ARN" in {
      forAll { arn: RoleArn ⇒
        RoleArn(arn.value) shouldBe arn
      }
    }

    "will fail to parse an invalid ARN" in {
      an [IllegalArgumentException] shouldBe thrownBy {
        RoleArn("arn:foo")
      }
    }
  }
}
