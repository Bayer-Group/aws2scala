package com.monsanto.arch.awsutil.kms.model

import com.monsanto.arch.awsutil.kms.KMS
import com.monsanto.arch.awsutil.testkit.KmsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class KeyArnSpec extends FreeSpec {
  KMS.init()

  "a KeyArn should" - {
    "provide the correct resource" in {
      forAll { arn: KeyArn ⇒
        arn.resource shouldBe s"key/${arn.id}"
      }
    }

    "produce the correct ARN" in {
      forAll { arn: KeyArn ⇒
        val partition = arn.account.partition.id
        val account = arn.account.id
        val region = arn.region.name
        val id = arn.id
        arn.arnString shouldBe s"arn:$partition:kms:$region:$account:key/$id"
      }
    }

    "can round-trip via an ARN" in {
      forAll { arn: KeyArn ⇒
        KeyArn.fromArnString(arn.arnString) shouldBe arn
      }
    }

    "will fail to parse an invalid ARN" in {
      an [IllegalArgumentException] shouldBe thrownBy {
        KeyArn.fromArnString("arn:aws:iam::111222333444:root")
      }
    }
  }
}
