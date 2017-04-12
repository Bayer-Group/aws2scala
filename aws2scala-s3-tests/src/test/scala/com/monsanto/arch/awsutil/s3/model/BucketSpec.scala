package com.monsanto.arch.awsutil.s3.model

import java.io.File
import java.util.Date

import akka.stream.Materializer
import com.amazonaws.services.s3.model.S3ObjectSummary
import com.amazonaws.services.s3.{model ⇒ aws}
import com.monsanto.arch.awsutil.s3.{AsyncS3Client, UploadSource}
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Materialised
import com.monsanto.arch.awsutil.testkit.UtilGen
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

import scala.concurrent.Future
import scala.util.Try

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

  "Bucket.validName" - {
    val lowerDigitChar = Gen.oneOf(('a' to 'z') ++ ('0' to '9'))
    def positive(n: Int): Int = Math.max(n,1)
    def nameContaining(sub: String): Gen[String] = {
      val subLen = sub.length
      val baseMin = (3 - subLen).max(1)
      val baseMax = 63 - subLen
      val gen =
        for {
          baseName ← UtilGen.stringOf(lowerDigitChar, baseMin, baseMax)
          index ← Gen.choose(1, positive(baseName.length - 1))
        } yield {
          val (start, end) = baseName.splitAt(index)
          s"$start$sub$end"
        }
      gen.suchThat { name ⇒
        val subIndex = name.indexOf(sub)
        name.length > 3 && subIndex != 0 && subIndex != (name.length - subLen)
      }
    }
    def nameStartingWith(prefix: String): Gen[String] = {
      val prefixLength = prefix.length
      val baseMin = (3 - prefixLength).max(1)
      val baseMax = 63 - prefixLength
      val gen = UtilGen.stringOf(lowerDigitChar, baseMin, baseMax).map(prefix + _)
      gen.suchThat { name ⇒
        name.length > 3 && name.startsWith(prefix)
      }
    }
    def nameEndingWith(suffix: String): Gen[String] = {
      val suffixLength = suffix.length
      val baseMin = (3 - suffixLength).max(1)
      val baseMax = 63 - suffixLength
      val gen = UtilGen.stringOf(lowerDigitChar, baseMin, baseMax).map(_ + suffix)
      gen.suchThat { name ⇒
        name.length > 3 && name.endsWith(suffix)
      }
    }


    "accepts" - {
      "simple names" in {
        forAll(UtilGen.stringOf(lowerDigitChar, 3, 63)) { name ⇒
          Bucket.validName(name) shouldBe true
        }
      }

      "names with hyphens" in {
        forAll(nameContaining("-")) { name ⇒
          Bucket.validName(name) shouldBe true
        }
      }

      "names with full stops" in {
        forAll(nameContaining(".")) { name ⇒
          Bucket.validName(name) shouldBe true
        }
      }
    }

    "rejects" - {
      "short names" in {
        val shortName = UtilGen.stringOf(lowerDigitChar, 0, 2)
        forAll(shortName) { name ⇒
          Bucket.validName(name) shouldBe false
        }
      }

      "long names" in {
        val longName = UtilGen.stringOf(lowerDigitChar, 64, 1024)
        forAll(longName) { name ⇒
          Bucket.validName(name) shouldBe false
        }
      }

      "names with upper-case characters" in {
        val upperCharName = UtilGen.stringOf(lowerDigitChar, 3, 63).map(_.toUpperCase).suchThat(_.exists(_.isUpper))
        forAll(upperCharName) { name ⇒
          Bucket.validName(name) shouldBe false
        }
      }

      "names with invalid punctuation" in {
        val badChars = Seq('_', ',', '\'', '"', '$', '%', '!', '?')
        val badCharName =
          for {
            badChar ← Gen.oneOf(badChars).map(_.toString)
            name ← Gen.oneOf(
              nameContaining(badChar),
              nameStartingWith(badChar),
              nameEndingWith(badChar)
            )
          } yield name
        forAll(badCharName) { name ⇒
          Bucket.validName(name) shouldBe false
        }
      }

      "names that start with a full stop" in {
        forAll(nameStartingWith(".")) { name ⇒
          Bucket.validName(name) shouldBe false
        }
      }

      "names that end with a full stop" in {
        forAll(nameEndingWith(".")) { name ⇒
          Bucket.validName(name) shouldBe false
        }
      }

      "names containing two full stops in a row" in {
        forAll(nameContaining("..")) { name ⇒
          Bucket.validName(name) shouldBe false
        }
      }

      "names that begin with a hyphen" in {
        forAll(nameStartingWith("-")) { name ⇒
          Bucket.validName(name) shouldBe false
        }
      }

      "names that end with a hyphen" in {
        forAll(nameEndingWith("-")) { name ⇒
          Bucket.validName(name) shouldBe false
        }
      }

      "labels that begin with a hyphen" in {
        forAll(nameContaining(".-")) { name ⇒
          Bucket.validName(name) shouldBe false
        }
      }

      "labels that end with a hyphen" in {
        forAll(nameEndingWith("-.")) { name ⇒
          Bucket.validName(name) shouldBe false
        }
      }

      "names that look like IP addresses" in {
        val ipName =
          Gen.listOfN(4, Gen.choose(0, 255)).map(_.mkString("."))
            .suchThat { ip ⇒
              Try {
                val octets = ip.split('.').map(_.toInt)
                octets.length == 4 && octets.forall(b ⇒ b >= 0 && b < 256)
              }.getOrElse(false)
            }
        forAll(ipName) { name ⇒
          Bucket.validName(name) shouldBe false
        }
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
