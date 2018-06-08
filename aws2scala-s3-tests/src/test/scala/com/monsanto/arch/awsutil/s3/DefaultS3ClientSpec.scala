package com.monsanto.arch.awsutil.s3

import java.io.{ByteArrayInputStream, File}
import java.net.URL
import java.util.concurrent.TimeoutException
import java.util.{Date, UUID}

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.s3.model. _
import com.amazonaws.services.s3.transfer.model.UploadResult
import com.amazonaws.services.s3.transfer.{Download, TransferManager, Upload}
import com.amazonaws.services.s3.{AbstractAmazonS3, Headers}
import com.monsanto.arch.awsutil.AwsSettings
import com.monsanto.arch.awsutil.s3.DefaultS3ClientSpec._
import com.monsanto.arch.awsutil.s3.model.BucketNameAndKey
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Materialised
import com.typesafe.config.ConfigFactory
import org.scalacheck.Gen
import org.scalactic.source.Position
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalatest.{Assertion, FreeSpec}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class DefaultS3ClientSpec extends FreeSpec with Materialised with MockFactory {
  private val userName = System.getProperty("user.name")

  "the S3 client" - {
    "using the default settings" - {
      behave like s3behaviours(DefaultSettings)
    }
    "using the alternate settings" - {
      behave like s3behaviours(AltSettings)
    }
  }

  def s3behaviours(settings: AwsSettings): Unit = {
    val bucketName = s"aws2scala-s3-test-common-${UUID.randomUUID()}"
    val sseValue = settings.s3.defaultPutObjectHeaders.get(Headers.SERVER_SIDE_ENCRYPTION)
    sseValue shouldBe settings.s3.defaultCopyObjectHeaders.get(Headers.SERVER_SIDE_ENCRYPTION)

    "can delete a bucket" in withFixture(settings) { f =>
      (f.s3.deleteBucket(_: String)).expects(bucketName)
      val result = f.asyncClient.deleteBucket(bucketName).futureValue
      result shouldBe bucketName
    }

    "can get a bucket policy when" - {
      "one is available" in withFixture(settings) { f =>
        val policyText = "{}"
        val bucketPolicy = {
          val p = new BucketPolicy
          p.setPolicyText(policyText)
          p
        }
        (f.s3.getBucketPolicy(_: String)).expects(bucketName).returning(bucketPolicy)
        val result = f.asyncClient.getBucketPolicy(bucketName).futureValue
        result shouldBe Some(policyText)
      }

      "there is no policy" in withFixture(settings) { f =>
        (f.s3.getBucketPolicy(_: String)).expects(bucketName).returning(new BucketPolicy)
        val result = f.asyncClient.getBucketPolicy(bucketName).futureValue
        result shouldBe None
      }
    }

    "can set a bucket policy" - {
      val policyText = genBucketPolicy(bucketName)

      "using a plain string" in withFixture(settings) { f =>
        (f.s3.setBucketPolicy(_: String, _: String)).expects(bucketName, policyText)
        val result = f.asyncClient.setBucketPolicy(bucketName, policyText).futureValue
        result shouldBe bucketName
      }

      "using a string in an option" in withFixture(settings) { f =>
        (f.s3.setBucketPolicy(_: String, _: String)).expects(bucketName, policyText)
        val result = f.asyncClient.setBucketPolicy(bucketName, Some(policyText)).futureValue
        result shouldBe bucketName
      }
    }

    "can delete a bucket policy" - {
      "using the delete method" in withFixture(settings) { f =>
        (f.s3.deleteBucketPolicy(_: String)).expects(bucketName)
        val result = f.asyncClient.deleteBucketPolicy(bucketName).futureValue
        result shouldBe bucketName
      }

      "using an empty option" in withFixture(settings) { f =>
        (f.s3.deleteBucketPolicy(_: String)).expects(bucketName)
        val result = f.asyncClient.setBucketPolicy(bucketName, None).futureValue
        result shouldBe bucketName
      }
    }

    "can get a bucket tagging configuration" - {
      "when there are no tags" in withFixture(settings) { f ⇒
        (f.s3.getBucketTaggingConfiguration(_: String)).expects(bucketName).returning(null)

        val result = f.asyncClient.getBucketTags(bucketName).futureValue
        result shouldBe empty
      }

      "when there are tags" in withFixture(settings) { f ⇒
        val tags = Map("mon:owner" → "me", "mon:application" → "aws2scala")
        (f.s3.getBucketTaggingConfiguration(_: String))
          .expects(bucketName)
          .returning(new BucketTaggingConfiguration(Seq(new TagSet(tags.asJava)).asJavaCollection))

        val result = f.asyncClient.getBucketTags(bucketName).futureValue
        result shouldBe tags
      }
    }

    "can set a bucket tagging configuration" in withFixture(settings) { f ⇒
      val tags = Map("mon:owner" → "me", "mon:application" → "aws2scala")

      (f.s3.setBucketTaggingConfiguration(_: String, _: BucketTaggingConfiguration))
          .expects(where { (name: String, config: BucketTaggingConfiguration) ⇒
            name == bucketName && config.getTagSet.getAllTags.asScala == tags
          })

      val result = f.asyncClient.setBucketTags(bucketName, tags).futureValue
      result shouldBe bucketName
    }

    "can remove a bucket tagging configuration" - {
      "using an empty set of tags" in withFixture(settings) { f ⇒
        (f.s3.deleteBucketTaggingConfiguration(_: String)).expects(bucketName)

        val result = f.asyncClient.setBucketTags(bucketName, Map.empty[String,String]).futureValue
        result shouldBe bucketName
      }

      "using the delete method" in withFixture(settings) { f ⇒
        (f.s3.deleteBucketTaggingConfiguration(_: String)).expects(bucketName)

        val result = f.asyncClient.deleteBucketTags(bucketName).futureValue
        result shouldBe bucketName
      }
    }

    "provides an upload object flow that" - {
      "works" in withFixture(settings) { f ⇒
        val key = "bar"
        val summary = generateSummary(bucketName, key, 2048)
        val request = new PutObjectRequest("foo", key, "http://example.com/")
        mockUpload(f, bucketName, key, _ == request, summary)
        val result = Source.single(request)
          .via(f.streamingClient.rawUploader)
          .runWith(Sink.head).futureValue

        result shouldBe summary
      }

      "propagates errors from the upload" in withFixture(settings) { f ⇒
        val key = "bar"
        val request = new PutObjectRequest("foo", key, "http://example.com/")
        val failure = new AmazonS3Exception("Sharks with lasers!")

        val upload = {
          val upload = mock[Upload]
          (upload.waitForUploadResult _).expects().onCall(() ⇒ throw failure)
          upload
        }

        (f.transferManager.upload(_: PutObjectRequest)).expects(request).returning(upload)

        val result = Source.single(request)
          .via(f.streamingClient.rawUploader)
          .runWith(Sink.head)

        Await.ready(result, 1.second)

        result.eitherValue should matchPattern { case Some(Left(ex)) if ex == failure ⇒ }
      }

      "propagates timeouts from the upload check" in withFixture(settings) { f ⇒
        val key = "bar"
        val request = new PutObjectRequest("foo", key, "http://example.com/")
        val count = Stream.from(1)

        val upload = {
          val upload = mock[Upload]("upload")
          (upload.waitForUploadResult _).expects().returning {
            val result = mock[UploadResult]("uploadResult")
            (result.getBucketName _).expects().returning(bucketName).anyNumberOfTimes()
            (result.getKey _).expects().returning(key).anyNumberOfTimes()
            result
          }
          upload
        }

        (f.transferManager.upload(_: PutObjectRequest)).expects(request).returning(upload)
        (f.s3.listObjects(_: ListObjectsRequest))
          .expects(where { r: ListObjectsRequest ⇒
            r.getBucketName == bucketName && r.getPrefix == key
          })
          .onCall { _: ListObjectsRequest ⇒
            val listing = mock[ObjectListing](s"listing-${count.head}")
            (listing.getObjectSummaries _).expects().returning(List.empty[S3ObjectSummary].asJava).noMoreThanOnce()
            (listing.getNextMarker _).expects().returning(null).noMoreThanOnce()
            listing
          }
          .anyNumberOfTimes()

        val result = Source.single(request)
          .via(f.streamingClient.rawUploader)
          .runWith(Sink.head)

        Await.ready(result, 1.second)

        result.eitherValue should matchPattern { case Some(Left(_: TimeoutException)) ⇒ }
      }
    }

    "can upload to a bucket" - {
      "an array of bytes" in withFixture(settings) { f ⇒
        val key = "bytes.data"
        val content = Array[Byte](0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val summary = generateSummary(bucketName, key, content.length)

        val requestMatcher = { r: PutObjectRequest ⇒
          r.getBucketName == bucketName &&
            r.getKey == key &&
            Option(r.getInputStream).isDefined &&
            Option(r.getFile).isEmpty &&
            r.getMetadata.getContentLength == content.length &&
            Option(r.getMetadata.getSSEAlgorithm) == sseValue
        }
        mockUpload(f, bucketName, key, requestMatcher, summary)

        val result = f.asyncClient.upload(bucketName, key, content).futureValue
        result shouldBe summary
      }

      "a string" in withFixture(settings) { f ⇒
        val key = "string.txt"
        val content = "some data"
        val summary = generateSummary(bucketName, key, content.getBytes.length)

        val requestMatcher = { r: PutObjectRequest ⇒
          r.getBucketName == bucketName &&
            r.getKey == key &&
            Option(r.getInputStream).isDefined &&
            Option(r.getFile).isEmpty &&
            r.getMetadata.getContentLength == content.getBytes.length &&
            Option(r.getMetadata.getSSEAlgorithm) == sseValue
        }
        mockUpload(f, bucketName, key, requestMatcher, summary)

        val result = f.asyncClient.upload(bucketName, key, content).futureValue
        result shouldBe summary
      }

      "a file" in withFixture(settings) { f ⇒
        val key = "file.txt"
        val file = new File("file.txt")
        val summary = generateSummary(bucketName, key, 42)

        val requestMatcher = { r: PutObjectRequest ⇒
          r.getBucketName == bucketName &&
            r.getKey == key &&
            Option(r.getInputStream).isEmpty &&
            Option(r.getFile).contains(file) &&
            Option(r.getMetadata.getSSEAlgorithm) == sseValue
        }

        mockUpload(f, bucketName, key, requestMatcher, summary)

        val result = f.asyncClient.upload(bucketName, key, file).futureValue
        result shouldBe summary
      }
    }

    "can download from a bucket" - {
      "an array of bytes" in withFixture(settings) { f ⇒
        val key = "bytes.data"
        val content = Array[Byte](0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        (f.s3.getObject(_: GetObjectRequest))
          .expects(where { request: GetObjectRequest ⇒ request.getBucketName == bucketName && request.getKey == key })
          .returning {
            val obj = new S3Object
            obj.setObjectContent(new ByteArrayInputStream(content))
            obj
          }

        val result = f.asyncClient.download[Array[Byte]](bucketName, key).futureValue
        result shouldBe content
      }

      "a string" in withFixture(settings) { f ⇒
        val key = "string.txt"
        val content = "a string"

        (f.s3.getObject(_: GetObjectRequest))
          .expects(where { request: GetObjectRequest ⇒ request.getBucketName == bucketName && request.getKey == key })
          .returning {
            val obj = new S3Object
            obj.setObjectContent(new ByteArrayInputStream(content.getBytes))
            obj
          }

        val result = f.asyncClient.download[String](bucketName, key).futureValue
        result shouldBe content
      }

      "to a file" in withFixture(settings) { f ⇒
        val key = "file.txt"
        val file = new File("file.txt")

        (f.transferManager.download(_: GetObjectRequest, _: File))
          .expects(where { (request: GetObjectRequest, dest: File) ⇒
            request.getBucketName == bucketName &&
              request.getKey == key &&
              dest == file
          })
          .returning {
            val dl = mock[Download]("download")
            (dl.waitForCompletion _).expects()
            dl
          }

        val result = f.asyncClient.downloadTo(bucketName, key, file).futureValue
        result shouldBe file
      }
    }

    "can copy objects" in withFixture(settings) { f ⇒
      val sourceKey = "source.txt"
      val destKey = "dest.txt"
      val summary = generateSummary(bucketName, destKey, 42)

      (f.s3.copyObject(_: CopyObjectRequest))
        .expects(where { r: CopyObjectRequest ⇒
          r.getSourceBucketName == bucketName &&
            r.getSourceKey == sourceKey &&
            r.getDestinationBucketName == bucketName &&
            r.getDestinationKey == destKey &&
            Option(r.getNewObjectMetadata.getSSEAlgorithm) == sseValue
        })
        .returning(new CopyObjectResult)
      (f.s3.listObjects(_: ListObjectsRequest))
        .expects(where { r: ListObjectsRequest ⇒
          r.getBucketName == bucketName && r.getPrefix == destKey
        })
        .returning {
          val listing = mock[ObjectListing]("listing")
          (listing.getObjectSummaries _).expects().returning(List(summary).asJava)
          (listing.getNextMarker _).expects().returning(null)
          listing
        }

      val result = f.asyncClient.copy(bucketName, sourceKey, destKey).futureValue
      result shouldBe summary
    }

    "can list objects in a bucket" - {
      "without a prefix and paging" in withFixture(settings) { f ⇒
        val marker = "marker"
        val summaries = Seq.tabulate(20) { i ⇒ mock[S3ObjectSummary](s"object$i") }
        (f.s3.listObjects(_: ListObjectsRequest))
          .expects(where { request: ListObjectsRequest ⇒
            request.getBucketName == bucketName &&
              request.getPrefix == null &&
              (request.getMarker == null || request.getMarker == marker)
          })
          .onCall { request: ListObjectsRequest ⇒
            val firstRequest = request.getMarker == null
            val chunk = if (firstRequest) summaries.take(10) else summaries.drop(10)
            val nextMarker = if (firstRequest) marker else null
            val listing = mock[ObjectListing](s"listing${if (firstRequest) 0 else 1}")
            (listing.getObjectSummaries _).expects().returning(chunk.asJava)
            (listing.getNextMarker _).expects().returning(nextMarker)
            listing
          }
          .twice()

        val result = f.asyncClient.listObjects(bucketName).futureValue
        result shouldBe summaries
      }

      "with a prefix" in withFixture(settings) { f ⇒
        val summaries = Seq.tabulate(10) { i ⇒ mock[S3ObjectSummary](s"object$i") }
        val prefix = "foo"
        (f.s3.listObjects(_: ListObjectsRequest))
          .expects(where { request: ListObjectsRequest ⇒
            request.getBucketName == bucketName &&
              request.getPrefix == prefix &&
              request.getMarker == null
          })
          .returning {
            val listing = mock[ObjectListing]("listing")
            (listing.getObjectSummaries _).expects().returning(summaries.asJava)
            (listing.getNextMarker _).expects().returning(null)
            listing
          }

        val result = f.asyncClient.listObjects(bucketName, prefix).futureValue
        result shouldBe summaries
      }
    }

    "can get a URL for an object" in withFixture(settings) { f ⇒
      val key = "foo.txt"
      val url = new URL(s"https://$bucketName.${settings.region.getServiceEndpoint("s3")}/$key")

      val result = f.asyncClient.getUrl(bucketName, key).futureValue
      result shouldBe url
    }

    "can empty a bucket" in withFixture(settings) { f =>
      val summaries = Seq.tabulate(10) { i ⇒ val s = new S3ObjectSummary()
        val key = i+"key"
        s.setBucketName(bucketName)
        s.setKey(key)
        (f.s3.deleteObject(_:DeleteObjectRequest))
          .expects(where{r:DeleteObjectRequest => r.getBucketName== bucketName && r.getKey == key})
        s
      }
      (f.s3.listObjects(_: ListObjectsRequest))
        .expects(*)
        .onCall { request: ListObjectsRequest ⇒
          val listing = mock[ObjectListing]("listing")
          if (request.getPrefix == null) {
            (listing.getObjectSummaries _).expects().returning(summaries.asJava)
          } else {
            (listing.getObjectSummaries _).expects().returning(Seq.empty[S3ObjectSummary].asJava)
          }
          (listing.getNextMarker _).expects().returning(null)
          listing
        }
        .anyNumberOfTimes()

      val result = f.asyncClient.emptyBucket(bucketName).futureValue(PatienceConfig(1.second, 100.milliseconds), Position.here)
      result shouldBe bucketName
    }

    "can empty and delete a bucket" in {
      val keyGenerator = Gen.choose(0, 50).map(n ⇒ List.tabulate(n)(i ⇒ s"object$i"))
      forAll(keyGenerator → "keys") { (keys: List[String]) ⇒
        withFixture(settings) { f ⇒
          val listings = if (keys.isEmpty) List(keys) else keys.grouped(5).toList
          listings.zipWithIndex.foreach { case (listing, i) ⇒
            val isFirst = i == 0
            val isLast = i == (listings.size - 1)
            (f.s3.listObjects(_: ListObjectsRequest))
              .expects(where { (request: ListObjectsRequest) ⇒
                request.getBucketName == bucketName && (
                  Option(request.getMarker) match {
                    case None ⇒ isFirst
                    case Some(m) ⇒ i.toString == m
                  }
                )
              })
              .returning {
                new ObjectListing {
                  override def getBucketName = bucketName
                  override def getNextMarker = if (isLast) null else (i + 1).toString
                  override def getObjectSummaries = {
                    listing.map { key ⇒
                      val summary = new S3ObjectSummary
                      summary.setBucketName(bucketName)
                      summary.setKey(key)
                      summary
                    }.asJava
                  }
                }
              }
          }
          keys.foreach { key ⇒
            (f.s3.deleteObject(_: DeleteObjectRequest))
              .expects(where { (request: DeleteObjectRequest) ⇒
                request.getBucketName == bucketName && request.getKey == key
              })
          }
          (f.s3.deleteBucket(_: String)).expects(bucketName)

          val result = f.asyncClient.emptyAndDeleteBucket(bucketName).futureValue
          result shouldBe bucketName
        }
      }
    }

    "can delete and empty a stream buckets" in withFixture(settings) { f =>
      val bucketAndKeyGenerator = for {
        numBuckets ← Gen.choose(0, 10)
        keysPerBucket ← Gen.listOfN(numBuckets, Gen.choose(0, 50))
      } yield {
        0.until(numBuckets).map { bucketNum ⇒
          s"bucket$bucketNum" → List.tabulate(keysPerBucket(bucketNum))(i ⇒ s"object$i")
        }.toMap
      }
      forAll(bucketAndKeyGenerator → "bucketsAndKeys") { (bucketAndKeys: Map[String,List[String]]) ⇒
        bucketAndKeys.foreach { case (bucketName, keys) ⇒
          (f.s3.listObjects(_: ListObjectsRequest))
            .expects(where { (request: ListObjectsRequest) ⇒
              request.getBucketName == bucketName &&
                request.getMarker == null
            })
            .returning {
              new ObjectListing {
                override def getBucketName = bucketName
                override def getObjectSummaries = {
                  keys.map { key ⇒
                    val summary = new S3ObjectSummary
                    summary.setBucketName(bucketName)
                    summary.setKey(key)
                    summary
                  }.asJava
                }
              }
            }
          keys.foreach { key ⇒
            (f.s3.deleteObject(_: DeleteObjectRequest))
              .expects(where { (request: DeleteObjectRequest) ⇒
                request.getBucketName == bucketName && request.getKey == key
              })
          }
          (f.s3.deleteBucket(_: String)).expects(bucketName)
        }

        val bucketNames = bucketAndKeys.keys.toList
        val result =
          Source(bucketNames)
            .via(f.streamingClient.bucketEmptierAndDeleter)
            .runWith(Sink.seq)
            .futureValue

        result shouldBe bucketNames
      }
    }

    "can delete an object" in withFixture(settings) { f =>
      val bucketNameAndKey = BucketNameAndKey(bucketName, "key")
      (f.s3.deleteObject(_:DeleteObjectRequest))
        .expects(where{r: DeleteObjectRequest ⇒ r.getBucketName == bucketNameAndKey.bucketName &&
          r.getKey == bucketNameAndKey.key})
      (f.s3.listObjects(_: ListObjectsRequest))
        .expects(where{r: ListObjectsRequest ⇒
          r.getBucketName == bucketNameAndKey.bucketName && r.getPrefix == bucketNameAndKey.key})
        .onCall { _: ListObjectsRequest ⇒
          val listing = mock[ObjectListing]
          (listing.getObjectSummaries _).expects().returning(List.empty[S3ObjectSummary].asJava)
          (listing.getNextMarker _).expects()
          listing
        }
      val result = f.asyncClient.deleteObject(bucketNameAndKey.bucketName, bucketNameAndKey.key).futureValue
      result shouldBe bucketNameAndKey
    }

    def mockUpload(fixture: BasicFixture, bucketName: String, key: String, requestMatcher: PutObjectRequest ⇒ Boolean,
                   summary: S3ObjectSummary): Unit = {
      val upload = {
        val upload = mock[Upload]("upload")
        (upload.waitForUploadResult _).expects().returning {
          val result = mock[UploadResult]("result")
          (result.getBucketName _).expects().returning(bucketName).anyNumberOfTimes()
          (result.getKey _).expects().returning(key).anyNumberOfTimes()
          result
        }
        upload
      }

      (fixture.transferManager.upload(_: PutObjectRequest)).expects(where(requestMatcher)).returning(upload)
      (fixture.s3.listObjects(_: ListObjectsRequest))
        .expects(where { r: ListObjectsRequest ⇒
          r.getBucketName == bucketName && r.getPrefix == key
        })
        .returning {
          val listing = mock[ObjectListing]("listing")
          (listing.getObjectSummaries _).expects().returning(List(summary).asJava)
          (listing.getNextMarker _).expects().returning(null)
          listing
        }
    }
  }

  private def withFixture(settings: AwsSettings)(test: BasicFixture => Assertion): Assertion = {
    val s3 = mock[AbstractAmazonS3]("s3")
    val transferManager = mock[TransferManager]("transferManager")
    val streamingClient = new DefaultStreamingS3Client(s3, transferManager, settings)(materialiser.executionContext)
    val asyncClient = new DefaultAsyncS3Client(streamingClient)
    test(BasicFixture(s3, transferManager, settings, streamingClient, asyncClient))
  }

  private def mockBucket(name: String): Bucket = {
    val bucket = mock[Bucket]
    (bucket.getName _).expects().returning(name).anyNumberOfTimes()
    (bucket.getCreationDate _).expects().returning(new Date(System.currentTimeMillis())).anyNumberOfTimes()
    (bucket.getOwner _).expects().returning(new Owner(userName, userName)).anyNumberOfTimes()
    bucket
  }

  private def generateSummary(bucketName: String, key: String, size: Int): S3ObjectSummary = {
    val summary = new S3ObjectSummary
    summary.setBucketName(bucketName)
    summary.setETag("ETAG")
    summary.setKey(key)
    summary.setLastModified(new Date)
    summary.setOwner(new Owner(userName, userName))
    summary.setSize(size)
    summary.setStorageClass(StorageClass.Standard.toString)
    summary
  }

  case class BasicFixture(s3: AbstractAmazonS3,
                          transferManager: TransferManager,
                          settings: AwsSettings,
                          streamingClient: DefaultStreamingS3Client,
                          asyncClient: DefaultAsyncS3Client)
}

object DefaultS3ClientSpec {
  val TestUploadCheckConfig = ConfigFactory.parseString(
    """awsutil.s3 {
      |  upload-check-interval = 10ms
      |  upload-check-timeout = 500ms
      |}
    """.stripMargin)

  val DefaultSettings = new AwsSettings(TestUploadCheckConfig.withFallback(ConfigFactory.load()))
  val AltSettings = new AwsSettings(TestUploadCheckConfig.withFallback(ConfigFactory.load("s3-alt-settings")))

  def genBucketPolicy(bucketName: String): String =
    AltSettings.s3.defaultBucketPolicy.get.replace("@BUCKET_NAME@", bucketName)
}
