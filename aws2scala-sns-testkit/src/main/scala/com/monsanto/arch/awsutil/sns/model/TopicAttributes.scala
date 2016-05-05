package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import org.scalacheck.{Arbitrary, Gen, Shrink}
import spray.json.{pimpAny, pimpString}

case class TopicAttributes(arn: TopicArn,
                           displayName: String,
                           policy: Policy,
                           subscriptionsPending: Int,
                           subscriptionsConfirmed: Int,
                           subscriptionsDeleted: Int,
                           deliveryPolicy: Option[TopicDeliveryPolicy],
                           effectiveDeliveryPolicy: TopicDeliveryPolicy) {
  def asMap: Map[String,String] =
    Map(
      "TopicArn" → arn.value,
      "DisplayName" → displayName,
      "Owner" → arn.owner.id,
      "Policy" → policy.toString,
      "SubscriptionsPending" → subscriptionsPending.toString,
      "SubscriptionsConfirmed" → subscriptionsConfirmed.toString,
      "SubscriptionsDeleted" → subscriptionsDeleted.toString,
      "DeliveryPolicy" → deliveryPolicy.map(_.toJson.compactPrint).orNull,
      "EffectiveDeliveryPolicy" → effectiveDeliveryPolicy.toJson.compactPrint
    ).filter(e ⇒ Option(e._2).isDefined)
}

object TopicAttributes {
  def apply(map: Map[String,String]): TopicAttributes = {
    val arn = TopicArn(map("TopicArn"))
    val displayName = map("DisplayName")
    val policy = Policy.fromJson(map("Policy"))
    val subscriptionsPending = map("SubscriptionsPending").toInt
    val subscriptionsConfirmed = map("SubscriptionsConfirmed").toInt
    val subscriptionsDeleted = map("SubscriptionsDeleted").toInt
    val deliveryPolicy = map.get("DeliveryPolicy").map(_.parseJson.convertTo[TopicDeliveryPolicy])
    val effectiveDeliveryPolicy = map("EffectiveDeliveryPolicy").parseJson.convertTo[TopicDeliveryPolicy]

    TopicAttributes(arn, displayName, policy, subscriptionsPending, subscriptionsConfirmed, subscriptionsDeleted,
      deliveryPolicy, effectiveDeliveryPolicy)
  }

  implicit lazy val arbTopicAttributes: Arbitrary[TopicAttributes] =
    Arbitrary {
      for {
        subscriptionsPending ← Gen.posNum[Int]
        subscriptionsConfirmed ← Gen.posNum[Int]
        subscriptionsDeleted ← Gen.posNum[Int]
        topic ← Gen.resultOf(
          TopicAttributes(_: TopicArn, _: String, _: Policy, subscriptionsPending, subscriptionsConfirmed,
            subscriptionsDeleted, _: Option[TopicDeliveryPolicy], _: TopicDeliveryPolicy))
      } yield topic
    }

  implicit lazy val shrinkTopicAttributes: Shrink[TopicAttributes] =
    Shrink { attrs ⇒
      Shrink.shrink(attrs.arn).map(x ⇒ attrs.copy(arn = x)) append
        Shrink.shrink(attrs.displayName).map(x ⇒ attrs.copy(displayName = x)) append
        Shrink.shrink(attrs.policy).map(x ⇒ attrs.copy(policy = x)) append
        Shrink.shrink(attrs.subscriptionsPending).filter(_ >= 0).map(x ⇒ attrs.copy(subscriptionsPending = x)) append
        Shrink.shrink(attrs.subscriptionsConfirmed).filter(_ >= 0).map(x ⇒ attrs.copy(subscriptionsConfirmed = x)) append
        Shrink.shrink(attrs.subscriptionsDeleted).filter(_ >= 0).map(x ⇒ attrs.copy(subscriptionsDeleted = x)) append
        Stream.empty
    }
}
