package com.monsanto.arch.awsutil.kms

import java.nio.ByteBuffer
import java.util.{Date, UUID}

import akka.Done
import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.AmazonServiceException
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.kms.AWSKMSAsync
import com.amazonaws.services.kms.model.{CreateKeyRequest ⇒ AWSCreateKeyRequest, DataKeySpec ⇒ AWSDataKeySpec, DecryptRequest ⇒ AWSDecryptRequest, EncryptRequest ⇒ AWSEncryptRequest, GenerateDataKeyRequest ⇒ AWSGenerateDataKeyRequest, KeyMetadata ⇒ _, KeyState ⇒ _, _}
import com.monsanto.arch.awsutil.kms.model._
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.{AwsMockUtils, Materialised}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._

import scala.collection.JavaConverters._

class DefaultKMSClientSpec extends FreeSpec with Materialised with MockFactory with AwsMockUtils {
  private val alias = "someAlias"
  private val keyIdentifier = "arn:test-key:0"

  case class Fixture(awsClient: AWSKMSAsync, asyncClient: AsyncKMSClient, streamingClient: StreamingKMSClient)

  private def withFixture(test: Fixture ⇒ Any): Unit = {
    val aws = mock[AWSKMSAsync]("aws")
    val streaming = new DefaultStreamingKMSClient(aws)
    val async = new DefaultAsyncKMSClient(streaming)

    test(Fixture(aws, async, streaming))
  }

  "the default KMS client can" - {
    "create keys" - {
      val keyMetadata = KeyMetadata("arn:key", "42", new Date, None, None, enabled = true, keyIdentifier, KeyState.Enabled,
        KeyUsage.EncryptDecrypt)
      def withCreateKeyFixture(description: String, policy: String)(test: Fixture ⇒ Any): Unit = {
        withFixture { f ⇒
          (f.awsClient.createKeyAsync(_: AWSCreateKeyRequest, _: AsyncHandler[AWSCreateKeyRequest, CreateKeyResult]))
            .expects(whereRequest(request ⇒
              request.getPolicy == policy &&
                request.getDescription == description &&
                request.getKeyUsage == KeyUsageType.ENCRYPT_DECRYPT.toString
            ))
            .withAwsSuccess(new CreateKeyResult().withKeyMetadata(keyMetadata.toAws))
          (f.awsClient.createAliasAsync(_: CreateAliasRequest, _: AsyncHandler[CreateAliasRequest,Void]))
            .expects(whereRequest(request ⇒
              request.getAliasName == s"alias/$alias" &&
                request.getTargetKeyId == keyIdentifier
            ))
            .withVoidAwsSuccess()

          test(f)
        }
      }

      val description = "a description"
      val policy = "a policy"

      "with no optional arguments" in withCreateKeyFixture(null, null) { f ⇒
        val result = f.asyncClient.createKey(alias).futureValue
        result shouldBe keyMetadata
      }

      "using a alias that already includes the `alias/` prefix" in withCreateKeyFixture(null, null) { f ⇒
        val result = f.asyncClient.createKey(s"alias/$alias").futureValue
        result shouldBe keyMetadata
      }

      "with a description" in withCreateKeyFixture(description, null) { f ⇒
        val result = f.asyncClient.createKey(alias, description).futureValue
        result shouldBe keyMetadata
      }

      "with a policy" in withCreateKeyFixture(null, policy) { f ⇒
        val result = f.asyncClient.createKey(CreateKeyRequest(alias, policy = Some(policy))).futureValue
        result shouldBe keyMetadata
      }

      "with a description and a policy" in withCreateKeyFixture(description, policy) { f ⇒
        val request = CreateKeyRequest(alias, description = Some(description), policy = Some(policy))
        val result = f.asyncClient.createKey(request).futureValue
        result shouldBe keyMetadata
      }
    }

    "schedule deletion of a key" in withFixture { f ⇒
      val keyId = "someKeyId"
      val days = 15

      val date = new Date(System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000))

      (f.awsClient.scheduleKeyDeletionAsync(_: ScheduleKeyDeletionRequest, _: AsyncHandler[ScheduleKeyDeletionRequest, ScheduleKeyDeletionResult]))
        .expects(whereRequest(request ⇒ request.getKeyId == keyId && request.getPendingWindowInDays == days))
        .withAwsSuccess(new ScheduleKeyDeletionResult().withKeyId(keyId).withDeletionDate(date))

      val result = f.asyncClient.scheduleKeyDeletion(keyId, days).futureValue
      result shouldBe date
    }

    "request cancellation of a key deletion" in withFixture { f ⇒
      val keyId = "someKeyId"

      (f.awsClient.cancelKeyDeletionAsync(_: CancelKeyDeletionRequest, _: AsyncHandler[CancelKeyDeletionRequest, CancelKeyDeletionResult]))
        .expects(whereRequest(_.getKeyId == keyId))
        .withAwsSuccess(new CancelKeyDeletionResult().withKeyId(keyId))

      val result = f.asyncClient.cancelKeyDeletion(keyId).futureValue
      result shouldBe Done
    }

    "enable keys" in withFixture { f ⇒
      val keyId = "someKeyId"

      (f.awsClient.enableKeyAsync(_: EnableKeyRequest, _: AsyncHandler[EnableKeyRequest, Void]))
        .expects(whereRequest(_.getKeyId == keyId))
        .withVoidAwsSuccess()

      val result = f.asyncClient.enableKey(keyId).futureValue
      result shouldBe Done
    }

    "disable keys" in withFixture { f ⇒
      val keyId = "someKeyId"

      (f.awsClient.disableKeyAsync(_: DisableKeyRequest, _: AsyncHandler[DisableKeyRequest, Void]))
        .expects(whereRequest(_.getKeyId == keyId))
        .withVoidAwsSuccess()

      val result = f.asyncClient.disableKey(keyId).futureValue
      result shouldBe Done
    }


    "list keys with aliases" in withFixture { f ⇒
      val listing = Seq.tabulate(20) { i ⇒
        if ((i % 2) == 0) {
          ListEntry(s"id$i", s"arn:key:$i", None, None)
        } else {
          ListEntry(s"id$i", s"arn:key:$i", Some(s"alias$i"), Some(s"arn:alias:$i"))
        }
      }

      (f.awsClient.listAliasesAsync(_: ListAliasesRequest, _: AsyncHandler[ListAliasesRequest, ListAliasesResult]))
        .expects(whereRequest(_.getMarker == null))
        .withAwsSuccess {
          val aliases = listing.filter(_.aliasName.isDefined).map { entry ⇒
            new AliasListEntry()
              .withAliasArn(entry.aliasArn.get)
              .withAliasName(s"alias/${entry.aliasName.get}")
              .withTargetKeyId(entry.keyId)
          }
          new ListAliasesResult().withAliases(aliases: _*)
        }
      (f.awsClient.listKeysAsync(_: ListKeysRequest, _: AsyncHandler[ListKeysRequest, ListKeysResult]))
        .expects(whereRequest(_.getMarker == null))
        .withAwsSuccess {
          val keys = listing.map { entry ⇒
            new KeyListEntry()
              .withKeyId(entry.keyId)
              .withKeyArn(entry.keyArn)
          }
          new ListKeysResult().withKeys(keys: _*)
        }

      val result = f.asyncClient.listKeys().futureValue
      result shouldBe listing
    }

    "describe a key" - {
      val metadata = KeyMetadata("arn:key", "42", new Date, None, None, enabled = true, keyIdentifier, KeyState.Enabled,
        KeyUsage.EncryptDecrypt)
      def expectDescribeKeyAsync(client: AWSKMSAsync, keyId: String): Unit = {
        (client.describeKeyAsync(_: DescribeKeyRequest, _: AsyncHandler[DescribeKeyRequest, DescribeKeyResult]))
          .expects(whereRequest(_.getKeyId == keyId))
          .withAwsSuccess(new DescribeKeyResult().withKeyMetadata(metadata.toAws))
      }

      "given a UUID" in withFixture { f ⇒
        val uuid = UUID.randomUUID().toString

        expectDescribeKeyAsync(f.awsClient, uuid)

        val result = f.asyncClient.describeKey(uuid).futureValue
        result shouldBe Some(metadata)
      }

      "given an arn" in withFixture { f ⇒
        val arn = "arn:foo:bar"

        expectDescribeKeyAsync(f.awsClient, arn)

        val result = f.asyncClient.describeKey(arn).futureValue
        result shouldBe Some(metadata)
      }

      "given an explicit alias" in withFixture { f ⇒
        val alias = "alias/MyAlias"

        expectDescribeKeyAsync(f.awsClient, alias)

        val result = f.asyncClient.describeKey(alias).futureValue
        result shouldBe Some(metadata)
      }

      "given an implicit alias" in withFixture { f ⇒
        val alias = "MyAlias"

        expectDescribeKeyAsync(f.awsClient, s"alias/$alias")

        val result = f.asyncClient.describeKey(alias).futureValue
        result shouldBe Some(metadata)
      }

      "that is not there" in withFixture { f ⇒
        val notThere = UUID.randomUUID().toString

        (f.awsClient.describeKeyAsync(_: DescribeKeyRequest, _: AsyncHandler[DescribeKeyRequest, DescribeKeyResult]))
          .expects(whereRequest(_.getKeyId == notThere))
          .withAwsError {
            val notFound = new AmazonServiceException("It’s gone!")
            notFound.setErrorCode("NotFoundException")
            notFound
          }

        val result = f.asyncClient.describeKey(notThere).futureValue
        result shouldBe empty
      }
    }

    "can generate a data key" - {
      val ciphertext = Array.tabulate(64)(i ⇒ i.toByte)
      val plaintext = Array.tabulate(64)(i ⇒ (i * 3).toByte)

      def mockWithoutPlaintextCall(aws: AWSKMSAsync, context: Map[String,String] = Map.empty): Unit = {
        (aws.generateDataKeyWithoutPlaintextAsync(_: GenerateDataKeyWithoutPlaintextRequest, _: AsyncHandler[GenerateDataKeyWithoutPlaintextRequest, GenerateDataKeyWithoutPlaintextResult]))
          .expects(whereRequest(request ⇒
            request.getKeyId == keyIdentifier &&
              request.getKeySpec == AWSDataKeySpec.AES_256.toString &&
              request.getNumberOfBytes == null &&
              request.getEncryptionContext.asScala == context &&
              request.getGrantTokens.isEmpty
          ))
          .withAwsSuccess {
            val result = new GenerateDataKeyWithoutPlaintextResult
            result.setKeyId(keyIdentifier)
            result.setCiphertextBlob(ByteBuffer.wrap(ciphertext))
            result
          }
      }

      def mockWithPlaintextCall(aws: AWSKMSAsync): Unit = {
        (aws.generateDataKeyAsync(_: AWSGenerateDataKeyRequest, _: AsyncHandler[AWSGenerateDataKeyRequest, GenerateDataKeyResult]))
          .expects(whereRequest(request ⇒
            request.getKeyId == keyIdentifier &&
              request.getKeySpec == AWSDataKeySpec.AES_256.toString &&
              request.getNumberOfBytes == null &&
              request.getEncryptionContext.isEmpty &&
              request.getGrantTokens.isEmpty
          ))
          .withAwsSuccess {
            val result = new GenerateDataKeyResult
            result.setKeyId(keyIdentifier)
            result.setCiphertextBlob(ByteBuffer.wrap(ciphertext))
            result.setPlaintext(ByteBuffer.wrap(plaintext))
            result
          }
      }

      "without plaintext" in withFixture { f ⇒
        mockWithoutPlaintextCall(f.awsClient)

        val result = f.asyncClient.generateDataKey(keyIdentifier).futureValue
        result.keyId shouldBe keyIdentifier
        result.ciphertext shouldBe ciphertext
        result.plaintext shouldBe None
      }

      "with context (no plaintext)" in withFixture { f ⇒
        val context = Map("some" → "context")
        mockWithoutPlaintextCall(f.awsClient, context)

        val result = f.asyncClient.generateDataKey(keyIdentifier, context).futureValue
        result.keyId shouldBe keyIdentifier
        result.ciphertext shouldBe ciphertext
        result.plaintext shouldBe None
      }

      "with plaintext" in withFixture { f ⇒
        mockWithPlaintextCall(f.awsClient)

        val result = f.asyncClient.generateDataKey(GenerateDataKeyRequest(keyIdentifier, includePlaintext = true)).futureValue
        result.keyId shouldBe keyIdentifier
        result.ciphertext shouldBe ciphertext
        result.plaintext should contain (plaintext)
      }

      "both kinds in the same flow" in withFixture { f ⇒
        val requests = List(
          GenerateDataKeyRequest(keyIdentifier),
          GenerateDataKeyRequest(keyIdentifier, includePlaintext = true)
        )

        mockWithoutPlaintextCall(f.awsClient)
        mockWithPlaintextCall(f.awsClient)

        val result = Source(requests).via(f.streamingClient.dataKeyGenerator).runWith(Sink.seq).futureValue

        result should have size 2
        result.head.plaintext shouldBe None
        result(1).plaintext shouldBe defined
      }
    }

    "can encrypt" - {
      val plaintext = Array.fill(16)(0.toByte)
      val ciphertext = Array.fill(16)(1.toByte)

      "without context" in withFixture { f ⇒
        (f.awsClient.encryptAsync(_: AWSEncryptRequest, _: AsyncHandler[AWSEncryptRequest, EncryptResult]))
          .expects(whereRequest(request ⇒
            request.getKeyId == keyIdentifier &&
              request.getPlaintext.array() == plaintext &&
              request.getEncryptionContext.isEmpty &&
              request.getGrantTokens.isEmpty
          ))
          .withAwsSuccess(new EncryptResult().withKeyId(keyIdentifier).withCiphertextBlob(ByteBuffer.wrap(ciphertext)))

        val result = f.asyncClient.encrypt(keyIdentifier, plaintext).futureValue
        result shouldBe ciphertext
      }

      "with context" in withFixture { f ⇒
        val context = Map("some" → "context")

        (f.awsClient.encryptAsync(_: AWSEncryptRequest, _: AsyncHandler[AWSEncryptRequest, EncryptResult]))
          .expects(whereRequest(request ⇒
            request.getKeyId == keyIdentifier &&
              request.getPlaintext.array() == plaintext &&
              request.getEncryptionContext.asScala == context &&
              request.getGrantTokens.isEmpty
          ))
          .withAwsSuccess(new EncryptResult().withKeyId(keyIdentifier).withCiphertextBlob(ByteBuffer.wrap(ciphertext)))

        val result = f.asyncClient.encrypt(keyIdentifier, plaintext, context).futureValue
        result shouldBe ciphertext
      }

      "with a request" in withFixture { f ⇒
        val grantTokens = Seq("a", "b", "c")

        (f.awsClient.encryptAsync(_: AWSEncryptRequest, _: AsyncHandler[AWSEncryptRequest, EncryptResult]))
          .expects(whereRequest(request ⇒
            request.getKeyId == keyIdentifier &&
              request.getPlaintext.array() == plaintext &&
              request.getEncryptionContext.isEmpty &&
              request.getGrantTokens.asScala == grantTokens
          ))
          .withAwsSuccess(new EncryptResult().withKeyId(keyIdentifier).withCiphertextBlob(ByteBuffer.wrap(ciphertext)))

        val result = f.asyncClient.encrypt(EncryptRequest(keyIdentifier, plaintext, grantTokens = grantTokens)).futureValue
        result shouldBe ciphertext
      }
    }

    "can decrypt" - {
      val plaintext = Array.fill(16)(0.toByte)
      val ciphertext = Array.fill(16)(1.toByte)

      "without context" in withFixture { f ⇒
        (f.awsClient.decryptAsync(_: AWSDecryptRequest, _: AsyncHandler[AWSDecryptRequest, DecryptResult]))
          .expects(whereRequest(request ⇒
            request.getCiphertextBlob.array() == ciphertext &&
              request.getEncryptionContext.isEmpty &&
              request.getGrantTokens.isEmpty
          ))
          .withAwsSuccess(new DecryptResult().withKeyId(keyIdentifier).withPlaintext(ByteBuffer.wrap(plaintext)))

        val result = f.asyncClient.decrypt(ciphertext).futureValue
        result shouldBe plaintext
      }

      "with context" in withFixture { f ⇒
        val context = Map("some" → "context")

        (f.awsClient.decryptAsync(_: AWSDecryptRequest, _: AsyncHandler[AWSDecryptRequest, DecryptResult]))
          .expects(whereRequest(request ⇒
            request.getCiphertextBlob.array() == ciphertext &&
              request.getEncryptionContext.asScala == context &&
              request.getGrantTokens.isEmpty
          ))
          .withAwsSuccess(new DecryptResult().withKeyId(keyIdentifier).withPlaintext(ByteBuffer.wrap(plaintext)))

        val result = f.asyncClient.decrypt(ciphertext, context).futureValue
        result shouldBe plaintext
      }

      "with a request" in withFixture { f ⇒
        val grantTokens = Seq("a", "b", "c")

        (f.awsClient.decryptAsync(_: AWSDecryptRequest, _: AsyncHandler[AWSDecryptRequest, DecryptResult]))
          .expects(whereRequest(request ⇒
            request.getCiphertextBlob.array() == ciphertext &&
              request.getEncryptionContext.isEmpty &&
              request.getGrantTokens.asScala == grantTokens
          ))
          .withAwsSuccess(new DecryptResult().withKeyId(keyIdentifier).withPlaintext(ByteBuffer.wrap(plaintext)))

        val result = f.asyncClient.decrypt(DecryptRequest(ciphertext, grantTokens = grantTokens)).futureValue
        result shouldBe plaintext
      }
    }
  }
}
