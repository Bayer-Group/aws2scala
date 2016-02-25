package com.monsanto.arch.awsutil.sns.model

import akka.Done
import com.monsanto.arch.awsutil.sns.StreamingSNSClient
import com.monsanto.arch.awsutil.sns.model.SNSGen.TopicArn
import com.monsanto.arch.awsutil.test.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.{AwsGen, FlowMockUtils, Materialised}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class TopicSpec extends FreeSpec with MockFactory with Materialised with FlowMockUtils {
  private implicit val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 50, maxSize = 50)

  "the Topic companion object" - {
    "can create a Topic instance from" - {
      "an attribute map" in {
        forAll(
          SNSGen.topicArnObj → "topicArnObj",
          Gen.alphaStr → "displayName",
          Gen.zip(Gen.choose(0, 10), Gen.choose(0, 100), Gen.choose(0, 50)) → "subscriptionCounts",
          SNSGen.policy → "policy",
          SNSGen.maybeTopicDeliveryPolicy → "deliveryPolicy",
          SNSGen.topicDeliveryPolicy → "effectiveDeliveryPolicy"
        ) { (topicArnObj, displayName, subscriptionCounts, policy, deliveryPolicy, effectiveDeliveryPolicy) ⇒
          val TopicArn(_, owner, name) = topicArnObj
          val arn = topicArnObj.toString
          val (subscriptionsPending, subscriptionsConfirmed, subscriptionsDeleted) = subscriptionCounts
          val attributes =
            deliveryPolicy.map(p ⇒ Map("DeliveryPolicy" → p)).getOrElse(Map.empty) ++
              Map(
                "TopicArn" → arn,
                "DisplayName" → displayName,
                "Owner" → owner,
                "Policy" → policy,
                "SubscriptionsPending" → subscriptionsPending.toString,
                "SubscriptionsConfirmed" → subscriptionsConfirmed.toString,
                "SubscriptionsDeleted" → subscriptionsDeleted.toString,
                "EffectiveDeliveryPolicy" → effectiveDeliveryPolicy
              )
          val result = Topic(attributes)
          result.attributes shouldBe attributes
          result.arn shouldBe arn
          result.name shouldBe name
          result.displayName shouldBe displayName
          result.owner shouldBe owner
          result.policy shouldBe policy
          result.subscriptionsPending shouldBe subscriptionsPending
          result.subscriptionsConfirmed shouldBe subscriptionsConfirmed
          result.subscriptionsDeleted shouldBe subscriptionsDeleted
          result.deliveryPolicy shouldBe deliveryPolicy
          result.effectiveDeliveryPolicy shouldBe effectiveDeliveryPolicy
        }
      }

      "a topic ARN" in {
        forAll(SNSGen.topic → "topic") { topic ⇒
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
        an [IllegalArgumentException] shouldBe thrownBy(Topic(badArn))
      }
    }
  }

  "a Topic instance" - {
    "can refresh itself" in {
      val topicAndNewAttributes = for {
        topic ← SNSGen.topic
        newAttributes ← SNSGen.topic.map(_.attributes.updated("TopicArn", topic.arn))
      } yield (topic, newAttributes)

      forAll(topicAndNewAttributes → "topicAndNewAttributes") { topicAndNewAttributes ⇒
        val (topic, newAttributes) = topicAndNewAttributes
        implicit val sns = mock[StreamingSNSClient]("sns")
        val refreshedTopic = Topic(newAttributes)

        (sns.topicAttributesGetter _).expects()
          .returningFlow(topic.arn, newAttributes)

        val result = topic.refresh().futureValue
        result shouldBe refreshedTopic
      }
    }

    "has a toString that features the topic ARN" in {
      forAll(SNSGen.topic → "topic") { topic ⇒
        topic.toString shouldBe s"Topic(${topic.arn})"
      }
    }

    "should provide a name extracted from the ARN" in {
      val NameRegex = "^.*:([A-Za-z0-9_-]+)$".r
      forAll(SNSGen.topic → "topic") { topic ⇒
        val name = NameRegex.unapplySeq(topic.arn).get.head
        topic.name shouldBe name
      }
    }

    "can update its display name" in {
      forAll(SNSGen.topic → "topic", Gen.alphaStr → "displayName") { (topic, displayName) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.topicAttributeSetter _)
          .expects()
          .returningFlow(SetTopicAttributesRequest(topic.arn, "DisplayName", displayName), topic.arn)

        val result = topic.setDisplayName(displayName).futureValue
        result shouldBe Done
      }
    }

    "can update its policy" in {
      forAll(SNSGen.topic → "topic", SNSGen.policy → "policy") { (topic, policy) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.topicAttributeSetter _)
          .expects()
          .returningFlow(SetTopicAttributesRequest(topic.arn, "Policy", policy), topic.arn)

        val result = topic.setPolicy(policy).futureValue
        result shouldBe Done
      }
    }

    "can update its delivery policy" in {
      forAll(
        SNSGen.topic → "topic",
        SNSGen.maybeTopicDeliveryPolicy → "maybeDeliveryPolicy"
      ) { (topic, maybeDeliveryPolicy) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.topicAttributeSetter _)
          .expects()
          .returningFlow(SetTopicAttributesRequest(topic.arn, "DeliveryPolicy", maybeDeliveryPolicy), topic.arn)

        val result = topic.setDeliveryPolicy(maybeDeliveryPolicy).futureValue
        result shouldBe Done
      }
    }

    "can add a permission" in {
      forAll(
        SNSGen.topic → "topic",
        SNSGen.nonEmptyString → "label",
        Gen.nonEmptyListOf(arbitrary[AwsGen.Account].map(_.value)) → "accounts",
        Gen.nonEmptyListOf(SNSGen.snsAction) → "actions"
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
      forAll(SNSGen.topic → "topic", SNSGen.nonEmptyString → "label") { (topic, label) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.permissionRemover _)
          .expects()
          .returningFlow(RemovePermissionRequest(topic.arn, label), topic.arn)

        val result = topic.removePermission(label).futureValue
        result shouldBe Done
      }
    }

    "can delete itself" in {
      forAll(SNSGen.topic → "topic") { topic ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.topicDeleter _)
          .expects()
          .returningFlow(topic.arn, topic.arn)

        topic.delete().futureValue shouldBe Done
      }
    }

    "lists subscriptions to itself" in {
      val topicAndSummaries =
        for {
          topic ← SNSGen.topic
          summaries ← Gen.listOf(SNSGen.subscriptionSummary(topic.arn))
        } yield (topic, summaries)
      forAll(topicAndSummaries → "topicAndSummaries", maxSize(25)) { topicAndSummaries ⇒
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
        forAll(SNSGen.topicWithSubscription → "topicWithSubscription") { topicWithSubscription ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")
          val (topic, subscription) = topicWithSubscription
          val SubscriptionEndpoint(protocol, endpoint) = subscription.endpoint

          (sns.subscriber _)
            .expects()
            .returningFlow(SubscribeRequest(topic.arn, protocol.toAws, endpoint), Some(subscription.arn))
          (sns.subscriptionAttributesGetter _)
            .expects()
            .returningFlow(subscription.arn, subscription.attributes)

          val result = topic.subscribe(subscription.endpoint).futureValue
          result shouldBe Some(subscription)
        }
      }

      "that requires confirmation" in {
        forAll(
          SNSGen.topic → "topic",
          SNSGen.subscriptionEndpoint → "subscriptionEndpoint"
        ) { (topic, subscriptionEndpoint) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")
          val SubscriptionEndpoint(protocol, endpoint) = subscriptionEndpoint

          (sns.subscriber _)
            .expects()
            .returningFlow(SubscribeRequest(topic.arn, protocol.toAws, endpoint), None)
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
        forAll(SNSGen.topicWithSubscription → "topicAndSub", SNSGen.confirmationToken → "token") { (topicAndSub, token) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")
          val (topic, subscription) = topicAndSub

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
          SNSGen.topicWithSubscription → "topicAndSub",
          SNSGen.confirmationToken → "token",
          SNSGen.boolean → "authOnUnsubscribe"
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
            SNSGen.topic → "topic",
            SNSGen.nonEmptyString → "message",
            SNSGen.messageId → "messageId"
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
            SNSGen.topic → "topic",
            SNSGen.nonEmptyString → "message",
            SNSGen.nonEmptyString → "subject",
            SNSGen.messageId → "messageId"
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
            SNSGen.topic → "topic",
            SNSGen.nonEmptyString → "message",
            SNSGen.messageAttributes → "attributes",
            SNSGen.messageId → "messageId"
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
            SNSGen.topic → "topic",
            SNSGen.nonEmptyString → "message",
            SNSGen.nonEmptyString → "subject",
            SNSGen.messageAttributes → "attributes",
            SNSGen.messageId → "messageId"
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
            SNSGen.topic → "topic",
            SNSGen.messageMap → "messageMap",
            SNSGen.messageId → "messageId"
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
            SNSGen.topic → "topic",
            SNSGen.messageMap → "messageMap",
            SNSGen.nonEmptyString → "subject",
            SNSGen.messageId → "messageId"
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
            SNSGen.topic → "topic",
            SNSGen.messageMap → "messageMap",
            SNSGen.messageAttributes → "attributes",
            SNSGen.messageId → "messageId"
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
            SNSGen.topic → "topic",
            SNSGen.messageMap → "messageMap",
            SNSGen.nonEmptyString → "subject",
            SNSGen.messageAttributes → "attributes",
            SNSGen.messageId → "messageId"
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
}
