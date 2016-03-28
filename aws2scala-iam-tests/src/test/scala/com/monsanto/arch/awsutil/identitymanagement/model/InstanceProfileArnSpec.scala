package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class InstanceProfileArnSpec extends FreeSpec {
  "a InstanceProfileArn should" - {
    "provide the correct resource" in {
      forAll(
        arbitrary[Account] → "account",
        arbitrary[Name] → "instanceProfileName",
        arbitrary[Path] → "path"
      ) { (account, instanceProfileName, path) ⇒
        val arn = InstanceProfileArn(account, instanceProfileName, path)
        arn.resource shouldBe s"instance-profile$path$instanceProfileName"
      }
    }

    "produce the correct ARN" in {
      forAll(
        arbitrary[Account] → "account",
        arbitrary[Name] → "instanceProfileName",
        arbitrary[Path] → "path"
      ) { (account, instanceProfileName, path) ⇒
        val arn = InstanceProfileArn(account, instanceProfileName, path)
        arn.value shouldBe s"arn:${account.partition}:iam::$account:instance-profile$path$instanceProfileName"
      }
    }

    "can round-trip via an ARN" in {
      forAll { arn: InstanceProfileArn ⇒
        InstanceProfileArn.fromArn(arn.value) shouldBe Some(arn)
      }
    }
  }
}
