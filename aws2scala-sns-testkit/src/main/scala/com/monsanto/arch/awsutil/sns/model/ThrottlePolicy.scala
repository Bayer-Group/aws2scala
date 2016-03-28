package com.monsanto.arch.awsutil.sns.model

import org.scalacheck.{Arbitrary, Gen, Shrink}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

case class ThrottlePolicy(maxReceivesPerSecond: Int)

object ThrottlePolicy {
  implicit lazy val arbThrottlePolicy: Arbitrary[ThrottlePolicy] =
    Arbitrary(Gen.posNum[Int].map(ThrottlePolicy.apply))

  implicit lazy val shrinkThrottlePolicy: Shrink[ThrottlePolicy] =
    Shrink { policy ⇒
      Shrink.shrink(policy.maxReceivesPerSecond)
        .filter(_ > 0)
        .map(ThrottlePolicy.apply)
    }

  implicit lazy val jsonFormat: RootJsonFormat[ThrottlePolicy] = jsonFormat1(ThrottlePolicy.apply)
}
