package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class PolicyArnSpec extends FreeSpec {
  "a PolicyArn should" - {
    "provide the correct resource" in {
      forAll(
        arbitrary[Account] → "account",
        arbitrary[Name] → "policyName",
        arbitrary[Path] → "path"
      ) { (account, policyName, path) ⇒
        val arn = PolicyArn(account, policyName, path)
        arn.resource shouldBe s"policy$path$policyName"
      }
    }

    "produce the correct ARN" in {
      forAll(
        arbitrary[Account] → "account",
        arbitrary[Name] → "policyName",
        arbitrary[Path] → "path"
      ) { (account, policyName, path) ⇒
        val arn = PolicyArn(account, policyName, path)
        arn.value shouldBe s"arn:${account.partition}:iam::$account:policy$path$policyName"
      }
    }

    "can round-trip via an ARN" in {
      forAll { arn: PolicyArn ⇒
        PolicyArn(arn.value) shouldBe arn
      }
    }

    "will fail to parse an invalid ARN" in {
      an [IllegalArgumentException] shouldBe thrownBy {
        UserArn("arn:foo")
      }
    }
  }
}
