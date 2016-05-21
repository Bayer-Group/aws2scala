package com.monsanto.arch.awsutil.auth.policy

import java.nio.ByteBuffer
import java.util.{Base64, Date}

import akka.util.ByteString
import com.amazonaws.auth.policy.conditions._
import com.monsanto.arch.awsutil.converters.CoreConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalatest.prop.TableDrivenPropertyChecks.{Table, forAll ⇒ forAllIn}

import scala.collection.JavaConverters._

class ConditionSpec extends FreeSpec with AwsEnumerationBehaviours {
  "Condition should" - {
    "be round-tripped through its AWS equivalent" in {
      forAll { condition: Condition ⇒
        condition.asAws.asScala shouldBe condition
      }
    }

    "provide convenience keys for" - {
      "source ARNs" in {
        val result = Condition.sourceArn is "foo"
        result.key shouldBe ConditionFactory.SOURCE_ARN_CONDITION_KEY
      }

      "current time" in {
        val result = Condition.currentTime is new Date()
        result.key shouldBe ConditionFactory.CURRENT_TIME_CONDITION_KEY
      }

      "epoch time" in {
        val result = Condition.epochTime is new Date()
        result.key shouldBe ConditionFactory.EPOCH_TIME_CONDITION_KEY
      }

      "source IP" in {
        Condition.sourceIp shouldBe Condition.IpAddressKey(ConditionFactory.SOURCE_IP_CONDITION_KEY, ignoreMissing = false)
      }
    }

    "provide methods for generating" - {
      "ARN conditions" - {
        "specifying ifExists on the key" in {
          forAll { condition: Condition.ArnCondition ⇒
            val key =
              if (condition.ignoreMissing) {
                Condition.arn(condition.key).ifExists
              } else {
                Condition.arn(condition.key)
              }
            val result =
              condition.comparisonType match {
                case Condition.ArnComparisonType.Equals ⇒ key.is(condition.values: _*)
                case Condition.ArnComparisonType.NotEquals ⇒ key.isNot(condition.values: _*)
                case Condition.ArnComparisonType.Like ⇒ key.isLike(condition.values: _*)
                case Condition.ArnComparisonType.NotLike ⇒ key.isNotLike(condition.values: _*)
              }
            result shouldBe condition
          }
        }

        "specifying ifExists on the condition" in {
          forAll { condition: Condition.ArnCondition ⇒
            val key = Condition.arn(condition.key)
            val baseCondition =
              condition.comparisonType match {
                case Condition.ArnComparisonType.Equals ⇒ key.is(condition.values: _*)
                case Condition.ArnComparisonType.NotEquals ⇒ key.isNot(condition.values: _*)
                case Condition.ArnComparisonType.Like ⇒ key.isLike(condition.values: _*)
                case Condition.ArnComparisonType.NotLike ⇒ key.isNotLike(condition.values: _*)
              }
            val result =
              if (condition.ignoreMissing) {
                baseCondition.ifExists
              } else {
                baseCondition
              }
            result shouldBe condition
          }
        }
      }

      "binary conditions" - {
        "using an array of bytes" in {
          forAll(Gen.identifier, arbitrary[Array[Byte]]) { (key, bytes) ⇒
            val result = Condition.binary(key) is bytes
            result shouldBe Condition.BinaryCondition(key, Seq(ByteString(bytes)), ignoreMissing = false)
          }
        }

        "using a ByteBuffer" in {
          forAll(Gen.identifier, arbitrary[Array[Byte]].map(ByteBuffer.wrap)) { (key, bytes) ⇒
            val result = Condition.binary(key) is bytes
            result shouldBe Condition.BinaryCondition(key, Seq(ByteString(bytes)), ignoreMissing = false)
          }
        }

        "using a ByteString" in {
          forAll(Gen.identifier, arbitrary[Array[Byte]].map(ByteString(_))) { (key, bytes) ⇒
            val result = Condition.binary(key) is bytes
            result shouldBe Condition.BinaryCondition(key, Seq(bytes), ignoreMissing = false)
          }
        }

        "specifying ifExists on the BinaryKey" in {
          forAll(Gen.identifier, arbitrary[Array[Byte]].map(ByteString(_))) { (key, bytes) ⇒
            val result = Condition.binary(key).ifExists is bytes
            result shouldBe Condition.BinaryCondition(key, Seq(bytes), ignoreMissing = true)
          }
        }

        "specifying ifExists on the BinaryCondition" in {
          forAll(Gen.identifier, arbitrary[Array[Byte]].map(ByteString(_))) { (key, bytes) ⇒
            val result = (Condition.binary(key) is bytes).ifExists
            result shouldBe Condition.BinaryCondition(key, Seq(bytes), ignoreMissing = true)
          }
        }
      }

      "boolean conditions" - {
        "specifying ifExists on the key" in {
          forAll { condition: Condition.BooleanCondition ⇒
            val key =
              if (condition.ignoreMissing) {
                Condition.boolean(condition.key).ifExists
              } else {
                Condition.boolean(condition.key)
              }
            val result = if (condition.value) key.isTrue else key.isFalse
            result shouldBe condition
          }
        }

        "specifying ifExists on the condition" in {
          forAll { condition: Condition.BooleanCondition ⇒
            val key = Condition.boolean(condition.key)
            val baseCondition = if (condition.value) key.isTrue else key.isFalse
            val result =
              if (condition.ignoreMissing) {
                baseCondition.ifExists
              } else {
                baseCondition
              }
            result shouldBe condition
          }
        }
      }

      "date conditions" - {
        "specifying ifExists on the key" in {
          forAll { condition: Condition.DateCondition ⇒
            val key =
              if (condition.ignoreMissing) {
                Condition.date(condition.key).ifExists
              } else {
                Condition.date(condition.key)
              }
            val result =
              condition.comparisonType match {
                case Condition.DateComparisonType.Equals ⇒ key.is(condition.values: _*)
                case Condition.DateComparisonType.NotEquals ⇒ key.isNot(condition.values: _*)
                case Condition.DateComparisonType.After ⇒ key.isAfter(condition.values: _*)
                case Condition.DateComparisonType.AtOrAfter ⇒ key.isAtOrAfter(condition.values: _*)
                case Condition.DateComparisonType.Before ⇒ key.isBefore(condition.values: _*)
                case Condition.DateComparisonType.AtOrBefore ⇒ key.isAtOrBefore(condition.values: _*)
              }
            result shouldBe condition
          }
        }

        "specifying ifExists on the condition" in {
          forAll { condition: Condition.DateCondition ⇒
            val key = Condition.date(condition.key)
            val baseCondition =
              condition.comparisonType match {
                case Condition.DateComparisonType.Equals ⇒ key.is(condition.values: _*)
                case Condition.DateComparisonType.NotEquals ⇒ key.isNot(condition.values: _*)
                case Condition.DateComparisonType.After ⇒ key.isAfter(condition.values: _*)
                case Condition.DateComparisonType.AtOrAfter ⇒ key.isAtOrAfter(condition.values: _*)
                case Condition.DateComparisonType.Before ⇒ key.isBefore(condition.values: _*)
                case Condition.DateComparisonType.AtOrBefore ⇒ key.isAtOrBefore(condition.values: _*)
              }
            val result =
              if (condition.ignoreMissing) {
                baseCondition.ifExists
              } else {
                baseCondition
              }
            result shouldBe condition
          }
        }
      }

      "IP address conditions" - {
        "specifying ifExists on the key" in {
          forAll { condition: Condition.IpAddressCondition ⇒
            val key =
              if (condition.ignoreMissing) {
                Condition.ipAddress(condition.key).ifExists
              } else {
                Condition.ipAddress(condition.key)
              }
            val result =
              condition.comparisonType match {
                case Condition.IpAddressComparisonType.IsIn ⇒ key.isIn(condition.cidrBlocks: _*)
                case Condition.IpAddressComparisonType.IsNotIn ⇒ key.isNotIn(condition.cidrBlocks: _*)
              }
            result shouldBe condition
          }
        }

        "specifying ifExists on the condition" in {
          forAll { condition: Condition.IpAddressCondition ⇒
            val key = Condition.ipAddress(condition.key)
            val baseCondition =
              condition.comparisonType match {
                case Condition.IpAddressComparisonType.IsIn ⇒ key.isIn(condition.cidrBlocks: _*)
                case Condition.IpAddressComparisonType.IsNotIn ⇒ key.isNotIn(condition.cidrBlocks: _*)
              }
            val result =
              if (condition.ignoreMissing) {
                baseCondition.ifExists
              } else {
                baseCondition
              }
            result shouldBe condition
          }
        }
      }

      "null conditions" in {
        forAll { condition: Condition.NullCondition ⇒
          val result =
            if (condition.value) {
              Condition.isMissing(condition.key)
            } else {
              Condition.isNotNull(condition.key)
            }
          result shouldBe condition
        }
      }

      "numeric conditions" - {
        "specifying ifExists on the key" in {
          forAll { condition: Condition.NumericCondition ⇒
            val key =
              if (condition.ignoreMissing) {
                Condition.numeric(condition.key).ifExists
              } else {
                Condition.numeric(condition.key)
              }
            val result =
              condition.comparisonType match {
                case Condition.NumericComparisonType.Equals ⇒ key.is(condition.values: _*)
                case Condition.NumericComparisonType.GreaterThan ⇒ key.isGreaterThan(condition.values: _*)
                case Condition.NumericComparisonType.GreaterThanEquals ⇒ key.isGreaterThanOrEqualTo(condition.values: _*)
                case Condition.NumericComparisonType.LessThan ⇒ key.isLessThan(condition.values: _*)
                case Condition.NumericComparisonType.LessThanEquals ⇒ key.isLessThanOrEqualTo(condition.values: _*)
                case Condition.NumericComparisonType.NotEquals ⇒ key.isNot(condition.values: _*)
              }
            result shouldBe condition
          }
        }

        "specifying ifExists on the condition" in {
          forAll { condition: Condition.NumericCondition ⇒
            val key = Condition.numeric(condition.key)
            val baseCondition =
              condition.comparisonType match {
                case Condition.NumericComparisonType.Equals ⇒ key.is(condition.values: _*)
                case Condition.NumericComparisonType.GreaterThan ⇒ key.isGreaterThan(condition.values: _*)
                case Condition.NumericComparisonType.GreaterThanEquals ⇒ key.isGreaterThanOrEqualTo(condition.values: _*)
                case Condition.NumericComparisonType.LessThan ⇒ key.isLessThan(condition.values: _*)
                case Condition.NumericComparisonType.LessThanEquals ⇒ key.isLessThanOrEqualTo(condition.values: _*)
                case Condition.NumericComparisonType.NotEquals ⇒ key.isNot(condition.values: _*)
              }
            val result =
              if (condition.ignoreMissing) {
                baseCondition.ifExists
              } else {
                baseCondition
              }
            result shouldBe condition
          }
        }
      }

      "string conditions" - {
        "specifying ifExists on the key" in {
          forAll { condition: Condition.StringCondition ⇒
            val key =
              if (condition.ignoreMissing) {
                Condition.string(condition.key).ifExists
              } else {
                Condition.string(condition.key)
              }
            val result =
              condition.comparisonType match {
                case Condition.StringComparisonType.Equals ⇒ key.is(condition.values: _*)
                case Condition.StringComparisonType.NotEquals ⇒ key.isNot(condition.values: _*)
                case Condition.StringComparisonType.EqualsIgnoreCase ⇒ key.ignoringCaseIs(condition.values: _*)
                case Condition.StringComparisonType.NotEqualsIgnoreCase ⇒ key.ignoringCaseIsNot(condition.values: _*)
                case Condition.StringComparisonType.Like ⇒ key.isLike(condition.values: _*)
                case Condition.StringComparisonType.NotLike ⇒ key.isNotLike(condition.values: _*)
              }
            result shouldBe condition
          }
        }

        "specifying ifExists on the condition" in {
          forAll { condition: Condition.StringCondition ⇒
            val key = Condition.string(condition.key)
            val baseCondition =
              condition.comparisonType match {
                case Condition.StringComparisonType.Equals ⇒ key.is(condition.values: _*)
                case Condition.StringComparisonType.NotEquals ⇒ key.isNot(condition.values: _*)
                case Condition.StringComparisonType.EqualsIgnoreCase ⇒ key.ignoringCaseIs(condition.values: _*)
                case Condition.StringComparisonType.NotEqualsIgnoreCase ⇒ key.ignoringCaseIsNot(condition.values: _*)
                case Condition.StringComparisonType.Like ⇒ key.isLike(condition.values: _*)
                case Condition.StringComparisonType.NotLike ⇒ key.isNotLike(condition.values: _*)
              }
            val result =
              if (condition.ignoreMissing) {
                baseCondition.ifExists
              } else {
                baseCondition
              }
            result shouldBe condition
          }
        }
      }
    }
  }

  "Condition.ArnCondition should" - {
    "convert to the correct AWS condition" in {
      forAll { condition: Condition.ArnCondition ⇒
        condition.asAws should have (
          'conditionKey (condition.key),
          'type (condition.comparisonType.asAws.toString + (if (condition.ignoreMissing)  "IfExists" else "")),
          'values (condition.values.asJava)
        )
      }
    }

    behave like multiValueSupportCondition[Condition.ArnCondition]
  }

  "Condition.ArnComparisonType enumeration" - {
    val comparisonTypes = Table("ARN comparison types", Condition.ArnComparisonType.values: _*)

    behave like anAwsEnumeration(
      ArnCondition.ArnComparisonType.values,
      Condition.ArnComparisonType.values,
      (_: Condition.ArnComparisonType).asAws,
      (_: ArnCondition.ArnComparisonType).asScala)

    "should have an id that matches the AWS name" in {
      forAllIn(comparisonTypes) { comparisonType ⇒
        comparisonType.id shouldBe comparisonType.asAws.name()
      }
    }

    "should be recoverable from an ID" in {
      forAllIn(comparisonTypes) { comparisonType ⇒
        Condition.ArnComparisonType(comparisonType.id) shouldBe theSameInstanceAs (comparisonType)
      }
    }
  }

  "Condition.BinaryCondition should" - {
    "convert to the correct AWS condition" in {
      forAll { condition: Condition.BinaryCondition ⇒
        condition.asAws should have (
          'conditionKey (condition.key),
          'type (if (condition.ignoreMissing)  "BinaryIfExists" else "Binary"),
          'values (condition.values.map(v ⇒ Base64.getEncoder.encodeToString(v.toArray)).asJava)
        )
      }
    }

    behave like multiValueSupportCondition[Condition.BinaryCondition]
  }

  "Condition.BooleanCondition should" - {
    "convert to the correct AWS condition" in {
      forAll { condition: Condition.BooleanCondition ⇒
        condition.asAws should have (
          'conditionKey (condition.key),
          'type (if (condition.ignoreMissing)  "BoolIfExists" else "Bool"),
          'values (Seq(condition.value.toString).asJava)
        )
      }
    }

    behave like multiValueSupportCondition[Condition.BooleanCondition]
  }

  "Condition.DateCondition should" - {
    "convert to the correct AWS condition" in {
      forAll { condition: Condition.DateCondition ⇒
        condition.asAws should have (
          'conditionKey (condition.key),
          'type (condition.comparisonType.asAws.toString + (if (condition.ignoreMissing)  "IfExists" else "")),
          'values (condition.values.map(_.toInstant.toString).asJava)
        )
      }
    }

    behave like multiValueSupportCondition[Condition.DateCondition]
  }

  "Condition.DateComparisonType enumeration" - {
    val comparisonTypes = Table("date comparison type", Condition.DateComparisonType.values: _*)

    behave like anAwsEnumeration(
      DateCondition.DateComparisonType.values, Condition.DateComparisonType.values,
      (_: Condition.DateComparisonType).asAws, (_: DateCondition.DateComparisonType).asScala)

    "should have an id that matches the AWS name" in {
      forAllIn(comparisonTypes) { comparisonType ⇒
        comparisonType.id shouldBe comparisonType.asAws.name()
      }
    }

    "should be recoverable from an ID" in {
      forAllIn(comparisonTypes) { comparisonType ⇒
        Condition.DateComparisonType(comparisonType.id) shouldBe theSameInstanceAs (comparisonType)
      }
    }
  }

  "Condition.IpAddressCondition should" - {
    "convert to the correct AWS condition" in {
      forAll { condition: Condition.IpAddressCondition ⇒
        condition.asAws should have (
          'conditionKey (condition.key),
          'type (condition.comparisonType.asAws.toString + (if (condition.ignoreMissing)  "IfExists" else "")),
          'values (condition.cidrBlocks.asJava)
        )
      }
    }

    behave like multiValueSupportCondition[Condition.IpAddressCondition]
  }

  "Condition.IpAddressComparisonType enumeration" - {
    val comparisonTypes = Table("IP address comparison type", Condition.IpAddressComparisonType.values: _*)

    behave like anAwsEnumeration(
      IpAddressCondition.IpAddressComparisonType.values, Condition.IpAddressComparisonType.values,
      (_: Condition.IpAddressComparisonType).asAws, (_: IpAddressCondition.IpAddressComparisonType).asScala)

    "should have an id that matches the AWS name" in {
      forAllIn(comparisonTypes) { comparisonType ⇒
        comparisonType.id shouldBe comparisonType.asAws.name()
      }
    }

    "should be recoverable from an ID" in {
      forAllIn(comparisonTypes) { comparisonType ⇒
        Condition.IpAddressComparisonType(comparisonType.id) shouldBe theSameInstanceAs (comparisonType)
      }
    }
  }

  "Condition.NumericCondition should" - {
    "convert to the correct AWS condition" in {
      forAll { condition: Condition.NumericCondition ⇒
        condition.asAws should have (
          'conditionKey (condition.key),
          'type (condition.comparisonType.asAws.toString + (if (condition.ignoreMissing)  "IfExists" else "")),
          'values (condition.values.map(_.toString).asJava)
        )
      }
    }

    behave like multiValueSupportCondition[Condition.NumericCondition]
  }

  "Condition.NumericComparisonType enumeration" - {
    val comparisonTypes = Table("numeric comparison type", Condition.NumericComparisonType.values: _*)

    behave like anAwsEnumeration(
      NumericCondition.NumericComparisonType.values, Condition.NumericComparisonType.values,
      (_: Condition.NumericComparisonType).asAws, (_: NumericCondition.NumericComparisonType).asScala)

    "should have an id that matches the AWS name" in {
      forAllIn(comparisonTypes) { comparisonType ⇒
        comparisonType.id shouldBe comparisonType.asAws.name()
      }
    }

    "should be recoverable from an ID" in {
      forAllIn(comparisonTypes) { comparisonType ⇒
        Condition.NumericComparisonType(comparisonType.id) shouldBe theSameInstanceAs (comparisonType)
      }
    }
  }

  "Condition.StringCondition should" - {
    "convert to the correct AWS condition" in {
      forAll { condition: Condition.StringCondition ⇒
        condition.asAws should have (
          'conditionKey (condition.key),
          'type (condition.comparisonType.asAws.toString + (if (condition.ignoreMissing)  "IfExists" else "")),
          'values (condition.values.asJava)
        )
      }
    }

    behave like multiValueSupportCondition[Condition.StringCondition]
  }

  "Condition.StringComparisonType enumeration" - {
    val comparisonTypes = Table("string comparison type", Condition.StringComparisonType.values: _*)

    "should have an id that matches the AWS name" in {
      forAllIn(comparisonTypes) { comparisonType ⇒
        comparisonType.id shouldBe comparisonType.asAws.name()
      }
    }

    "should be recoverable from an ID" in {
      forAllIn(comparisonTypes) { comparisonType ⇒
        Condition.StringComparisonType(comparisonType.id) shouldBe theSameInstanceAs (comparisonType)
      }
    }

    behave like anAwsEnumeration(
      StringCondition.StringComparisonType.values, Condition.StringComparisonType.values,
      (_: Condition.StringComparisonType).asAws, (_: StringCondition.StringComparisonType).asScala)
  }

  "Condition.NullCondition should" - {
    "convert to the correct AWS condition" in {
      forAll { condition: Condition.NullCondition ⇒
        condition.asAws should have (
          'conditionKey (condition.key),
          'type ("Null"),
          'values (Seq(condition.value.toString).asJava)
        )
      }
    }

    behave like multiValueSupportCondition[Condition.NullCondition]
  }

  "Condition.MultiValueCondition should convert to the correct AWS condition" in {
    forAll { condition: Condition.MultipleKeyValueCondition ⇒
      val innerAwsCondition = condition.condition.asAws
      val awsType = condition.op match {
        case Condition.SetOperation.ForAllValues ⇒ s"ForAllValues:${innerAwsCondition.getType}"
        case Condition.SetOperation.ForAnyValue  ⇒ s"ForAnyValue:${innerAwsCondition.getType}"
      }
      condition.asAws should have (
        'conditionKey (innerAwsCondition.getConditionKey),
        'type (awsType),
        'values (innerAwsCondition.getValues)
      )
    }
  }

  //noinspection UnitMethodIsParameterless
  private def multiValueSupportCondition[T <: Condition with Condition.MultipleKeyValueSupport: Arbitrary]: Unit = {
    "support the forAllValues set operation" in {
      forAll { condition: T ⇒
        condition.forAllValues shouldBe
          Condition.MultipleKeyValueCondition(
            Condition.SetOperation.ForAllValues,
            condition)
      }
    }

    "support the forAnyValue set operation" in {
      forAll { condition: T ⇒
        condition.forAnyValue shouldBe
          Condition.MultipleKeyValueCondition(
            Condition.SetOperation.ForAnyValue,
            condition)
      }
    }
  }
}
