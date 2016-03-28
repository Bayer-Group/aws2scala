package com.monsanto.arch.awsutil.sns.model

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Shrink}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

case class SubscriptionDeliveryPolicy(throttlePolicy: Option[ThrottlePolicy],
                                      healthyRetryPolicy: RetryPolicy)

object SubscriptionDeliveryPolicy {
  implicit lazy val arbSubscriptionDeliveryPolicy: Arbitrary[SubscriptionDeliveryPolicy] =
    Arbitrary {
      for {
        throttlePolicy ← arbitrary[Option[ThrottlePolicy]]
        healthyRetryPolicy ← arbitrary[RetryPolicy]
      } yield
        SubscriptionDeliveryPolicy(throttlePolicy, healthyRetryPolicy)
    }

  implicit lazy val shrinkSubscriptionDeliveryPolicy: Shrink[SubscriptionDeliveryPolicy] =
    Shrink.xmap((SubscriptionDeliveryPolicy.apply _).tupled, SubscriptionDeliveryPolicy.unapply(_).get)

  implicit lazy val jsonFormat: RootJsonFormat[SubscriptionDeliveryPolicy] = jsonFormat2(SubscriptionDeliveryPolicy.apply)
}
