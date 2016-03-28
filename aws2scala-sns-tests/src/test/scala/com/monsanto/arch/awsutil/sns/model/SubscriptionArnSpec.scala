package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.SnsGen
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class SubscriptionArnSpec extends FreeSpec {
  "a SubscriptionArn should" - {
    "provide the correct resource" in {
      forAll(
        arbitrary[Account] → "account",
        arbitrary[Region] → "region",
        SnsGen.topicName → "topicName",
        Gen.uuid → "uuid"
      ) { (account, region, topicName, uuid) ⇒
        val subscriptionId = uuid.toString
        val arn = SubscriptionArn(account, region, topicName, subscriptionId)
        arn.resource shouldBe s"$topicName:$subscriptionId"
      }
    }

    "produce the correct ARN" in {
      forAll(
        arbitrary[Account] → "account",
        arbitrary[Region] → "region",
        SnsGen.topicName → "topicName",
        Gen.uuid → "uuid"
      ) { (account, region, topicName, uuid) ⇒
        val subscriptionId = uuid.toString
        val arn = SubscriptionArn(account, region, topicName, subscriptionId)
        arn.value shouldBe s"arn:${account.partition}:sns:${region.name}:$account:$topicName:$subscriptionId"
      }
    }

    "can round-trip via an ARN" in {
      forAll { arn: SubscriptionArn ⇒
        SubscriptionArn(arn.value) shouldBe arn
      }
    }

    "will fail to parse an invalid ARN" in {
      an [IllegalArgumentException] shouldBe thrownBy {
        SubscriptionArn("arn:foo")
      }
    }
  }
}
