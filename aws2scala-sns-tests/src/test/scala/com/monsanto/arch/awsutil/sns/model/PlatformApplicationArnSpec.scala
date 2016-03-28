package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class PlatformApplicationArnSpec extends FreeSpec {
  "a PlatformApplicationArn should" - {
    "provide the correct resource" in {
      forAll { arn: PlatformApplicationArn ⇒
        val platform = arn.platform.name
        val name = arn.name
        arn.resource shouldBe s"app/$platform/$name"
      }
    }

    "produce the correct ARN" in {
      forAll { arn: PlatformApplicationArn ⇒
        val partition = arn.account.partition
        val region = arn.region.name
        val account = arn.account.id
        val platform = arn.platform.name
        val name = arn.name
        arn.value shouldBe s"arn:$partition:sns:$region:$account:app/$platform/$name"
      }
    }

    "can round-trip via an ARN" in {
      forAll { arn: PlatformApplicationArn ⇒
        PlatformApplicationArn(arn.value) shouldBe arn
      }
    }

    "will fail to parse an invalid ARN" in {
      an [IllegalArgumentException] shouldBe thrownBy {
        PlatformApplicationArn("arn:foo")
      }
    }
  }
}
