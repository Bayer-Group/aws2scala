package com.monsanto.arch.awsutil.kms.model

import org.scalatest.FreeSpec
import org.scalatest.Matchers._

import scala.collection.JavaConverters._


class DecryptRequestSpec extends FreeSpec {
  val ciphertext = Array.fill(32)(0.toByte)

  "an EncryptRequest" - {
    "generates a correct AWS request" - {
      "with no additional args" in {
        val request = DecryptRequest(ciphertext).toAws

        request.getCiphertextBlob.array shouldBe ciphertext
        request.getEncryptionContext shouldBe empty
        request.getGrantTokens shouldBe empty
      }

      "with a context" in {
        val context = Map("some" → "context")
        val request = DecryptRequest(ciphertext, context).toAws

        request.getCiphertextBlob.array shouldBe ciphertext
        request.getEncryptionContext.asScala shouldBe context
        request.getGrantTokens shouldBe empty
      }

      "with grant tokens" in {
        val grantTokens = Seq("a", "b", "c")

        val request = DecryptRequest(ciphertext, grantTokens = grantTokens).toAws

        request.getCiphertextBlob.array shouldBe ciphertext
        request.getEncryptionContext shouldBe empty
        request.getGrantTokens.asScala shouldBe grantTokens
      }
    }
  }
}
