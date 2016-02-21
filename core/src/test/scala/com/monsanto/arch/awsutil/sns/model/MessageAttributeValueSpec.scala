package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.sns.model.MessageAttributeValue.AwsAdapter
import org.scalacheck.Gen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class MessageAttributeValueSpec extends FreeSpec {
  "a MessageAttributeValue" - {
    "can be built from" - {
      "strings" in {
        forAll(Gen.alphaStr → "value") { value ⇒
          val result = MessageAttributeValue(value)
          result.value shouldBe value
        }
      }

      "byte arrays" in {
        forAll(SNSGen.byteArray → "value") { value ⇒
          val result = MessageAttributeValue(value)
          result.value shouldBe value
        }
      }
    }
  }

  "the MessageAttributeValue.AwsAdapter type class supports" - {
    "strings" in {
      forAll(Gen.alphaStr → "value") { value ⇒
        val result = implicitly[AwsAdapter[String]].toAws(value)
        result.getDataType shouldBe "String"
        result.getBinaryValue shouldBe null
        result.getStringValue shouldBe value
      }
    }

    "byte arrays" in {
      forAll(SNSGen.byteArray → "value") { value ⇒
        val result = implicitly[AwsAdapter[Array[Byte]]].toAws(value)
        result.getDataType shouldBe "Binary"
        result.getBinaryValue.array() shouldBe value
        result.getStringValue shouldBe null
      }
    }
  }
}
