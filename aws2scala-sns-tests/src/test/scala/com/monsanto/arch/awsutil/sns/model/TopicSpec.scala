package com.monsanto.arch.awsutil.sns.model

import akka.Done
import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.auth.policy.action.SNSAction
import com.monsanto.arch.awsutil.sns.StreamingSNSClient
import com.monsanto.arch.awsutil.sns.model.AwsConverters._
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.{FlowMockUtils, Materialised}
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.{SnsGen, UtilGen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class TopicSpec extends FreeSpec with MockFactory with Materialised with FlowMockUtils {
  private implicit val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 50, maxSize = 50)

  "the Topic companion object" - {
    "can create a Topic instance from" - {
      "an attribute map" in {
        forAll { attributes: TopicAttributes ⇒
          Topic(attributes.asMap) should have(
            'arn (attributes.arn.arnString),
            'attributes (attributes.asMap),
            'deliveryPolicy (attributes.deliveryPolicy.map(_.toString)),
            'displayName (attributes.displayName),
            'effectiveDeliveryPolicy (attributes.effectiveDeliveryPolicy.toString),
            'name (attributes.arn.name),
            'owner (attributes.arn.account.id),
            'policy (attributes.policy.toString),
            'subscriptionsConfirmed (attributes.subscriptionsConfirmed),
            'subscriptionsDeleted (attributes.subscriptionsDeleted),
            'subscriptionsPending (attributes.subscriptionsPending)
          )
        }
      }

      "a topic ARN" in {
        forAll { topic: Topic ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.topicAttributesGetter _)
            .expects()
            .returningFlow(topic.arn, topic.attributes)

          Topic(topic.arn).futureValue shouldBe topic
        }
      }
    }

    "will not create a topic from something that does not look like a topic ARN" in {
      implicit val sns = mock[StreamingSNSClient]("sns")

      forAll(Gen.alphaStr → "badArn") { badArn ⇒
        an[IllegalArgumentException] shouldBe thrownBy(Topic(badArn))
      }
    }
  }

  "a Topic instance" - {
    "can refresh itself" in {
      val topicAndNewAttributes = for {
        topic ← arbitrary[Topic]
        newAttributes ← arbitrary[Topic].map(_.attributes.updated("TopicArn", topic.arn))
      } yield (topic, newAttributes)

      forAll(topicAndNewAttributes) { case (topic, newAttributes) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")
        val refreshedTopic = Topic(newAttributes)

        (sns.topicAttributesGetter _).expects()
          .returningFlow(topic.arn, newAttributes)

        val result = topic.refresh().futureValue
        result shouldBe refreshedTopic
      }
    }

    "has a toString that features the topic ARN" in {
      forAll { topic: Topic ⇒
        topic.toString shouldBe s"Topic(${topic.arn})"
      }
    }

    "should provide a name extracted from the ARN" in {
      val NameRegex = "^.*:([A-Za-z0-9_-]+)$".r
      forAll { topic: Topic ⇒
        val name = NameRegex.unapplySeq(topic.arn).get.head
        topic.name shouldBe name
      }
    }

    "can update its display name" in {
      forAll { (topic: Topic, displayName: String) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.topicAttributeSetter _)
          .expects()
          .returningFlow(SetTopicAttributesRequest(topic.arn, "DisplayName", displayName), topic.arn)

        val result = topic.setDisplayName(displayName).futureValue
        result shouldBe Done
      }
    }

    "can update its policy" in {
      forAll { (topic: Topic, policy: Policy) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.topicAttributeSetter _)
          .expects()
          .returningFlow(SetTopicAttributesRequest(topic.arn, "Policy", policy.toString), topic.arn)

        val result = topic.setPolicy(policy.toString).futureValue
        result shouldBe Done
      }
    }

    "can update its delivery policy" in {
      forAll { (topic: Topic, maybeDeliveryPolicy: Option[TopicDeliveryPolicy]) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")
        val deliveryPolicy = maybeDeliveryPolicy.map(_.toString)

        (sns.topicAttributeSetter _)
          .expects()
          .returningFlow(SetTopicAttributesRequest(topic.arn, "DeliveryPolicy", deliveryPolicy), topic.arn)

        val result = topic.setDeliveryPolicy(deliveryPolicy).futureValue
        result shouldBe Done
      }
    }

    "can add a permission" in {
      forAll(
        arbitrary[Topic] → "topic",
        UtilGen.nonEmptyString → "label",
        Gen.nonEmptyListOf(arbitrary[Account].map(_.id)) → "accounts",
        Gen.nonEmptyListOf(arbitrary[SNSAction]) → "actions"
      ) { (topic, label, accounts, actions) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.permissionAdder _)
          .expects()
          .returningFlow(AddPermissionRequest(topic.arn, label, accounts, actions), topic.arn)

        val result = topic.addPermission(label, accounts, actions).futureValue
        result shouldBe Done
      }
    }

    "can remove a permission" in {
      forAll(arbitrary[Topic] → "topic", UtilGen.nonEmptyString → "label") { (topic, label) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.permissionRemover _)
          .expects()
          .returningFlow(RemovePermissionRequest(topic.arn, label), topic.arn)

        val result = topic.removePermission(label).futureValue
        result shouldBe Done
      }
    }

    "can delete itself" in {
      forAll { topic: Topic ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.topicDeleter _)
          .expects()
          .returningFlow(topic.arn, topic.arn)

        topic.delete().futureValue shouldBe Done
      }
    }

    "lists subscriptions to itself" in {
      forAll { topicAndSummaries: (Topic, List[SubscriptionSummary]) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")
        val (topic, summaries) = topicAndSummaries

        (sns.subscriptionLister _)
          .expects()
          .returningConcatFlow(ListSubscriptionsRequest.forTopic(topic.arn), summaries)

        val result = topic.listSubscriptions().futureValue
        result shouldBe summaries
      }
    }

    "can initiate a subscription" - {
      "that succeeds immediately" in {
        forAll { topicWithSubscription: (Topic,Subscription) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")
          val (topic, subscription) = topicWithSubscription
          val SubscriptionEndpoint(protocol, endpoint) = subscription.endpoint

          (sns.subscriber _)
            .expects()
            .returningFlow(SubscribeRequest(topic.arn, protocol.asAws, endpoint), Some(subscription.arn))
          (sns.subscriptionAttributesGetter _)
            .expects()
            .returningFlow(subscription.arn, subscription.attributes)

          val result = topic.subscribe(subscription.endpoint).futureValue
          result shouldBe Some(subscription)
        }
      }

      "that requires confirmation" in {
        forAll { (topic: Topic, subscriptionEndpoint: SubscriptionEndpoint) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")
          val SubscriptionEndpoint(protocol, endpoint) = subscriptionEndpoint

          (sns.subscriber _)
            .expects()
            .returningFlow(SubscribeRequest(topic.arn, protocol.asAws, endpoint), None)
          (sns.subscriptionAttributesGetter _)
            .expects()
            .returningFailingFlow()

          val result = topic.subscribe(subscriptionEndpoint).futureValue
          result shouldBe None
        }
      }
    }

    "can confirm a subscription" - {
      "without specifying authenticate on unsubscribe" in {
        forAll(
          arbitrary[(Topic,Subscription)] → "topicWithSubscription",
          SnsGen.confirmationToken → "confirmationToken"
        ) { (topicWithSubscription, token) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")
          val (topic, subscription) = topicWithSubscription

          (sns.subscriptionConfirmer _)
            .expects()
            .returningFlow(ConfirmSubscriptionRequest(topic.arn, token), subscription.arn)
          (sns.subscriptionAttributesGetter _)
            .expects()
            .returningFlow(subscription.arn, subscription.attributes)

          val result = topic.confirmSubscription(token).futureValue
          result shouldBe subscription
        }
      }

      "specifying authenticate on unsubscribe" in {
        forAll(
          arbitrary[(Topic,Subscription)] → "topicAndSub",
          SnsGen.confirmationToken → "token",
          arbitrary[Boolean] → "authOnUnsubscribe"
        ) { (topicAndSub, token, authOnUnsubscribe) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")
          val (topic, subscription) = topicAndSub

          (sns.subscriptionConfirmer _)
            .expects()
            .returningFlow(ConfirmSubscriptionRequest(topic.arn, token, Some(authOnUnsubscribe)), subscription.arn)
          (sns.subscriptionAttributesGetter _)
            .expects()
            .returningFlow(subscription.arn, subscription.attributes)

          val result = topic.confirmSubscription(token, authOnUnsubscribe).futureValue
          result shouldBe subscription
        }
      }
    }

    "can publish a" - {
      "simple messages" - {
        "using just the message" in {
          forAll(
            arbitrary[Topic] → "topic",
            UtilGen.nonEmptyString → "message",
            SnsGen.messageId → "messageId"
          ) { (topic, message, messageId) ⇒
            implicit val sns = mock[StreamingSNSClient]("sns")

            (sns.publisher _)
              .expects()
              .returningFlow(PublishRequest(topic.arn, message), messageId)

            val result = topic.publish(message).futureValue
            result shouldBe messageId
          }
        }

        "using the message and a subject" in {
          forAll(
            arbitrary[Topic] → "topic",
            UtilGen.nonEmptyString → "message",
            UtilGen.nonEmptyString → "subject",
            SnsGen.messageId → "messageId"
          ) { (topic, message, subject, messageId) ⇒
            implicit val sns = mock[StreamingSNSClient]("sns")

            (sns.publisher _)
              .expects()
              .returningFlow(PublishRequest(topic.arn, message, subject), messageId)

            val result = topic.publish(message, subject).futureValue
            result shouldBe messageId
          }
        }

        "using the message and some attributes" in {
          forAll(
            arbitrary[Topic] → "topic",
            UtilGen.nonEmptyString → "message",
            arbitrary[Map[String,MessageAttributeValue]] → "attributes",
            SnsGen.messageId → "messageId"
          ) { (topic, message, attributes, messageId) ⇒
            implicit val sns = mock[StreamingSNSClient]("sns")

            (sns.publisher _)
              .expects()
              .returningFlow(PublishRequest(topic.arn, message, attributes), messageId)

            val result = topic.publish(message, attributes).futureValue
            result shouldBe messageId
          }
        }

        "using the message, a subject, and some attributes" in {
          forAll(
            arbitrary[Topic] → "topic",
            UtilGen.nonEmptyString → "message",
            UtilGen.nonEmptyString → "subject",
            arbitrary[Map[String,MessageAttributeValue]] → "attributes",
            SnsGen.messageId → "messageId"
          ) { (topic, message, subject, attributes, messageId) ⇒
            implicit val sns = mock[StreamingSNSClient]("sns")

            (sns.publisher _)
              .expects()
              .returningFlow(PublishRequest(topic.arn, message, subject, attributes), messageId)

            val result = topic.publish(message, subject, attributes).futureValue
            result shouldBe messageId
          }
        }
      }

      "compound messages" - {
        "using just the message map" in {
          forAll(
            arbitrary[Topic] → "topic",
            SnsGen.messageMap → "messageMap",
            SnsGen.messageId → "messageId"
          ) { (topic, messageMap, messageId) ⇒
            implicit val sns = mock[StreamingSNSClient]("sns")

            (sns.publisher _)
              .expects()
              .returningFlow(PublishRequest(topic.arn, messageMap), messageId)

            val result = topic.publish(messageMap).futureValue
            result shouldBe messageId
          }
        }

        "using the message and a subject" in {
          forAll(
            arbitrary[Topic] → "topic",
            SnsGen.messageMap → "messageMap",
            UtilGen.nonEmptyString → "subject",
            SnsGen.messageId → "messageId"
          ) { (topic, messageMap, subject, messageId) ⇒
            implicit val sns = mock[StreamingSNSClient]("sns")

            (sns.publisher _)
              .expects()
              .returningFlow(PublishRequest(topic.arn, messageMap, subject), messageId)

            val result = topic.publish(messageMap, subject).futureValue
            result shouldBe messageId
          }
        }

        "using the message and some attributes" in {
          forAll(
            arbitrary[Topic] → "topic",
            SnsGen.messageMap → "messageMap",
            arbitrary[Map[String,MessageAttributeValue]] → "attributes",
            SnsGen.messageId → "messageId"
          ) { (topic, messageMap, attributes, messageId) ⇒
            implicit val sns = mock[StreamingSNSClient]("sns")

            (sns.publisher _)
              .expects()
              .returningFlow(PublishRequest(topic.arn, messageMap, attributes), messageId)

            val result = topic.publish(messageMap, attributes).futureValue
            result shouldBe messageId
          }
        }

        "using the message, a subject, and some attributes" in {
          forAll(
            arbitrary[Topic] → "topic",
            SnsGen.messageMap → "messageMap",
            UtilGen.nonEmptyString → "subject",
            arbitrary[Map[String,MessageAttributeValue]] → "attributes",
            SnsGen.messageId → "messageId"
          ) { (topic, messageMap, subject, attributes, messageId) ⇒
            implicit val sns = mock[StreamingSNSClient]("sns")

            (sns.publisher _)
              .expects()
              .returningFlow(PublishRequest(topic.arn, messageMap, subject, attributes), messageId)

            val result = topic.publish(messageMap, subject, attributes).futureValue
            result shouldBe messageId
          }
        }
      }
    }
  }

  implicit val arbTopicWithSubscription: Arbitrary[(Topic, Subscription)] =
    Arbitrary {
      for {
        topic ← arbitrary[Topic]
        subscriptionId ← SnsGen.subscriptionId
        endpoint ← arbitrary[SubscriptionEndpoint]
        confirmationWasAuthenticated ← arbitrary[Boolean]
        rawMessageDelivery ← arbitrary[Boolean]
        deliveryPolicy ← arbitrary[Option[SubscriptionDeliveryPolicy]]
        effectiveDeliveryPolicy ← arbitrary[Option[SubscriptionDeliveryPolicy]]
      } yield {
        val topicArn = TopicArn.fromArnString(topic.arn)
        val arn = SubscriptionArn(topicArn.account, topicArn.region, topic.name, subscriptionId)
        val attrs = SubscriptionAttributes(arn, endpoint, confirmationWasAuthenticated, rawMessageDelivery,
          deliveryPolicy, effectiveDeliveryPolicy)
        (topic, Subscription(attrs.asMap))
      }
    }

  implicit val arbTopicWithSummaries: Arbitrary[(Topic, List[SubscriptionSummary])] =
    Arbitrary {
      for {
        topic ← arbitrary[Topic]
        summaries ← UtilGen.nonEmptyListOfSqrtN( subscriptionSummaryForTopic(topic))
      } yield (topic, summaries)
    }

  def subscriptionSummaryForTopic(topic: Topic): Gen[SubscriptionSummary] = {
    val pendingSubscription =
      for {
        endpoint ← arbitrary[SubscriptionEndpoint]
      } yield SubscriptionSummary(None, topic.arn, endpoint, topic.owner)
    val confirmedSubscription =
      for {
        subscriptionId ← SnsGen.subscriptionId
        endpoint ← arbitrary[SubscriptionEndpoint]
      } yield {
        val topicArn = TopicArn.fromArnString(topic.arn)
        val subscriptionArn = SubscriptionArn(topicArn.account, topicArn.region, topicArn.name, subscriptionId)
        SubscriptionSummary(Some(subscriptionArn.arnString), topic.arn, endpoint, topic.owner)
      }

    Gen.frequency(
      9 → confirmedSubscription,
      1 → pendingSubscription
    )
  }
}
