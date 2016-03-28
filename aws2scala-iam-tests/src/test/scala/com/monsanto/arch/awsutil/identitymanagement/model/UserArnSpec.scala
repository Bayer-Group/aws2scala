package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class UserArnSpec extends FreeSpec {
  "a UserArn should" - {
    "provide the correct resource" in {
      forAll(
        arbitrary[Account] → "account",
        arbitrary[Name] → "userName",
        arbitrary[Path] → "path"
      ) { (account, userName, path) ⇒
        val arn = UserArn(account, userName, path)
        arn.resource shouldBe s"user$path$userName"
      }
    }

    "produce the correct ARN" in {
      forAll(
        arbitrary[Account] → "account",
        arbitrary[Name] → "userName",
        arbitrary[Path] → "path"
      ) { (account, userName, path) ⇒
        val arn = UserArn(account, userName, path)
        arn.value shouldBe s"arn:${account.partition}:iam::$account:user$path$userName"
      }
    }

    "can round-trip via an ARN" in {
      forAll { arn: UserArn ⇒
        UserArn(arn.value) shouldBe arn
      }
    }

    "will fail to parse an invalid ARN" in {
      an [IllegalArgumentException] shouldBe thrownBy {
        UserArn("arn:foo")
      }
    }
  }
}
