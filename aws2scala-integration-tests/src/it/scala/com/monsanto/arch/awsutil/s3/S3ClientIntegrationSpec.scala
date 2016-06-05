package com.monsanto.arch.awsutil.s3

import java.io.{File, PrintWriter}
import java.net.URL
import java.util.UUID

import com.amazonaws.services.s3.model.S3ObjectSummary
import com.monsanto.arch.awsutil.AwsSettings
import com.monsanto.arch.awsutil.s3.model.Bucket
import com.monsanto.arch.awsutil.test_support.AwsScalaFutures._
import com.monsanto.arch.awsutil.test_support.{AwsIntegrationSpec, IntegrationCleanup, IntegrationTest, TestDefaults}
import com.typesafe.scalalogging.StrictLogging
import org.scalactic.Equality
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually._
import spray.json.JsonParser

@IntegrationTest
class S3ClientIntegrationSpec extends FreeSpec with AwsIntegrationSpec with StrictLogging with IntegrationCleanup {
  val streamingClient = awsClient.streaming(S3)
  val asyncClient = awsClient.async(S3)
  val bucketPrefix = s"aws2scala-it-s3"
  val bucketName = s"$bucketPrefix-$testId"
  val stringKey = "string.txt"
  val stringContent = "This is a string."
  val bytesKey = "bytes.data"
  val bytesContent = Array.tabulate[Byte](200)(i ⇒ i.toByte)
  val fileKey = "file.uuids"
  val fileContent = Seq.fill(100){ UUID.randomUUID() }.mkString("\n")
  val defaultPolicy = JsonParser(AwsSettings.Default.s3.defaultBucketPolicy.get.replaceAll("@BUCKET_NAME@", bucketName))

  "the default Async S3 client can" - {
    "create a bucket" in {
      val result = asyncClient.createBucket(bucketName).futureValue
      result.name shouldBe bucketName
      logger.info(s"Created bucket $bucketName")
      eventually {
        asyncClient.doesBucketExist(bucketName).futureValue shouldBe true
      }
      setDefaultBucketTags()
    }

    "check the bucket exists" in {
      val result = asyncClient.doesBucketExist(bucketName).futureValue
      result shouldBe true
    }

    "list the buckets" in {
      val result = asyncClient.listBuckets().futureValue
      (result should contain (bucketName)) (decided by theBucketName)
    }

    "remove the policy from the bucket" in {
      val setResult = asyncClient.deleteBucketPolicy(bucketName).futureValue
      setResult shouldBe bucketName

      val getResult = asyncClient.getBucketPolicy(bucketName).futureValue
      getResult shouldBe empty
    }

    "set a policy for the bucket" in {
      val setResult = asyncClient.setBucketPolicy(bucketName, defaultPolicy.prettyPrint).futureValue
      setResult shouldBe bucketName

      val getResult = asyncClient.getBucketPolicy(bucketName).futureValue
      getResult shouldBe defined
      JsonParser(getResult.get) shouldBe defaultPolicy
    }

    "verify that the bucket has the default tags" in {
      val result = asyncClient.getBucketTags(bucketName).futureValue
      result shouldBe TestDefaults.Tags
    }

    "set some tags on the bucket" in {
      val tags = TestDefaults.Tags ++ Map("test" → "tag")

      val setResult = asyncClient.setBucketTags(bucketName, tags).futureValue
      setResult shouldBe bucketName

      val getResult = asyncClient.getBucketTags(bucketName).futureValue
      getResult shouldBe tags
    }

    "clear the tags on the bucket" in {
      val setResult = asyncClient.deleteBucketTags(bucketName).futureValue
      setResult shouldBe bucketName

      val getResult = asyncClient.getBucketTags(bucketName).futureValue
      getResult shouldBe empty

      setDefaultBucketTags()
    }

    "upload" - {
      "a byte array" in {
        val result = asyncClient.upload(bucketName, bytesKey, bytesContent).futureValue
        result.getBucketName shouldBe bucketName
        result.getKey shouldBe bytesKey
      }

      "a string" in {
        val result = asyncClient.upload(bucketName, stringKey, stringContent).futureValue
        result.getBucketName shouldBe bucketName
        result.getKey shouldBe stringKey
      }

      "a file" in {
        val sourceFile = File.createTempFile("file", ".uuids")
        sourceFile.deleteOnExit()
        try {
          val out = new PrintWriter(sourceFile, "UTF-8")
          try {
            out.print(fileContent)
          } finally out.close()
          val result = asyncClient.upload(bucketName, fileKey, sourceFile).futureValue
          result.getBucketName shouldBe bucketName
          result.getKey shouldBe fileKey
        } finally sourceFile.delete()
      }
    }

    "list bucket contents" - {
      val theKey = new Equality[S3ObjectSummary] {
        override def areEqual(a: S3ObjectSummary, b: Any): Boolean = {
          b match {
            case s: String => a.getKey == s
          }
        }
      }

      "using a prefix" in {
        val keyListing = asyncClient.listObjects(bucketName, stringKey).futureValue
        keyListing should have size 1
        (keyListing should contain(stringKey)) (decided by theKey)
      }

      "all contents" in {
        val allListing = asyncClient.listObjects(bucketName).futureValue
        (allListing should contain(stringKey)) (decided by theKey)
        (allListing should contain(bytesKey)) (decided by theKey)
        (allListing should contain(fileKey)) (decided by theKey)
      }
    }

    "download" - {
      "a byte array" in {
        val result = asyncClient.download[Array[Byte]](bucketName, bytesKey).futureValue
        result shouldBe bytesContent
      }

      "a string" in {
        val result = asyncClient.download[String](bucketName, stringKey).futureValue
        result shouldBe stringContent
      }

      "to a file" in {
        val dest = File.createTempFile("file", "uuids")
        dest.deleteOnExit()
        try {
          val result = asyncClient.downloadTo(bucketName, fileKey, dest).futureValue
          result shouldBe dest
          io.Source.fromFile(dest).mkString shouldBe fileContent
        } finally dest.delete()
      }
    }

    "copy an object" in {
      val destKey = "copy.txt"

      val copyResult = asyncClient.copy(bucketName, stringKey, destKey).futureValue
      copyResult.getBucketName shouldBe bucketName
      copyResult.getKey shouldBe destKey

      val downloadResult = asyncClient.download[String](bucketName, destKey).futureValue
      downloadResult shouldBe stringContent
    }

    "get the URL of an object" in {
      val result = asyncClient.getUrl(bucketName, stringKey).futureValue
      result shouldBe new URL(s"https://$bucketName.s3.amazonaws.com/$stringKey")
    }

    "empty and delete the bucket" in {
      logger.info(s"Removing bucket $bucketName…")
      val result = asyncClient.emptyAndDeleteBucket(bucketName).futureValue
      result shouldBe bucketName
      eventually {
        asyncClient.doesBucketExist(bucketName).futureValue shouldBe false
      }
    }

    behave like cleanupS3Buckets(bucketPrefix)
  }

  private val theBucketName = new Equality[Bucket] {
    override def areEqual(a: Bucket, b: Any): Boolean = {
      b match {
        case s: String => a.name == s
      }
    }
  }

  private def setDefaultBucketTags(): Unit = {
    asyncClient.setBucketTags(bucketName, TestDefaults.Tags).futureValue
  }
}
