package com.monsanto.arch.awsutil.testkit

import com.monsanto.arch.awsutil.s3.model.Bucket
import org.scalacheck.Gen

object S3Gen {
  /** Generates an arbitrary bucket name. */
  val bucketName = UtilGen.stringOf(Gen.oneOf(('a' to 'z') ++ ('0' to '9') :+ '-' :+ '.'), 3, 63)
    .suchThat(Bucket.validName)
}
