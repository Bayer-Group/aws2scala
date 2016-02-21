package com.monsanto.arch.awsutil.sns.model

import java.util.UUID

import akka.Done
import com.monsanto.arch.awsutil.sns.StreamingSNSClient
import com.monsanto.arch.awsutil.{AwsGen, FlowMockUtils, Materialised}
import org.scalacheck.Arbitrary.arbitrary
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class SubscriptionSpec extends FreeSpec with Materialised with MockFactory with FlowMockUtils {
  private implicit val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 50, maxSize = 50)

  "the Subscription companion object" - {
    "can build a subscription using" - {
      "a set of attributes (and back)" in {
        forAll(
          SNSGen.topicArn → "topicArn",
          arbitrary[AwsGen.Account].map(_.value) → "owner",
          SNSGen.boolean → "rawMessageDelivery",
          SNSGen.boolean → "confirmationWasAuthenticated",
          SNSGen.subscriptionEndpoint → "subscriptionEndpoint",
          SNSGen.maybeSubscriptionDeliveryPolicy → "maybeDeliveryPolicy"
        ) { (topicArn, owner, rawMessageDelivery, confirmationWasAuthenticated, subscriptionEndpoint, maybeDeliveryPolicy) ⇒
          val subscriptionArn = s"$topicArn:${UUID.randomUUID()}"
          val attributes =
            maybeDeliveryPolicy
              .map(p ⇒ Map("DeliveryPolicy" → p, "EffectiveDeliveryPolicy" → p))
              .getOrElse(Map.empty) ++
              Map(
                "SubscriptionArn" → subscriptionArn,
                "TopicArn" → topicArn,
                "Protocol" → subscriptionEndpoint.protocol.toAws,
                "Endpoint" → subscriptionEndpoint.endpoint,
                "Owner" → owner,
                "RawMessageDelivery" → rawMessageDelivery.toString,
                "ConfirmationWasAuthenticated" → confirmationWasAuthenticated.toString
              )

          val result = Subscription(attributes)
          result.attributes shouldBe attributes
          result.arn shouldBe subscriptionArn
          result.topicArn shouldBe topicArn
          result.endpoint shouldBe subscriptionEndpoint
          result.owner shouldBe owner
          result.confirmationWasAuthenticated shouldBe confirmationWasAuthenticated
          result.rawMessageDelivery shouldBe rawMessageDelivery
          result.deliveryPolicy shouldBe maybeDeliveryPolicy
          result.effectiveDeliveryPolicy shouldBe maybeDeliveryPolicy
        }
      }

      "a subscription ARN" in {
        forAll(SNSGen.subscription → "subscription") { subscription ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.subscriptionAttributesGetter _)
            .expects()
            .returningFlow(subscription.arn, subscription.attributes)

          val result = Subscription(subscription.arn).futureValue
          result shouldBe subscription
        }
      }
    }
  }

  "a Subscription instance should" - {
    "refresh" in {
      forAll(
        SNSGen.subscription → "subscription",
        SNSGen.boolean → "newRawMessageDelivery",
        SNSGen.maybeSubscriptionDeliveryPolicy → "newDeliveryPolicy"
      ) { (subscription, newRawMessageDelivery, newDeliveryPolicy) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        val newAttributes = newDeliveryPolicy match {
          case None ⇒
            subscription.attributes +
              ("RawMessageDelivery" → newRawMessageDelivery.toString) -
              "DeliveryPolicy" -
              "EffectiveDeliveryPolicy"
          case Some(p) ⇒
            subscription.attributes +
              ("RawMessageDelivery" → newRawMessageDelivery.toString) +
              ("DeliveryPolicy" → p) +
              ("EffectiveDeliveryPolicy" → p)
        }
        val newSubscription = Subscription(newAttributes)

        (sns.subscriptionAttributesGetter _)
          .expects()
          .returningFlow(subscription.arn, newAttributes)

        val result = subscription.refresh().futureValue
        result shouldBe newSubscription
      }
    }

    "have a toString that is mainly the ARN" in {
      forAll(SNSGen.subscription → "subscription") { subscription ⇒
        subscription.toString shouldBe s"Subscription(${subscription.arn})"
      }
    }

    "allow setting the raw message delivery attribute" in {
      forAll(
        SNSGen.subscription → "subscription",
        SNSGen.boolean → "rawMessageDelivery"
      ) { (subscription, rawMessageDelivery) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.subscriptionAttributeSetter _)
          .expects()
          .returningFlow(
            SetSubscriptionAttributesRequest(subscription.arn, "RawMessageDelivery", rawMessageDelivery.toString),
            subscription.arn)

        val result = subscription.setRawMessageDelivery(rawMessageDelivery).futureValue
        result shouldBe Done
      }
    }

    "allow setting the delivery policy attribute" in {
      forAll(
        SNSGen.subscription → "subscription",
        SNSGen.maybeSubscriptionDeliveryPolicy → "maybeDeliveryPolicy"
      ) { (subscription, maybeDeliveryPolicy) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.subscriptionAttributeSetter _)
          .expects()
          .returningFlow(
            SetSubscriptionAttributesRequest(subscription.arn, "DeliveryPolicy", maybeDeliveryPolicy),
            subscription.arn)

        val result = subscription.setDeliveryPolicy(maybeDeliveryPolicy).futureValue
        result shouldBe Done
      }
    }

    "delete itself" in {
      forAll(SNSGen.subscription → "subscription") { subscription ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.unsubscriber _)
          .expects()
          .returningFlow(subscription.arn, subscription.arn)

        val result = subscription.unsubscribe().futureValue
        result shouldBe Done
      }
    }
  }
}
