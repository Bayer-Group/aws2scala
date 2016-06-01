package com.monsanto.arch.awsutil.testkit

import java.util.Date

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.kms.model._
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen, Shrink}

object KmsScalaCheckImplicits {
  implicit lazy val arbKeyArn: Arbitrary[KeyArn] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        region ← CoreGen.regionFor(account)
        id ← Gen.uuid.map(_.toString)
      } yield KeyArn(account, region, id)
    }

  implicit lazy val arbKeyMetadata: Arbitrary[KeyMetadata] =
    Arbitrary {
      for {
        arn ← arbitrary[KeyArn]
        creationDate ← arbitrary[Date]
        enabled ← arbitrary[Boolean]
        description ← arbitrary[Option[String]]
        keyUsage ← arbitrary[KeyUsage]
        keyState ← arbitrary[KeyState]
        deletionDate ← arbitrary[Option[Date]]
      } yield KeyMetadata(arn.account, arn.id, arn, creationDate, enabled, description, keyUsage, keyState, deletionDate)
    }

  implicit lazy val shrinkKeyMetadata: Shrink[KeyMetadata] =
    Shrink { metadata ⇒
      Shrink.shrink(metadata.arn).map(x ⇒ metadata.copy(arn = x, account = x.account, id = x.id)) append
        Shrink.shrink(metadata.creationDate).map(x ⇒ metadata.copy(creationDate = x)) append
        Shrink.shrink(metadata.enabled).map(x ⇒ metadata.copy(enabled = x)) append
        Shrink.shrink(metadata.description).map(x ⇒ metadata.copy(description = x)) append
        Shrink.shrink(metadata.usage).map(x ⇒ metadata.copy(usage = x)) append
        Shrink.shrink(metadata.state).map(x ⇒ metadata.copy(state = x)) append
        Shrink.shrink(metadata.deletionDate).map(x ⇒ metadata.copy(deletionDate = x))
    }

  implicit lazy val arbKeyState: Arbitrary[KeyState] = Arbitrary(Gen.oneOf(KeyState.values))

  implicit lazy val arbKeyUsage: Arbitrary[KeyUsage] = Arbitrary(Gen.oneOf(KeyUsage.values))
}
