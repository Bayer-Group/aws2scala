package com.monsanto.arch.awsutil.testkit

import com.monsanto.arch.awsutil.kms.model._
import org.scalacheck.{Arbitrary, Gen}

object KmsScalaCheckImplicits {
  implicit lazy val arbKeyUsage: Arbitrary[KeyUsage] = Arbitrary(Gen.oneOf(KeyUsage.values))
}
