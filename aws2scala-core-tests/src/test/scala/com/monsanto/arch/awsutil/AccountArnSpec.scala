package com.monsanto.arch.awsutil

import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class AccountArnSpec extends FreeSpec {
  "an account ARN should" - {
    "have the correct resource" in {
      forAll { arn: AccountArn ⇒
        arn.resource shouldBe "root"
      }
    }

    "produce the correct ARN" in {
      forAll { account: Account ⇒
        val arn = AccountArn(account)

        arn.value shouldBe s"arn:${account.partition}:iam::${account.id}:root"
      }
    }

    "round-trip via an ARN" in {
      forAll { arn: AccountArn ⇒
        AccountArn.fromArn(arn.value) shouldBe Some(arn)
      }
    }
  }
}
