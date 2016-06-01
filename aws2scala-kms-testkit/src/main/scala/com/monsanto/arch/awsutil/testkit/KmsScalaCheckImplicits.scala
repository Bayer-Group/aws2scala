package com.monsanto.arch.awsutil.testkit

import com.monsanto.arch.awsutil.kms.model._
import org.scalacheck.{Arbitrary, Gen}

object KmsScalaCheckImplicits {
  implicit lazy val arbKeyState: Arbitrary[KeyState] = Arbitrary(Gen.oneOf(KeyState.values))

  implicit lazy val arbKeyUsage: Arbitrary[KeyUsage] = Arbitrary(Gen.oneOf(KeyUsage.values))
}
