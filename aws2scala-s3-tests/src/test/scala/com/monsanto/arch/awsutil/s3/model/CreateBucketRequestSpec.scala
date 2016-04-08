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

      "with a bucket name and canned ACL" in {
        forAll(S3Gen.bucketName, arbitrary[CannedAccessControlList]) { (name, cannedAcl) ⇒
          CreateBucketRequest(name, cannedAcl) shouldBe
            CreateBucketRequest.CreateBucketWithCannedAcl(name, cannedAcl, None)
        }
      }

      "with a bucket name, canned ACL, and region" in {
        forAll(S3Gen.bucketName, arbitrary[CannedAccessControlList], arbitrary[Region]) { (name, cannedAcl, region) ⇒
          CreateBucketRequest(name, cannedAcl, region) shouldBe
            CreateBucketRequest.CreateBucketWithCannedAcl(name, cannedAcl, Some(region))
        }
      }
    }

    "converts to the correct AWS object" in {
      forAll { request: CreateBucketRequest ⇒
        val cannedAcl = request match {
          case CreateBucketRequest.CreateBucketWithCannedAcl(_, acl, _) ⇒ Some(acl)
          case _ ⇒ None
        }
        request.asAws should have (
          'AccessControlList (null),
          'BucketName (request.bucketName),
          'CannedAcl (cannedAcl.map(_.asAws).orNull),
          'Region (request.region.map(_.toString).orNull)
        )
      }
    }
  }
}
