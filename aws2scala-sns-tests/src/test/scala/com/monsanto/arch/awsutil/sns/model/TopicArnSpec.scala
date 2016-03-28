package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.SnsGen
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class TopicArnSpec extends FreeSpec {
  "a TopicArn should" - {
    "provide the correct resource" in {
      forAll(
        arbitrary[Account] → "account",
        arbitrary[Region] → "region",
        SnsGen.topicName → "topicName"
      ) { (account, region, topicName) ⇒
        val arn = TopicArn(account, region, topicName)
        arn.resource shouldBe topicName
      }
    }

    "produce the correct ARN" in {
      forAll(
        arbitrary[Account] → "account",
        arbitrary[Region] → "region",
        SnsGen.topicName → "topicName"
      ) { (account, region, topicName) ⇒
        val arn = TopicArn(account, region, topicName)
        arn.value shouldBe s"arn:${account.partition}:sns:${region.name}:$account:$topicName"
      }
    }

    "can round-trip via an ARN" in {
      forAll { arn: TopicArn ⇒
        TopicArn(arn.value) shouldBe arn
      }
    }

    "will fail to parse an invalid ARN" in {
      an [IllegalArgumentException] shouldBe thrownBy {
        TopicArn("arn:foo")
      }
    }
  }
}
