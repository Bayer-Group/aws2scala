package com.monsanto.arch.awsutil.s3.model

import java.io.File
import java.net.URL
import java.util.Date

import akka.stream.Materializer
import com.amazonaws.services.s3.{model ⇒ aws}
import com.monsanto.arch.awsutil.Materialised
import com.monsanto.arch.awsutil.s3.{AsyncS3Client, DownloadSink}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.Future

class ObjectSpec extends FreeSpec with MockFactory with Materialised {
  private val who = System.getProperty("user.name")
  private val bucketName = "some-bucket"
  private val key = "key"
  private val obj = Object(bucketName, key, "foo", new Date(), Owner(who, who), 42, StorageClass.Standard)
  private val unit = ()

  "an Object instance can" - {
    "delete itself" in {
      implicit val client = mock[AsyncS3Client]("asyncS3Client")
      (client.deleteObject(_: String, _: String)(_: Materializer))
        .expects(bucketName, key, materialiser)
        .returning(Future.successful(BucketNameAndKey(bucketName, key)))

      obj.delete().futureValue shouldBe unit
    }

    "download itself" - {
      "as a string" in {
        implicit val client = mock[AsyncS3Client]("asyncS3Client")
        val content = "some content"
        (client.download(_: String, _: String)(_: DownloadSink[String], _: Materializer))
          .expects(bucketName, key, DownloadSink.stringSink, materialiser)
          .returning(Future.successful(content))

        obj.download[String]().futureValue shouldBe content
      }

      "as an array of bytes" in {
        implicit val client = mock[AsyncS3Client]("asyncS3Client")
        val content = Array.tabulate(1024)(i ⇒ i.toByte)
        (client.download(_: String, _: String)(_: DownloadSink[Array[Byte]], _: Materializer))
          .expects(bucketName, key, DownloadSink.bytesSink, materialiser)
          .returning(Future.successful(content))

        obj.download[Array[Byte]]().futureValue shouldBe content
      }

      "to a file" in {
        implicit val client = mock[AsyncS3Client]("asyncS3Client")
        val file = new File("file")

        (client.downloadTo(_: String, _: String, _: File)(_: Materializer))
          .expects(bucketName, key, file, materialiser)
          .returning(Future.successful(file))

        obj.downloadTo(file).futureValue shouldBe file
      }
    }

    "copy itself" in {
      implicit val client = mock[AsyncS3Client]
      val destKey = "copy"
      val copied = Object(obj.bucketName, destKey, "ETAG", obj.lastModified, obj.owner, obj.size, obj.storageClass)

      (client.copy(_: String, _: String, _: String, _: String)(_: Materializer))
        .expects(bucketName, key, bucketName, destKey, materialiser)
        .returning {
          val summary = new aws.S3ObjectSummary
          summary.setBucketName(copied.bucketName)
          summary.setKey(copied.key)
          summary.setETag(copied.eTag)
          summary.setLastModified(copied.lastModified)
          summary.setOwner(new aws.Owner(copied.owner.id, copied.owner.displayName))
          summary.setSize(copied.size)
          summary.setStorageClass(copied.storageClass.toAws.toString)
          Future.successful(summary)
        }

      obj.copy(destKey).futureValue shouldBe copied
    }

    "get its URL" in {
      implicit val client = mock[AsyncS3Client]
      val url = new URL(s"https://$bucketName.s3.amazonaws.com/$key")

      (client.getUrl(_: String, _: String)(_: Materializer))
        .expects(bucketName, key, materialiser)
        .returning(Future.successful(url))

      obj.getUrl().futureValue shouldBe url
    }
  }
}
