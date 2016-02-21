package com.monsanto.arch.awsutil.test

import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table

import scala.reflect.runtime.{universe ⇒ ru}

trait AwsEnumerationBehaviours { this: FreeSpec ⇒
  def anAwsEnumeration[T <: AwsEnumerationCompanion[_ <: AwsEnumeration[_]]: ru.TypeTag](companion: T): Unit = {
    type ScalaType = T#ScalaType
    type AwsType = T#AwsType

    val awsValues = {
      val typeTag = ru.typeTag[T]
      val scalaType = typeTag.tpe.member(ru.TermName("values")).asMethod.returnType.typeArgs.head
      val awsType = scalaType.member(ru.TermName("toAws")).asMethod.returnType
      val awsClass = typeTag.mirror.runtimeClass(awsType)
      val valuesMethod = awsClass.getDeclaredMethod("values")
      val values = valuesMethod.invoke(null).asInstanceOf[Array[AwsType]]
      Table("AWS value", values: _*)
    }

    val scalaValues = Table("Scala values", companion.values.map(_.asInstanceOf[ScalaType]): _*)

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
          companion.fromAws(scalaValue.toAws.asInstanceOf[AwsType]) shouldBe scalaValue
        }
      }

      "via its string representation" in {
        forAll(scalaValues) { scalaValue ⇒
          companion.fromString(scalaValue.toString) shouldBe Some(scalaValue)
        }
      }
    }
  }
}
