package com.monsanto.arch.awsutil.sns.model

import akka.stream.scaladsl.{Flow, Source}
import com.monsanto.arch.awsutil.sns.StreamingSNSClient
import com.monsanto.arch.awsutil.test.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.{FlowMockUtils, Materialised}
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class SNSSpec extends FreeSpec with MockFactory with Materialised with FlowMockUtils {
  private implicit val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 50, maxSize = 50)

  "the SNS interface" - {
    "creates topics" - {
      forAll(SNSGen.topic → "topic") { topic ⇒
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
      forAll(Gen.listOf(SNSGen.topic) → "topics", maxSize(25)) { topics ⇒
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
      forAll(Gen.listOf(SNSGen.subscriptionSummary) → "subscriptionSummaries", maxSize(25)) { subscriptionSummaries ⇒
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
        forAll(SNSGen.applicationName → "name", SNSGen.platformApplicationCredentials → "credentials") { (name, credentials) ⇒
          val maybeArn = SNSGen.platformApplicationArn(credentials.platform, name).sample
          whenever(maybeArn.isDefined) {
            implicit val sns = mock[StreamingSNSClient]("sns")
            val attributes = Map("Enabled" → "true")
            val arn = maybeArn.get

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
      }

      "with additional attributes" in {
        forAll(SNSGen.applicationName → "name", SNSGen.platformApplicationCredentials → "credentials") { (name, credentials) ⇒
          val maybeApplication = SNSGen.platformApplication(name, credentials).sample
          whenever(maybeApplication.isDefined) {
            implicit val sns = mock[StreamingSNSClient]("sns")
            val arn = maybeApplication.get.arn
            val attributes = maybeApplication.get.attributes - "Enabled"
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
    }

    "lists platform applications" in {
      forAll(Gen.listOf(SNSGen.platformApplication) → "applications", maxSize(25)) { applications ⇒
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
