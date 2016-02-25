package com.monsanto.arch.awsutil.sns.model

import akka.Done
import com.monsanto.arch.awsutil.sns.StreamingSNSClient
import com.monsanto.arch.awsutil.test.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.{AwsGen, FlowMockUtils, Materialised}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class PlatformApplicationSpec extends FreeSpec with MockFactory with Materialised with FlowMockUtils {
  private implicit val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 50, maxSize = 50)

  "the PlatformApplication object can" - {
    "build an object from an ARN" in {
      def feedbakRoleGen(owner: String, name: String) =
        Gen.option(Gen.oneOf(Gen.const(""), Gen.const(s"arn:aws:iam::$owner:role/$name")))
      val argsGen =
        for {
          region ← SNSGen.region
          owner ← arbitrary[AwsGen.Account].map(_.value)
          enabled ← SNSGen.boolean
          name ← SNSGen.applicationName
          credentials ← SNSGen.platformApplicationCredentials
          eventEndpointCreated ← Gen.option(SNSGen.topicArn(region, owner))
          eventEndpointDeleted ← Gen.option(SNSGen.topicArn(region, owner))
          eventEndpointUpdated ← Gen.option(SNSGen.topicArn(region, owner))
          eventDeliveryFailure ← Gen.option(SNSGen.topicArn(region, owner))
          successFeedbackRoleArn ← feedbakRoleGen(owner, "SNSSuccessFeedback")
          failureFeedbackRoleArn ← feedbakRoleGen(owner, "SNSFailureFeedback")
          successFeedbackSampleRate ← Gen.option(Gen.choose(0, 100))
        } yield {
          val arn = SNSGen.platformApplicationArn(region, owner, credentials.platform, name)
          val attributes =
            Map(
              "Enabled" → Some(enabled.toString),
              "EventEndpointCreated" → eventEndpointCreated,
              "EventEndpointDeleted" → eventEndpointDeleted,
              "EventEndpointUpdated" → eventEndpointUpdated,
              "EventDeliveryFailure" → eventDeliveryFailure,
              "SuccessFeedbackRoleArn" → successFeedbackRoleArn,
              "FailureFeedbackRoleArn" → failureFeedbackRoleArn,
              "SuccessFeedbackSampleRate" → successFeedbackSampleRate.map(_.toString)
            ).filter(_._2.isDefined).mapValues(_.get)
          val eventTopics = (eventEndpointCreated, eventEndpointDeleted, eventEndpointUpdated, eventDeliveryFailure)
          val feedbackProps = (
            successFeedbackRoleArn.filter(_.nonEmpty),
            failureFeedbackRoleArn.filter(_.nonEmpty),
            successFeedbackSampleRate
          )
          (arn, name, credentials.platform, enabled, eventTopics, feedbackProps, attributes)
        }
      forAll(argsGen) { args ⇒
        val (arn, name, platform, enabled, eventTopics, feedbackProps, attributes) = args
        val (eventEndpointCreated, eventEndpointDeleted, eventEndpointUpdated, eventDeliveryFailure) = eventTopics
        val (successFeedbackRoleArn, failureFeedbackRoleArn, successFeedbackSampleRate) = feedbackProps
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.platformApplicationAttributesGetter _)
          .expects()
          .returningFlow(arn, attributes)

        val result = PlatformApplication(arn).futureValue
        result shouldBe PlatformApplication(arn, attributes)
        result.name shouldBe name
        result.platform shouldBe platform
        result.enabled shouldBe enabled
        result.eventEndpointCreated shouldBe eventEndpointCreated
        result.eventEndpointDeleted shouldBe eventEndpointDeleted
        result.eventEndpointUpdated shouldBe eventEndpointUpdated
        result.eventDeliveryFailure shouldBe eventDeliveryFailure
        result.successFeedbackRoleArn shouldBe successFeedbackRoleArn
        result.failureFeedbackRoleArn shouldBe failureFeedbackRoleArn
        result.successFeedbackSampleRate shouldBe successFeedbackSampleRate
      }
    }
  }

  "a PlatformApplication instance" - {
    "provides a refresh method" in {
      forAll(
        SNSGen.platformApplication → "platformApplication",
        SNSGen.platformApplication.map(_.attributes) → "newAttributes"
      ) { (platformApplication, newAttributes) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.platformApplicationAttributesGetter _)
          .expects()
          .returningFlow(platformApplication.arn, newAttributes)

        val result = platformApplication.refresh().futureValue
        result shouldBe PlatformApplication(platformApplication.arn, newAttributes)
      }
    }

    "can update its" - {
      "enabled attribute" in {
        forAll(SNSGen.platformApplication → "application", SNSGen.boolean → "enabled") { (application, enabled) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.platformApplicationAttributesSetter _)
            .expects()
            .returningFlow(
              SetPlatformApplicationAttributesRequest(application.arn, "Enabled", enabled.toString),
              application.arn)

          val result = application.setEnabled(enabled).futureValue
          result shouldBe Done
        }
      }

      "endpoint created topic" in {
        forAll(
          SNSGen.platformApplication → "application",
          Gen.option(SNSGen.topicArn) → "maybeTopicArn"
        ) { (application, maybeTopicArn) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.platformApplicationAttributesSetter _)
            .expects()
            .returningFlow(
              SetPlatformApplicationAttributesRequest(application.arn, "EventEndpointCreated", maybeTopicArn),
              application.arn)

          val result = application.setEventEndpointCreated(maybeTopicArn).futureValue
          result shouldBe Done
        }
      }

      "endpoint deleted topic" in {
        forAll(
          SNSGen.platformApplication → "application",
          Gen.option(SNSGen.topicArn) → "maybeTopicArn"
        ) { (application, maybeTopicArn) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.platformApplicationAttributesSetter _)
            .expects()
            .returningFlow(
              SetPlatformApplicationAttributesRequest(application.arn, "EventEndpointDeleted", maybeTopicArn),
              application.arn)

          val result = application.setEventEndpointDeleted(maybeTopicArn).futureValue
          result shouldBe Done
        }
      }

      "endpoint updated topic" in {
        forAll(
          SNSGen.platformApplication → "application",
          Gen.option(SNSGen.topicArn) → "maybeTopicArn"
        ) { (application, maybeTopicArn) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.platformApplicationAttributesSetter _)
            .expects()
            .returningFlow(
              SetPlatformApplicationAttributesRequest(application.arn, "EventEndpointUpdated", maybeTopicArn),
              application.arn)

          val result = application.setEventEndpointUpdated(maybeTopicArn).futureValue
          result shouldBe Done
        }
      }

      "delivery failure topic" in {
        forAll(
          SNSGen.platformApplication → "application",
          Gen.option(SNSGen.topicArn) → "maybeTopicArn"
        ) { (application, maybeTopicArn) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.platformApplicationAttributesSetter _)
            .expects()
            .returningFlow(
              SetPlatformApplicationAttributesRequest(application.arn, "EventDeliveryFailure", maybeTopicArn),
              application.arn)

          val result = application.setEventDeliveryFailure(maybeTopicArn).futureValue
          result shouldBe Done
        }
      }

      "success feedback role" in {
        forAll(
          SNSGen.platformApplication → "application",
          Gen.option(arbitrary[AwsGen.IAM.RoleArn].map(_.value)) → "maybeRoleArn"
        ) { (application, maybeRoleArn) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.platformApplicationAttributesSetter _)
            .expects()
            .returningFlow(
              SetPlatformApplicationAttributesRequest(application.arn, "SuccessFeedbackRoleArn", maybeRoleArn),
              application.arn)

          val result = application.setSuccessFeedbackRoleArn(maybeRoleArn).futureValue
          result shouldBe Done
        }
      }

      "failure feedback role" in {
        forAll(
          SNSGen.platformApplication → "application",
          Gen.option(arbitrary[AwsGen.IAM.RoleArn].map(_.value)) → "maybeRoleArn"
        ) { (application, maybeRoleArn) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.platformApplicationAttributesSetter _)
            .expects()
            .returningFlow(
              SetPlatformApplicationAttributesRequest(application.arn, "FailureFeedbackRoleArn", maybeRoleArn),
              application.arn)

          val result = application.setFailureFeedbackRoleArn(maybeRoleArn).futureValue
          result shouldBe Done
        }
      }

      "success feedback sample rate" in {
        forAll(
          SNSGen.platformApplication → "application",
          Gen.choose(0, 100) → "sampleRate"
        ) { (application, sampleRate) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.platformApplicationAttributesSetter _)
            .expects()
            .returningFlow(
              SetPlatformApplicationAttributesRequest(application.arn, "SuccessFeedbackSampleRate", sampleRate.toString),
              application.arn)

          val result = application.setSuccessFeedbackSampleRate(sampleRate).futureValue
          result shouldBe Done
        }
      }

      "credentials" - {
        "when the platform matches" in {
          val argsGen =
            for {
              application ← SNSGen.platformApplication
              credentials ← SNSGen.platformApplicationCredentials.retryUntil(_.platform == application.platform)
            } yield (application, credentials)

          forAll(argsGen) { args ⇒
            val (application, credentials @ PlatformApplicationCredentials(_, principal, credential)) = args
            implicit val sns = mock[StreamingSNSClient]("sns")

            (sns.platformApplicationAttributesSetter _)
              .expects()
              .returningFlow(
                SetPlatformApplicationAttributesRequest(
                  application.arn,
                  Map("PlatformPrincipal" → principal, "PlatformCredential" → credential)),
                application.arn)

            val result = application.setCredentials(credentials).futureValue
            result shouldBe Done
          }
        }

        "but not when the platform differs" in {
          val argsGen =
            for {
              application ← SNSGen.platformApplication
              credentials ← SNSGen.platformApplicationCredentials.retryUntil(_.platform != application.platform)
            } yield (application, credentials)

          forAll(argsGen) { args ⇒
            val (application, credentials) = args
            implicit val sns = mock[StreamingSNSClient]("sns")

            an [IllegalArgumentException] shouldBe thrownBy (application.setCredentials(credentials))
          }
          }
        }
    }

    "can delete itself" in {
      forAll(SNSGen.platformApplication → "application") { application ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.platformApplicationDeleter _)
          .expects()
          .returningFlow(application.arn, application.arn)

        val result = application.delete().futureValue
        result shouldBe Done
      }
    }

    "create endpoints" - {
      val appAndEndpoint =
        for {
          application ← SNSGen.platformApplication
          endpoint ← SNSGen.platformEndpoint(application.arn)
        } yield (application, endpoint)
      val token = SNSGen.nonEmptyString
      val customUserData = Gen.alphaStr
      val attributes = Gen.mapOf(Gen.zip(SNSGen.nonEmptyString, SNSGen.nonEmptyString))

      "using only a token" in {
        forAll(appAndEndpoint → "appAndEndpoint", token → "token") { (appAndEndpoint, token) ⇒
          val (application, endpoint) = appAndEndpoint
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.platformEndpointCreator _)
            .expects()
            .returningFlow(CreatePlatformEndpointRequest(application.arn, token), endpoint.arn)
          (sns.platformEndpointAttributesGetter _)
            .expects()
            .returningFlow(endpoint.arn, endpoint.attributes)

          val result = application.createEndpoint(token).futureValue
          result shouldBe endpoint
        }
      }

      "using a token and custom user data" in {
        forAll(
          appAndEndpoint → "appAndEndpoint",
          token → "token",
          customUserData → "customUserData"
        ) { (appAndEndpoint, token, customUserData) ⇒
          val (application, endpoint) = appAndEndpoint
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.platformEndpointCreator _)
            .expects()
            .returningFlow(CreatePlatformEndpointRequest(application.arn, token, customUserData), endpoint.arn)
          (sns.platformEndpointAttributesGetter _)
            .expects()
            .returningFlow(endpoint.arn, endpoint.attributes)

          val result = application.createEndpoint(token, customUserData).futureValue
          result shouldBe endpoint
        }
      }

      "using a token and attributes" in {
        forAll(
          appAndEndpoint → "appAndEndpoint",
          token → "token",
          attributes → "attributes"
        ) { (appAndEndpoint, token, attributes) ⇒
          val (application, endpoint) = appAndEndpoint
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.platformEndpointCreator _)
            .expects()
            .returningFlow(CreatePlatformEndpointRequest(application.arn, token, attributes), endpoint.arn)
          (sns.platformEndpointAttributesGetter _)
            .expects()
            .returningFlow(endpoint.arn, endpoint.attributes)

          val result = application.createEndpoint(token, attributes).futureValue
          result shouldBe endpoint
        }
      }

      "using a token, attributes, and custom user data" in {
        forAll(
          appAndEndpoint → "appAndEndpoint",
          token → "token",
          customUserData → "customUserData",
          attributes → "attributes"
        ) { (appAndEndpoint, token, customUserData, attributes) ⇒
          val (application, endpoint) = appAndEndpoint
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.platformEndpointCreator _)
            .expects()
            .returningFlow(CreatePlatformEndpointRequest(application.arn, token, customUserData, attributes), endpoint.arn)
          (sns.platformEndpointAttributesGetter _)
            .expects()
            .returningFlow(endpoint.arn, endpoint.attributes)

          val result = application.createEndpoint(token, customUserData, attributes).futureValue
          result shouldBe endpoint
        }
      }
    }

    "list its endpoints" in {
      val argsGen =
        for {
          application ← SNSGen.platformApplication
          endpoints ← Gen.listOf(SNSGen.platformEndpoint(application.arn))
        } yield (application, endpoints)
      forAll(argsGen) { args ⇒
        val (application, endpoints) = args
        implicit val sns = mock[StreamingSNSClient]("aws")

        (sns.platformEndpointLister _)
          .expects()
          .returningConcatFlow(application.arn, endpoints)

        val result = application.listEndpoints().futureValue
        result shouldBe endpoints
      }
    }
  }
}
