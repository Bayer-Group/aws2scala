package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.sns.model.AwsConverters._
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import org.scalacheck.{Arbitrary, Gen, Shrink}
import spray.json.{pimpAny, pimpString}

case class SubscriptionAttributes(arn: SubscriptionArn,
                                  endpoint: SubscriptionEndpoint,
                                  confirmationWasAuthenticated: Boolean,
                                  rawMessageDelivery: Boolean,
                                  deliveryPolicy: Option[SubscriptionDeliveryPolicy],
                                  effectiveDeliveryPolicy: Option[SubscriptionDeliveryPolicy]) {
  def asMap: Map[String,String] =
    Map(
      "SubscriptionArn" → arn.arnString,
      "Protocol" → endpoint.protocol.asAws,
      "Endpoint" → endpoint.endpoint,
      "TopicArn" → TopicArn(arn.account, arn.region, arn.topicName).arnString,
      "Owner" → arn.account.id,
      "ConfirmationWasAuthenticated" → confirmationWasAuthenticated.toString,
      "RawMessageDelivery" → rawMessageDelivery.toString,
      "DeliveryPolicy" → deliveryPolicy.map(_.toJson.compactPrint).orNull,
      "EffectiveDeliveryPolicy" → effectiveDeliveryPolicy.map(_.toJson.compactPrint).orNull
    ).filter(e ⇒ Option(e._2).isDefined)
}

object SubscriptionAttributes {
  private val makeSubscription =
    SubscriptionAttributes.apply(_: SubscriptionArn, _: SubscriptionEndpoint, _: Boolean, _: Boolean,
      _: Option[SubscriptionDeliveryPolicy], _: Option[SubscriptionDeliveryPolicy])

  def apply(attrs: Map[String,String]): SubscriptionAttributes = {
    val arn = SubscriptionArn(attrs("SubscriptionArn"))
    val subscriptionEndpoint = attrs("Protocol").asScala(attrs("Endpoint"))
    val confirmationWasAuthenticated = attrs("ConfirmationWasAuthenticated").toBoolean
    val rawMessageDelivery = attrs("RawMessageDelivery").toBoolean
    val deliveryPolicy = attrs.get("DeliveryPolicy").map(_.parseJson.convertTo[SubscriptionDeliveryPolicy])
    val effectiveDeliveryPolicy =
      attrs.get("EffectiveDeliveryPolicy").map(_.parseJson.convertTo[SubscriptionDeliveryPolicy])
    SubscriptionAttributes(arn, subscriptionEndpoint, confirmationWasAuthenticated, rawMessageDelivery, deliveryPolicy,
      effectiveDeliveryPolicy)
  }

  implicit lazy val arbSubscriptionAttributes: Arbitrary[SubscriptionAttributes] =
    Arbitrary(Gen.resultOf(makeSubscription))

  implicit lazy val shrinkSubscriptionAttributes: Shrink[SubscriptionAttributes] =
    Shrink.xmap(makeSubscription.tupled, SubscriptionAttributes.unapply(_).get)
}
