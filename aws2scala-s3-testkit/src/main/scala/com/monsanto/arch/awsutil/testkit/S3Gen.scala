package com.monsanto.arch.awsutil.testkit

import com.monsanto.arch.awsutil.s3.model.Bucket
import org.scalacheck.Gen

object S3Gen {
  /** Generates an arbitrary bucket name. */
  val bucketName: Gen[String] =
    UtilGen.stringOf(Gen.oneOf(('a' to 'z') ++ ('0' to '9') :+ '-' :+ '.'), 3, 63)
      .suchThat(Bucket.validName)

  /** Generates a canonical ID for an AWS account. */
  val canonicalIdentifier: Gen[String] =
    Gen.listOfN(64, UtilGen.lowerHexChar).map(_.mkString).suchThat(_.length == 64)
}
