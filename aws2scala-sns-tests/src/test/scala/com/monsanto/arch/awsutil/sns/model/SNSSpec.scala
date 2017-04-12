package com.monsanto.arch.awsutil.sns.model

import akka.stream.scaladsl.{Flow, Source}
import com.monsanto.arch.awsutil.sns.StreamingSNSClient
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.{FlowMockUtils, Materialised}
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class SNSSpec extends FreeSpec with MockFactory with Materialised with FlowMockUtils {
  private implicit val generatorDrivenConfig = PropertyCheckConfiguration(minSuccessful = 50, sizeRange = 50)

  "the SNS interface" - {
    "creates topics" - {
      forAll { topic: Topic ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.topicCreator _)
          .expects()
          .returningFlow(topic.name, topic.arn)
        (sns.topicAttributesGetter _)
          .expects()
          .returningFlow(topic.arn, topic.attributes)

        val result = SNS.createTopic(topic.name).futureValue
        result shouldBe topic
      }
    }

    "lists topics" in {
      forAll(SizeRange(25)) { topics: List[Topic] ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.topicLister _)
          .expects()
          .returning(Source(topics.map(_.arn)))
        (sns.topicAttributesGetter _)
          .expects()
          .returning(
            Flow[String]
              .zip(Source(topics))
              .map { case (arn, topic) ⇒
                arn shouldBe topic.arn
                topic.attributes
              })

        val result = SNS.listTopics().futureValue
        result shouldBe topics
      }
    }

    "lists subscriptions" in {
      forAll(SizeRange(25)) { subscriptionSummaries: List[SubscriptionSummary] ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.subscriptionLister _)
          .expects()
          .returningConcatFlow(ListSubscriptionsRequest.allSubscriptions, subscriptionSummaries)

        val result = SNS.listSubscriptions().futureValue
        result shouldBe subscriptionSummaries
      }
    }

    "creates platform applications" - {
      "without additional attributes" in {
        forAll { (application: PlatformApplication, credentials: PlatformApplicationCredentials) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")
          val name = application.name
          val arn = application.arn
          val attributes = Map("Enabled" → "true")

          (sns.platformApplicationCreator _)
            .expects()
            .returningFlow(
              CreatePlatformApplicationRequest(name, credentials.platform.name, credentials.principal, credentials.credential),
              arn)

          (sns.platformApplicationAttributesGetter _)
            .expects()
            .returningFlow(arn, attributes)

          val result = SNS.createPlatformApplication(name, credentials).futureValue
          result shouldBe PlatformApplication(arn, attributes)
        }
      }

      "with additional attributes" in {
        forAll { (application: PlatformApplication, credentials: PlatformApplicationCredentials) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")
          val arn = application.arn
          val name = application.name
          val attributes = application.attributes - "Enabled"
          val finalAttributes = attributes + ("Enabled" → "true")

          (sns.platformApplicationCreator _)
            .expects()
            .returningFlow(
              CreatePlatformApplicationRequest(name, credentials.platform.name, credentials.principal, credentials.credential, attributes),
              arn)

          (sns.platformApplicationAttributesGetter _)
            .expects()
            .returningFlow(arn, finalAttributes)

          val result = SNS.createPlatformApplication(name, credentials, attributes).futureValue
          result shouldBe PlatformApplication(arn, finalAttributes)
        }
      }
    }

    "lists platform applications" in {
      forAll(SizeRange(25)) { applications: List[PlatformApplication] ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.platformApplicationLister _)
          .expects()
          .returning(Source(applications))

        val result = SNS.listPlatformApplications().futureValue
        result shouldBe applications
      }
    }
  }
}
