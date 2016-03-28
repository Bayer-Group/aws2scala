package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.sns.model.AwsConverters._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class MessageAttributeValueSpec extends FreeSpec {
  "a MessageAttributeValue" - {
    "can be built from" - {
      "strings" in {
        forAll { value: String ⇒
          val result = MessageAttributeValue(value)
          result should matchPattern { case MessageAttributeValue.StringValue(`value`) ⇒ }
        }
      }

      "byte arrays" in {
        forAll { value: Array[Byte] ⇒
          val result = MessageAttributeValue(value)
          result should matchPattern { case MessageAttributeValue.BinaryValue(`value`) ⇒ }
        }
      }
    }
  }

  "the MessageAttributeValue.AwsAdapter type class supports" - {
    "strings" in {
      forAll { value: String ⇒
        val result = MessageAttributeValue(value).asAws
        result.getDataType shouldBe "String"
        result.getBinaryValue shouldBe null
        result.getStringValue shouldBe value
      }
    }

    "byte arrays" in {
      forAll { value: Array[Byte] ⇒
        val result = MessageAttributeValue(value).asAws
        result.getDataType shouldBe "Binary"
        result.getBinaryValue.array() shouldBe value
        result.getStringValue shouldBe null
      }
    }
  }
}
