package com.monsanto.arch.awsutil.s3.model

import java.io.File
import java.util.Date

import akka.stream.Materializer
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.services.s3.{model ⇒ aws}
import com.monsanto.arch.awsutil.Materialised
import com.monsanto.arch.awsutil.s3.{AsyncS3Client, UploadSource}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.Future

class BucketSpec extends FreeSpec with MockFactory with Materialised {
  private val who = System.getProperty("user.name")
  private val bucketName = "some-bucket"
  private val bucket = Bucket(bucketName, Owner(who,who), new Date())
  private val unit = ()

  "a Bucket instance can" - {
    "delete itself" in {
      implicit val client = mock[AsyncS3Client]
      (client.deleteBucket(_: String)(_: Materializer)).expects(bucketName, materialiser).returning(Future.successful(bucketName))

      bucket.delete().futureValue shouldBe unit
    }

    "check if it really exists" in {
      implicit val client = mock[AsyncS3Client]
      (client.doesBucketExist(_: String)(_: Materializer))
        .expects(bucketName, materialiser)
        .returning(Future.successful(true))

      bucket.exists().futureValue shouldBe true
    }

    "can get its policy" in {
      implicit val client = mock[AsyncS3Client]
      val policy = Some("policy text")
      (client.getBucketPolicy(_: String)(_: Materializer))
        .expects(bucketName, materialiser)
        .returning(Future.successful(policy))

      bucket.getPolicy().futureValue shouldBe policy
    }

    "can set its policy" - {
      "using a plain string" in {
        implicit val client = mock[AsyncS3Client]
        val policy = "policy text"
        (client.setBucketPolicy(_: String, _: Option[String])(_: Materializer))
          .expects(bucketName, Some(policy), materialiser)
          .returning(Future.successful(bucketName))

        bucket.setPolicy(policy).futureValue shouldBe unit
      }

      "using an option" in {
        implicit val client = mock[AsyncS3Client]
        val policy = "policy text"
        (client.setBucketPolicy(_: String, _: Option[String])(_: Materializer))
          .expects(bucketName, Some(policy), materialiser)
          .returning(Future.successful(bucketName))

        bucket.setPolicy(Some(policy)).futureValue shouldBe unit
      }
    }

    "can remove its policy" - {
      "using setPolicy" in {
        implicit val client = mock[AsyncS3Client]
        (client.setBucketPolicy(_: String, _: Option[String])(_: Materializer))
          .expects(bucketName, None, materialiser)
          .returning(Future.successful(bucketName))

        bucket.setPolicy(None).futureValue shouldBe unit
      }

      "using deletePolicy" in {
        implicit val client = mock[AsyncS3Client]
        (client.setBucketPolicy(_: String, _: Option[String])(_: Materializer))
          .expects(bucketName, None, materialiser)
          .returning(Future.successful(bucketName))

        bucket.deletePolicy().futureValue shouldBe unit
      }
    }

    "can get its tags" in {
      val tags = Map("key" → "value")

      implicit val client = mock[AsyncS3Client]
      (client.getBucketTags(_: String)(_: Materializer))
        .expects(bucketName, materialiser)
        .returning(Future.successful(tags))

      bucket.getTags().futureValue shouldBe tags
    }

    "can set its tags" in {
      val tags = Map("key" → "value")

      implicit val client = mock[AsyncS3Client]
      (client.setBucketTags(_: String, _: Map[String,String])(_: Materializer))
        .expects(bucketName, tags, materialiser)
        .returning(Future.successful(bucketName))

      bucket.setTags(tags).futureValue shouldBe unit
    }

    "can delete its tags" in {
      implicit val client = mock[AsyncS3Client]
      (client.deleteBucketTags(_: String)(_: Materializer))
        .expects(bucketName, materialiser)
        .returning(Future.successful(bucketName))

      bucket.deleteTags().futureValue shouldBe unit
    }

    "list objects within itself" - {
      "all of them" in {
        implicit val client = mock[AsyncS3Client]
        val storageClasses = IndexedSeq(StorageClass.Glacier, StorageClass.ReducedRedundancy, StorageClass.Standard,
          StorageClass.StandardInfrequentAccess)
        val nextStorageClass = Stream.from(0).map(i ⇒ storageClasses(i % storageClasses.size)).iterator
        val objects = Seq.tabulate(10) { i ⇒
          Object(bucketName, s"object$i", "some-etag", new Date(), bucket.owner, i, nextStorageClass.next())
        }
        (client.listObjects(_: String)(_: Materializer))
          .expects(bucketName, materialiser)
          .returning(Future.successful(objects.map(toAws)))

        bucket.list().futureValue shouldBe objects
      }

      "with a prefix" in {
        implicit val client = mock[AsyncS3Client]
        val prefix = "prefix"
        val objects = Seq.tabulate(10) { i ⇒
          Object(bucketName, s"$prefix$i", "some-etag", new Date(), bucket.owner, i, StorageClass.Standard)
        }
        (client.listObjects(_: String, _: String)(_: Materializer))
          .expects(bucketName, prefix, materialiser)
          .returning(Future.successful(objects.map(toAws)))

        bucket.list(prefix).futureValue shouldBe objects
      }
    }

    "empty a bucket " in {
      implicit val client = mock[AsyncS3Client]
      (client.emptyBucket(_:String)(_:Materializer))
        .expects(bucketName,materialiser)
        .returning(Future.successful(bucketName))
      bucket.empty().futureValue shouldBe unit
    }

    "delete and empty a bucket " in {
      implicit val client = mock[AsyncS3Client]
      (client.emptyAndDeleteBucket(_:String)(_:Materializer))
        .expects(bucketName,materialiser)
        .returning(Future.successful(bucketName))
      bucket.emptyAndDelete().futureValue shouldBe unit
    }

    "upload" - {
      "a string" in {
        implicit val client = mock[AsyncS3Client]
        val key = "string.txt"
        val content = "some data"

        val uploadedObject = Object(bucketName, key, "ETAG", new Date(), bucket.owner, content.getBytes.length,
          StorageClass.Standard)

        (client.upload(_: String, _: String, _: String)(_: UploadSource[String], _: Materializer))
          .expects(bucketName, key, content, UploadSource.stringSource, materialiser)
          .returning(Future.successful(toAws(uploadedObject)))

        bucket.upload(key, content).futureValue shouldBe uploadedObject
      }

      "an array of bytes" in {
        implicit val client = mock[AsyncS3Client]
        val key = "bytes.data"
        val content = Array.fill(100)(0.toByte)

        val uploadedObject = Object(bucketName, key, "ETAG", new Date(), bucket.owner, content.length,
          StorageClass.Standard)

        (client.upload(_: String, _: String, _: Array[Byte])(_: UploadSource[Array[Byte]], _:Materializer))
          .expects(bucketName, key, content, UploadSource.bytesSource, materialiser)
          .returning(Future.successful(toAws(uploadedObject)))

        bucket.upload(key, content).futureValue shouldBe uploadedObject
      }

      "a file" in {
        implicit val client = mock[AsyncS3Client]
        val key = "bytes.data"
        val file = new File("file")

        val uploadedObject = Object(bucketName, key, "ETAG", new Date(), bucket.owner, 42, StorageClass.Standard)

        (client.upload(_: String, _: String, _: File)(_: UploadSource[File], _:Materializer))
          .expects(bucketName, key, file, UploadSource.fileSource, materialiser)
          .returning(Future.successful(toAws(uploadedObject)))

        bucket.upload(key, file).futureValue shouldBe uploadedObject
      }
    }
  }

  private def toAws(obj: Object): aws.S3ObjectSummary = {
    val summary = new S3ObjectSummary
    summary.setBucketName(obj.bucketName)
    summary.setKey(obj.key)
    summary.setETag(obj.eTag)
    summary.setLastModified(obj.lastModified)
    summary.setOwner(new aws.Owner(obj.owner.id, obj.owner.displayName))
    summary.setSize(obj.size)
    summary.setStorageClass(obj.storageClass.toAws.toString)
    summary
  }
}
