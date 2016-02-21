package com.monsanto.arch.awsutil.kms.model

import java.util.UUID

import org.scalatest.FreeSpec
import org.scalatest.Matchers._

import scala.collection.JavaConverters._

class EncryptRequestSpec extends FreeSpec {

  val keyId = "arn:key"
  val plaintext = Array.fill(32)(0.toByte)

  "an EncryptRequest" - {
    "generates a correct AWS request" - {
      "with no additional args" in {
        val request = EncryptRequest(keyId, plaintext).toAws

        request.getKeyId shouldBe keyId
        request.getPlaintext.array shouldBe plaintext
        request.getEncryptionContext shouldBe empty
        request.getGrantTokens shouldBe empty
      }

      "with a context" in {
        val context = Map("some" → "context")
        val request = EncryptRequest(keyId, plaintext, context).toAws

        request.getKeyId shouldBe keyId
        request.getPlaintext.array shouldBe plaintext
        request.getEncryptionContext.asScala shouldBe context
        request.getGrantTokens shouldBe empty
      }

      "with grant tokens" in {
        val grantTokens = Seq("a", "b", "c")

        val request = EncryptRequest(keyId, plaintext, grantTokens = grantTokens).toAws

        request.getKeyId shouldBe keyId
        request.getPlaintext.array shouldBe plaintext
        request.getEncryptionContext shouldBe empty
        request.getGrantTokens.asScala shouldBe grantTokens
      }

      "with a key GUID" in {
        val keyGuid = UUID.randomUUID().toString
        val request = EncryptRequest(keyGuid, plaintext).toAws

        request.getKeyId shouldBe keyGuid
      }

      "with a full alias" in {
        val alias = "alias/some-alias"
        val request = EncryptRequest(alias, plaintext).toAws

        request.getKeyId shouldBe alias
      }

      "with a simple alias" in {
        val alias = "some-alias"
        val request = EncryptRequest(alias, plaintext).toAws

        request.getKeyId shouldBe s"alias/$alias"
      }
    }
  }
}
