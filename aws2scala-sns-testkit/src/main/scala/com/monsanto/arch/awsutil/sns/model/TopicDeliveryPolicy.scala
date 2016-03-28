package com.monsanto.arch.awsutil.sns.model

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Shrink}
import spray.json.DefaultJsonProtocol._
import spray.json._

case class TopicDeliveryPolicy(disableSubscriptionOverrides: Boolean,
                               defaultThrottlePolicy: Option[ThrottlePolicy],
                               defaultHealthyRetryPolicy: RetryPolicy) {
  override def toString = this.toJson.compactPrint
}

object TopicDeliveryPolicy {
  implicit lazy val arbTopicDeliveryPolicy: Arbitrary[TopicDeliveryPolicy] =
    Arbitrary {
      for {
        disableSubscriptionOverrides ← arbitrary[Boolean]
        maxReceivesPerSecond ← arbitrary[Option[ThrottlePolicy]]
        defaultHealthyRetryPolicy ← arbitrary[RetryPolicy]
      } yield
        TopicDeliveryPolicy(
          disableSubscriptionOverrides,
          maxReceivesPerSecond,
          defaultHealthyRetryPolicy)
    }

  implicit lazy val shrinkTopicDeliveryPolicy: Shrink[TopicDeliveryPolicy] =
    Shrink.xmap((TopicDeliveryPolicy.apply _).tupled, TopicDeliveryPolicy.unapply(_).get)

  implicit lazy val jsonFormat: RootJsonFormat[TopicDeliveryPolicy] = new RootJsonFormat[TopicDeliveryPolicy] {
    val deliveryPolicyFormat = jsonFormat3(TopicDeliveryPolicy.apply)

    override def read(json: JsValue): TopicDeliveryPolicy =
      json match {
        case JsObject(fields) if fields.keys == Set("http") ⇒ deliveryPolicyFormat.read(fields("http"))
        case x ⇒ deserializationError(s"Expected an object with the field ‘http’, but got $x")
      }

    override def write(obj: TopicDeliveryPolicy): JsValue = JsObject("http" → deliveryPolicyFormat.write(obj))
  }
}
