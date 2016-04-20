package com.monsanto.arch.awsutil.test_support

import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.Tables.Table

import scala.reflect.ClassTag

trait AwsEnumerationBehaviours { this: FreeSpec ⇒
  def anAwsEnumeration[AwsType,ScalaType](awsValueArray: Array[AwsType],
                                          scalaValueSeq: Seq[ScalaType],
                                          asAws: ScalaType ⇒ AwsType,
                                          asScala: AwsType ⇒ ScalaType): Unit = {
    val awsValues = Table("AWS value", awsValueArray: _*)
    val scalaValues = Table("Scala value", scalaValueSeq: _*)

    "can be round-tripped" - {
      "from its AWS equivalent" in {
        forAll(awsValues) { awsValue ⇒
          asAws(asScala(awsValue)) shouldBe awsValue
        }
      }

      "via an AWS value" in {
        forAll(scalaValues) { scalaValue ⇒
          asScala(asAws(scalaValue)) shouldBe scalaValue
        }
      }
    }
  }

  def anAwsEnumeration[ScalaType <: AwsEnumeration[AwsType], AwsType <: Enum[AwsType]: ClassTag](companion: AwsEnumerationCompanion[ScalaType,AwsType]): Unit = {
    val awsValues = {
      val awsClass = implicitly[ClassTag[AwsType]].runtimeClass
      val valuesMethod = awsClass.getDeclaredMethod("values")
      val values = valuesMethod.invoke(null).asInstanceOf[Array[AwsType]]
      Table("AWS value", values: _*)
    }

    val scalaValues = Table("Scala values", companion.values: _*)

    "can be round-tripped" - {
      "from its AWS equivalent" in {
        forAll(awsValues) { awsValue ⇒
          companion.fromAws(awsValue).toAws shouldBe awsValue
        }
      }

      "from an AWS string representation" in {
        forAll(awsValues) { awsValue ⇒
          val awsString = awsValue.toString
          val result = companion.fromString(awsString).map(_.toString)
          result shouldBe Some(awsString)
        }
      }

      "via an AWS value" in {
        forAll(scalaValues) { scalaValue ⇒
          companion.fromAws(scalaValue.toAws) shouldBe scalaValue
        }
      }

      "via its string representation" in {
        forAll(scalaValues) { scalaValue ⇒
          companion.fromString(scalaValue.toString) shouldBe Some(scalaValue)
        }
      }
    }

    "provides an extractor" in {
      forAll(scalaValues) { scalaValue ⇒
        companion.unapply(scalaValue.toString) shouldBe Some(scalaValue)
      }
    }
  }
}
