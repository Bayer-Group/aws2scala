package com.monsanto.arch.awsutil.s3.model

import com.monsanto.arch.awsutil.s3.model.AwsConverters._
import com.monsanto.arch.awsutil.testkit.S3Gen
import com.monsanto.arch.awsutil.testkit.S3ScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class CreateBucketRequestSpec extends FreeSpec {
  "a CreateBucketRequest" - {
    "can be created" - {
      "with a bucket name" in {
        forAll(S3Gen.bucketName) { name ⇒
          CreateBucketRequest(name) shouldBe CreateBucketRequest.CreateBucketWithNoAcl(name, None)
        }
      }

      "with a bucket name and region" in {
        forAll(S3Gen.bucketName, arbitrary[Region]) { (name, region) ⇒
          CreateBucketRequest(name, region) shouldBe CreateBucketRequest.CreateBucketWithNoAcl(name, Some(region))
        }
      }
    }

    "converts to the correct AWS object" in {
      forAll { request: CreateBucketRequest ⇒
        request.asAws should have (
          'AccessControlList (null),
          'BucketName (request.bucketName),
          'CannedAcl (null),
          'Region (request.region.orNull)
        )
      }
    }
  }
}
