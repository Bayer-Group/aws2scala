package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class PlatformEndpointArnSpec extends FreeSpec {
  "a PlatformEndpointArn should" - {
    "provide the correct resource" in {
      forAll { arn: PlatformEndpointArn ⇒
        val platform = arn.platform.name
        val applicationName = arn.applicationName
        val endpointId = arn.endpointId

        arn.resource shouldBe s"endpoint/$platform/$applicationName/$endpointId"
      }
    }

    "produce the correct ARN" in {
      forAll { arn: PlatformEndpointArn ⇒
        val partition = arn.account.partition
        val region = arn.region.name
        val account = arn.account.id
        val platform = arn.platform.name
        val applicationArn = arn.applicationName
        val endpointId = arn.endpointId

        arn.value shouldBe s"arn:$partition:sns:$region:$account:endpoint/$platform/$applicationArn/$endpointId"
      }
    }

    "can round-trip via an ARN" in {
      forAll { arn: PlatformEndpointArn ⇒
        PlatformEndpointArn(arn.value) shouldBe arn
      }
    }

    "will fail to parse an invalid ARN" in {
      an [IllegalArgumentException] shouldBe thrownBy {
        PlatformEndpointArn("arn:foo")
      }
    }
  }
}
