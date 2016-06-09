package com.monsanto.arch.awsutil.kms.model

import java.util.UUID

import com.amazonaws.services.kms.model.{GenerateDataKeyRequest ⇒ AWSGenerateDataKeyRequest, GenerateDataKeyWithoutPlaintextRequest}
import org.scalatest.FreeSpec
import org.scalatest.Matchers._

import scala.collection.JavaConverters._

class GenerateDataKeyRequestSpec extends FreeSpec {
  val keyId = UUID.randomUUID().toString

  trait AWSRequestLike[T] {
    def keyId(request: T): String
    def keySpec(request: T): String
    def numberOfBytes(request: T): Integer
    def context(request: T): java.util.Map[String,String]
    def grantTokens(request: T): java.util.List[String]
  }

  object AWSRequestLike {
    implicit val withPlaintext = new AWSRequestLike[AWSGenerateDataKeyRequest] {
      override def keyId(request: AWSGenerateDataKeyRequest) = request.getKeyId
      override def keySpec(request: AWSGenerateDataKeyRequest) = request.getKeySpec
      override def context(request: AWSGenerateDataKeyRequest) = request.getEncryptionContext
      override def grantTokens(request: AWSGenerateDataKeyRequest) = request.getGrantTokens
      override def numberOfBytes(request: AWSGenerateDataKeyRequest) = request.getNumberOfBytes
    }
    implicit val withoutPlaintext = new AWSRequestLike[GenerateDataKeyWithoutPlaintextRequest] {
      override def keyId(request: GenerateDataKeyWithoutPlaintextRequest) = request.getKeyId
      override def keySpec(request: GenerateDataKeyWithoutPlaintextRequest) = request.getKeySpec
      override def context(request: GenerateDataKeyWithoutPlaintextRequest) = request.getEncryptionContext
      override def grantTokens(request: GenerateDataKeyWithoutPlaintextRequest) = request.getGrantTokens
      override def numberOfBytes(request: GenerateDataKeyWithoutPlaintextRequest) = request.getNumberOfBytes
    }
  }

  def createsTheCorrectAWSRequest[T: GenerateDataKeyRequest.AWSKeyRequestLike](implicit fromResult: AWSRequestLike[T]): Unit = {
    "will create the correct AWS request" - {
      "with no additional arguments" in {
        val request = GenerateDataKeyRequest(keyId)
        val result = request.toAws[T]

        fromResult.keyId(result) shouldBe keyId
        fromResult.keySpec(result) shouldBe DataKeySpec.Aes256.toString
        fromResult.numberOfBytes(result) shouldBe null
        fromResult.context(result) shouldBe empty
        fromResult.grantTokens(result) shouldBe empty
      }

      "with an alternate key spec" in {
        val keySpec = DataKeySpec.Aes128
        val request = GenerateDataKeyRequest(keyId, keySpec = keySpec)
        val result = request.toAws[T]

        fromResult.keyId(result) shouldBe keyId
        fromResult.keySpec(result) shouldBe keySpec.toString
        fromResult.numberOfBytes(result) shouldBe null
        fromResult.context(result) shouldBe empty
        fromResult.grantTokens(result) shouldBe empty
      }

      "with a non-empty encryption context" in {
        val encryptionContext = Map("a" → "b", "c" → "d")
        val request = GenerateDataKeyRequest(keyId, context = encryptionContext)
        val result = request.toAws[T]

        fromResult.keyId(result) shouldBe keyId
        fromResult.keySpec(result) shouldBe DataKeySpec.Aes256.toString
        fromResult.numberOfBytes(result) shouldBe null
        fromResult.context(result).asScala shouldBe encryptionContext
        fromResult.grantTokens(result) shouldBe empty
      }

      "with a non-empty list of grant tokens" in {
        val grantTokens = Seq("a", "b", "c", "d")
        val request = GenerateDataKeyRequest(keyId, grantTokens = grantTokens)
        val result = request.toAws[T]

        fromResult.keyId(result) shouldBe keyId
        fromResult.keySpec(result) shouldBe DataKeySpec.Aes256.toString
        fromResult.numberOfBytes(result) shouldBe null
        fromResult.context(result) shouldBe empty
        fromResult.grantTokens(result).asScala shouldBe grantTokens
      }

      "with a key UUID" in {
        val keyUuid = UUID.randomUUID().toString
        val request = GenerateDataKeyRequest(keyUuid)
        val result = request.toAws[T]

        fromResult.keyId(result) shouldBe keyUuid
      }

      "with a full key alias" in {
        val alias = "alias/some-alias"
        val request = GenerateDataKeyRequest(alias)
        val result = request.toAws[T]

        fromResult.keyId(result) shouldBe alias
      }

      "with a simple key alias" in {
        val alias = "some-alias"
        val request = GenerateDataKeyRequest(alias)
        val result = request.toAws[T]

        fromResult.keyId(result) shouldBe s"alias/$alias"
      }
    }
  }

  "a GenerateDataKeyRequest" - {
    "when converting to an AWS request" - {
      behave like createsTheCorrectAWSRequest[AWSGenerateDataKeyRequest]
    }
    "when converting to an AWS request with plaintext" - {
      behave like createsTheCorrectAWSRequest[GenerateDataKeyWithoutPlaintextRequest]
    }
  }
}
