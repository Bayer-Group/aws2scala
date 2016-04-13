package com.monsanto.arch.awsutil.s3

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.{AmazonS3, model ⇒ aws}
import com.monsanto.arch.awsutil.AwsSettings
import com.monsanto.arch.awsutil.s3.model.AwsConverters._
import com.monsanto.arch.awsutil.s3.model.{Bucket, CreateBucketRequest}
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Materialised
import com.monsanto.arch.awsutil.testkit.S3Gen
import com.monsanto.arch.awsutil.testkit.S3ScalaCheckImplicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalacheck.Arbitrary.arbitrary

import scala.collection.JavaConverters._

class DefaultStreamingS3ClientSpec extends FreeSpec with MockFactory with Materialised {
  "the default streaming S3 client can" - {
    "create buckets" in {
      forAll { (request: CreateBucketRequest, bucket: Bucket) ⇒
        withFixture { f ⇒
          (f.s3.createBucket(_: aws.CreateBucketRequest))
            .expects(where { r: aws.CreateBucketRequest ⇒
              val (maybeCannedAcl, maybeAccessControlList) = request.acl match {
                case None                  ⇒ (None, None)
                case Some(Left(cannedAcl)) ⇒ (Some(cannedAcl.toAws), None)
                case Some(Right(grants))   ⇒ (None, Some(grants.asAws))
              }
              r should have (
                'AccessControlList (maybeAccessControlList.orNull),
                'BucketName (request.bucketName),
                'CannedAcl (maybeCannedAcl.orNull),
                'Region (request.region.map(_.toString).orNull)
              )
              true
            })
            .returning(bucket.asAws)
          (f.s3.listBuckets(_: aws.ListBucketsRequest))
            .expects(*)
            .returning(Seq(bucket.asAws).asJava)
          val result = Source.single(request)
            .via(f.streamingClient.bucketCreator)
            .runWith(Sink.head)
            .futureValue

          result shouldBe bucket
        }
      }
    }

    "check if a bucket exists" in {
      forAll(S3Gen.bucketName, arbitrary[Boolean]) { (bucketName, exists) ⇒
        withFixture { f ⇒
          (f.s3.doesBucketExist _)
            .expects(bucketName)
            .returning(exists)
          val result = Source.single(bucketName)
            .via(f.streamingClient.bucketExistenceChecker)
            .runWith(Sink.head)
            .futureValue

          result shouldBe exists
        }
      }
    }

    "list buckets" in {
      forAll(maxSize(25)) { buckets: Seq[Bucket] ⇒
        withFixture { f ⇒
          (f.s3.listBuckets(_: aws.ListBucketsRequest))
            .expects(*)
            .returning(buckets.map(_.asAws).asJava)
          val result =
            f.streamingClient.bucketLister
              .runWith(Sink.seq)
              .futureValue

          result shouldBe buckets
        }
      }
    }
  }

  private case class Fixture(s3: AmazonS3, transferManager: TransferManager, streamingClient: StreamingS3Client)

  private def withFixture(test: Fixture ⇒ Any): Unit = {
    val s3 = mock[AmazonS3]("s3")
    val transferManager = mock[TransferManager]("transferManager")
    val streamingS3Client = new DefaultStreamingS3Client(s3, transferManager, AwsSettings.Default)(materialiser.executionContext)
    val fixture = Fixture(s3, transferManager, streamingS3Client)
    test(fixture)
  }
}
