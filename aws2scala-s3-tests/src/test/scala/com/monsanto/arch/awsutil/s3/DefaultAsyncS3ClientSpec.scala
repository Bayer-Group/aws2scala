package com.monsanto.arch.awsutil.s3

import akka.stream.scaladsl.Source
import com.monsanto.arch.awsutil.s3.model.{Bucket, CreateBucketRequest}
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.{FlowMockUtils, Materialised}
import com.monsanto.arch.awsutil.testkit.S3Gen
import com.monsanto.arch.awsutil.testkit.S3ScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DefaultAsyncS3ClientSpec extends FreeSpec with MockFactory with Materialised with FlowMockUtils {
  "the async S3 client can" - {
    "create buckets" in {
      forAll(S3Gen.bucketName, arbitrary[Bucket]) { (bucketName, bucket) ⇒
        val streaming = mock[StreamingS3Client]("streaming")
        val async = new DefaultAsyncS3Client(streaming)

        (streaming.bucketCreator _)
          .expects()
          .returningFlow(CreateBucketRequest(bucketName), bucket)

        val result = async.createBucket(bucketName).futureValue
        result shouldBe bucket
      }
    }

    "check if buckets exist" in {
      forAll(S3Gen.bucketName, arbitrary[Boolean]) { (bucketName, exists) ⇒
        val streaming = mock[StreamingS3Client]("streaming")
        val async = new DefaultAsyncS3Client(streaming)

        (streaming.bucketExistenceChecker _)
          .expects()
          .returningFlow(bucketName, exists)

        val result = async.doesBucketExist(bucketName).futureValue
        result shouldBe exists
      }
    }
  }

  "list buckets" in {
    forAll(maxSize(25)) { buckets: List[Bucket] ⇒
      val streaming = mock[StreamingS3Client]("streaming")
      val async = new DefaultAsyncS3Client(streaming)

      (streaming.bucketLister _)
        .expects()
        .returning(Source(buckets))

      val result = async.listBuckets().futureValue
      result shouldBe buckets
    }
  }
}
