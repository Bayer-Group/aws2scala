package com.monsanto.arch.awsutil.testkit

import com.monsanto.arch.awsutil.s3.model.{Bucket, CreateBucketRequest, Region}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen, Shrink}

object S3ScalaCheckImplicits {
  implicit lazy val arbRegion: Arbitrary[Region] = Arbitrary(Gen.oneOf(Region.values))

  implicit lazy val arbCreateBucketRequest: Arbitrary[CreateBucketRequest] =
    Arbitrary {
      for {
        name ← S3Gen.bucketName
        region ← arbitrary[Option[Region]]
      } yield CreateBucketRequest.CreateBucketWithNoAcl(name, region)
    }

  implicit lazy val shrinkCreateBucketRequest: Shrink[CreateBucketRequest] =
    Shrink {
      case r @ CreateBucketRequest.CreateBucketWithNoAcl(name, region) ⇒
        Shrink.shrink(name).filter(Bucket.validName).map(n ⇒ r.copy(bucketName = n)) append
          Shrink.shrink(region).map(x ⇒ r.copy(region = x))
    }
}
