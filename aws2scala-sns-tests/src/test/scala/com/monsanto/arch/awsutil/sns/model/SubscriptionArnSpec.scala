package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.sns
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class SubscriptionArnSpec extends FreeSpec {
  sns.SNS.init()

  "a SubscriptionArn should" - {
    "provide the correct resource" in {
      forAll { arn: SubscriptionArn ⇒
        arn.resource shouldBe s"${arn.topicName}:${arn.subscriptionId}"
      }
    }

    "produce the correct ARN" in {
      forAll { arn: SubscriptionArn ⇒
        arn.arnString shouldBe
          s"arn:${arn.account.partition}:sns:${arn.region.name}:${arn.account.id}:${arn.topicName}:${arn.subscriptionId}"
      }
    }

    "can round-trip via an ARN" in {
      forAll { arn: SubscriptionArn ⇒
        SubscriptionArn.fromArnString(arn.arnString) shouldBe arn
      }
    }

    "will fail to parse an invalid ARN" in {
      an [IllegalArgumentException] shouldBe thrownBy {
        SubscriptionArn.fromArnString("arn:aws:iam::111222333444:root")
      }
    }
  }
}
