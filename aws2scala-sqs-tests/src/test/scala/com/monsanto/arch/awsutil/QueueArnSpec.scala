package com.monsanto.arch.awsutil

import com.monsanto.arch.awsutil.sqs.SQS
import com.monsanto.arch.awsutil.sqs.model.QueueArn
import com.monsanto.arch.awsutil.testkit.SqsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class QueueArnSpec extends FreeSpec {
  SQS.init()

  "a queue ARN should" - {
    "have the correct resource" in {
      forAll { arn: QueueArn ⇒
        arn.resource shouldBe arn.name
      }
    }

    "produce the correct ARN" in {
      forAll { arn: QueueArn ⇒

        arn.arnString shouldBe s"arn:${arn.account.partition.id}:sqs:${arn.region.name}:${arn.account.id}:${arn.name}"
      }
    }

    "round-trip via an ARN" in {
      forAll { arn: QueueArn ⇒
        QueueArn.fromArnString(arn.arnString) shouldBe arn
      }
    }

    "will fail to parse an invalid ARN" in {
      an [IllegalArgumentException] shouldBe thrownBy {
        QueueArn.fromArnString("arn:aws:iam::111222333444:root")
      }
    }
  }
}
