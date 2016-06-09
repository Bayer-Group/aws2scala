package com.monsanto.arch.awsutil.kms

import java.util.{Date, UUID}

import com.monsanto.arch.awsutil.kms.model.{GenerateDataKeyRequest, KeyArn}
import com.monsanto.arch.awsutil.test_support.AwsScalaFutures._
import com.monsanto.arch.awsutil.test_support.{AwsIntegrationSpec, IntegrationTest}
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.Eventually._

import scala.concurrent.duration.DurationInt

@IntegrationTest
class KMSClientIntegrationSpec extends FreeSpec with AwsIntegrationSpec with StrictLogging {
  val streamingClient = awsClient.streaming(KMS)
  val asyncClient = awsClient.async(KMS)
  val testPrefix = "aws2scala-it-kms"
  val alias = s"$testPrefix-$testId"
  var keyId: String = _
  var keyArn: KeyArn = _
  var aliasArn: String = _

  "the default KMS client should" - {
    "create a key" in {
      logger.info(s"Creating key with alias $alias")
      val description = "aws2scala KMS integration spec key"
      val keyMetadata = asyncClient.createKey(alias, description).futureValue
      keyMetadata.description shouldBe Some(description)
      keyId = keyMetadata.id
      keyArn = keyMetadata.arn
      logger.info(s"Key has ID $keyId and ARN $keyArn")
    }

    "find the key and alias in a listing" in {
      val listing = asyncClient.listKeys().futureValue
      listing.map(_.keyId) should contain(keyId)
      val entry = listing.find(_.keyId == keyId).get
      entry.keyArn shouldBe keyArn.arnString
      entry.aliasName should contain (alias)
      aliasArn = entry.aliasArn.get
      logger.info(s"Key has alias ARN $aliasArn")
    }

    "describe the key given" - {
      "the key ID" in {
        val maybeMetadata = asyncClient.describeKey(keyId).futureValue
        maybeMetadata shouldBe defined
        maybeMetadata.get.arn shouldBe keyArn
      }

      "the key ARN" in {
        val maybeMetadata = asyncClient.describeKey(keyArn.arnString).futureValue
        maybeMetadata shouldBe defined
        maybeMetadata.get.id shouldBe keyId
      }

      "the alias ARN" in {
        val maybeMetadata = asyncClient.describeKey(aliasArn).futureValue
        maybeMetadata shouldBe defined
        maybeMetadata.get.id shouldBe keyId
      }

      "the full alias" in {
        val maybeMetadata = asyncClient.describeKey(s"alias/$alias").futureValue
        maybeMetadata shouldBe defined
        maybeMetadata.get.id shouldBe keyId
      }

      "the simple alias" in {
        val maybeMetadata = asyncClient.describeKey(alias).futureValue
        maybeMetadata shouldBe defined
        maybeMetadata.get.id shouldBe keyId
      }
    }

    "handle describing a key that does not exist" in {
      val maybeMetadata = asyncClient.describeKey(keyArn + UUID.randomUUID().toString).futureValue
      maybeMetadata shouldBe empty
    }

    "disable the key" in {
      asyncClient.describeKey(keyArn.arnString).futureValue.get.enabled shouldBe true
      asyncClient.disableKey(keyId)
      eventually {
        asyncClient.describeKey(keyArn.arnString).futureValue.get.enabled shouldBe false
      }(Eventually.PatienceConfig(2.minutes, 1.second))
    }

    "enable the key" in {
      asyncClient.describeKey(keyId).futureValue.get.enabled shouldBe false
      asyncClient.enableKey(keyArn.arnString)
      eventually {
        asyncClient.describeKey(keyId).futureValue.get.enabled shouldBe true
      }(Eventually.PatienceConfig(2.minutes, 1.second))
    }

    "generate and decrypt a data key" in {
      val dataKey = asyncClient.generateDataKey(GenerateDataKeyRequest(alias, includePlaintext = true)).futureValue

      dataKey.keyId shouldBe keyArn.arnString
      dataKey.ciphertext.length shouldBe > (0)
      dataKey.plaintext shouldBe defined

      val decrypted = asyncClient.decrypt(dataKey.ciphertext).futureValue

      decrypted shouldBe dataKey.plaintext.get
    }

    "encrypt and decrypt something" in {
      val secret = "Setec Astronomy"
      val context = Map("year" → "1992")

      val encrypted = asyncClient.encrypt(alias, secret.getBytes, context).futureValue
      val decrypted = asyncClient.decrypt(encrypted, context).futureValue

      new String(decrypted) shouldBe secret
    }

    "schedule deletion of a key" in {
      val deleteDate = asyncClient.scheduleKeyDeletion(keyId).futureValue
      val aDay = 24 * 60 * 60 * 1000L
      val thirtyDaysFromNow = System.currentTimeMillis() + (30 * aDay)

      deleteDate shouldBe >=(new Date(thirtyDaysFromNow - aDay))
      deleteDate shouldBe <=(new Date(thirtyDaysFromNow + aDay))

      asyncClient.describeKey(keyArn.arnString).futureValue.get.deletionDate shouldBe Some(deleteDate)
    }

    "cancel deletion of a key" in {
      asyncClient.describeKey(keyArn.arnString).futureValue.get.deletionDate shouldBe defined

      asyncClient.cancelKeyDeletion(keyArn.arnString).futureValue

      asyncClient.describeKey(keyArn.arnString).futureValue.get.deletionDate shouldBe empty
    }

    "schedule deletion of a key in seven days" in {
      val deleteDate = asyncClient.scheduleKeyDeletion(keyId, 7).futureValue
      val aDayMillis = 24 * 60 * 60 * 1000

      val aWeekFromNowMillis = System.currentTimeMillis() + (7 * aDayMillis)

      deleteDate shouldBe >=(new Date(aWeekFromNowMillis - aDayMillis))
      deleteDate shouldBe <=(new Date(aWeekFromNowMillis + aDayMillis))

      logger.info(s"Key scheduled for deletion on $deleteDate")
    }
  }
}
