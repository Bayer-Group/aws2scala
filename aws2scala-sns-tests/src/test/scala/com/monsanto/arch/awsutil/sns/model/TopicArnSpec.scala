package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.sns
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class TopicArnSpec extends FreeSpec {
  sns.SNS.init()

  "a TopicArn should" - {
    "provide the correct resource" in {
      forAll { arn: TopicArn ⇒
        arn.resource shouldBe arn.name
      }
    }

    "produce the correct ARN" in {
      forAll { arn: TopicArn ⇒
        arn.arnString shouldBe s"arn:${arn.account.partition.id}:sns:${arn.region.name}:${arn.account.id}:${arn.name}"
      }
    }

    "can round-trip via an ARN" in {
      forAll { arn: TopicArn ⇒
        TopicArn.fromArnString(arn.arnString) shouldBe arn
      }
    }

    "will fail to parse an invalid ARN" in {
      an [IllegalArgumentException] shouldBe thrownBy {
        TopicArn.fromArnString("arn:aws:iam::111222333444:root")
      }
    }
  }
}
