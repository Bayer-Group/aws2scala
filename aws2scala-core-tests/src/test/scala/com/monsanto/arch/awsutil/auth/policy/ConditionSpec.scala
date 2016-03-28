package com.monsanto.arch.awsutil.auth.policy

import java.util.Date

import com.amazonaws.auth.policy.conditions.ConditionFactory
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ConditionSpec extends FreeSpec with AwsEnumerationBehaviours {
  "the Condition.Arn should" - {
    implicit val arbArnConditionKey: Arbitrary[Condition.Arn.Key] =
      Arbitrary {
        val randomKey = Gen.alphaStr.map(Condition.Arn.apply)
        Gen.frequency(
          9 → Gen.const(Condition.Arn.SourceArn),
          1 → randomKey)
      }

    "have a SourceArn key" in {
      Condition.Arn.SourceArn.key shouldBe ConditionFactory.SOURCE_ARN_CONDITION_KEY
    }

    "create ArnEquals conditions" in {
      forAll { (key: Condition.Arn.Key, arn: String) ⇒
        val result = key is arn
        result shouldBe Condition(key.key, "ArnEquals", Seq(arn))
      }
    }

    "create ArnLike conditions" in {
      forAll { (key: Condition.Arn.Key, arn: String) ⇒
        val result = key isLike arn
        result shouldBe Condition(key.key, "ArnLike", Seq(arn))
      }
    }

    "create ArnNotEquals conditions" in {
      forAll { (key: Condition.Arn.Key, arn: String) ⇒
        val result = key isNot arn
        result shouldBe Condition(key.key, "ArnNotEquals", Seq(arn))
      }
    }

    "create ArnNotLike conditions" in {
      forAll { (key: Condition.Arn.Key, arn: String) ⇒
        val result = key isNotLike arn
        result shouldBe Condition(key.key, "ArnNotLike", Seq(arn))
      }
    }
  }

  "the Condition.Arn.ComparisonType enumeration" - {
    behave like anAwsEnumeration(Condition.Arn.ComparisonType)
  }

  "the Condition.Boolean object should" - {
    "create is false conditions" in {
      forAll { key: String ⇒
        val result = Condition.Boolean(key).isFalse
        result shouldBe Condition(key, "Bool", Seq("false"))
      }
    }

    "create is true conditions" in {
      forAll { key: String ⇒
        val result = Condition.Boolean(key).isTrue
        result shouldBe Condition(key, "Bool", Seq("true"))
      }
    }
  }

  "the Condition.Date object should" - {
    implicit val arbDateConditionKey: Arbitrary[Condition.Date.Key] =
      Arbitrary {
        val randomKey = Gen.alphaStr.map(Condition.Date.apply)
        Gen.frequency(
          9 → Gen.oneOf(Condition.Date.CurrentTime, Condition.Date.EpochTime, Condition.Date.TokenIssueTime),
          1 → randomKey)
      }

    "have a CurrentTime key" in {
      Condition.Date.CurrentTime shouldBe Condition.Date("aws:CurrentTime")
    }

    "have an EpochTime key" in {
      Condition.Date.EpochTime shouldBe Condition.Date("aws:EpochTime")
    }

    "have an TokenIssueTime key" in {
      Condition.Date.TokenIssueTime shouldBe Condition.Date("aws:TokenIssueTime")
    }

    "create DateEquals conditions" in {
      forAll { (key: Condition.Date.Key, date: Date) ⇒
        val result = key is date
        result shouldBe Condition(key.key, "DateEquals", Seq(date.toInstant.toString))
      }
    }

    "create DateNotEquals conditions" in {
      forAll { (key: Condition.Date.Key, date: Date) ⇒
        val result = key isNot date
        result shouldBe Condition(key.key, "DateNotEquals", Seq(date.toInstant.toString))
      }
    }

    "create DateLessThan conditions" in {
      forAll { (key: Condition.Date.Key, date: Date) ⇒
        val result = key isBefore date
        result shouldBe Condition(key.key, "DateLessThan", Seq(date.toInstant.toString))
      }
    }

    "create DateLessThanEquals conditions" in {
      forAll { (key: Condition.Date.Key, date: Date) ⇒
        val result = key isAtOrBefore date
        result shouldBe Condition(key.key, "DateLessThanEquals", Seq(date.toInstant.toString))
      }
    }

    "create DateGreaterThan conditions" in {
      forAll { (key: Condition.Date.Key, date: Date) ⇒
        val result = key isAfter date
        result shouldBe Condition(key.key, "DateGreaterThan", Seq(date.toInstant.toString))
      }
    }

    "create DateGreaterThanEquals conditions" in {
      forAll { (key: Condition.Date.Key, date: Date) ⇒
        val result = key isAtOrAfter date
        result shouldBe Condition(key.key, "DateGreaterThanEquals", Seq(date.toInstant.toString))
      }
    }
  }

  "the Condition.Date.ComparisonType enumeration" - {
    behave like anAwsEnumeration(Condition.Date.ComparisonType)
  }
}
