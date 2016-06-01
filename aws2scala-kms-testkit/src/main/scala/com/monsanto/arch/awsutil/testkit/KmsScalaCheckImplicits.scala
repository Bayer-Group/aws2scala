package com.monsanto.arch.awsutil.testkit

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.kms.model._
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

object KmsScalaCheckImplicits {
  implicit lazy val arbKeyArn: Arbitrary[KeyArn] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        region ← CoreGen.regionFor(account)
        id ← Gen.uuid.map(_.toString)
      } yield KeyArn(account, region, id)
    }

  implicit lazy val arbKeyState: Arbitrary[KeyState] = Arbitrary(Gen.oneOf(KeyState.values))

  implicit lazy val arbKeyUsage: Arbitrary[KeyUsage] = Arbitrary(Gen.oneOf(KeyUsage.values))
}
