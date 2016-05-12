package com.monsanto.arch.awsutil.sns.model

import akka.Done
import com.monsanto.arch.awsutil.identitymanagement.model.RoleArn
import com.monsanto.arch.awsutil.sns.StreamingSNSClient
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Samplers.arbitrarySample
import com.monsanto.arch.awsutil.test_support.{FlowMockUtils, Materialised}
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.{SnsGen, UtilGen}
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
      val args =
        for {
          arn ← arbitrary[PlatformApplicationArn]
          attrs ← SnsGen.platformApplicationAttributes(arn)
        } yield (arn, attrs)
      forAll(args) { case (arn, attributes) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.platformApplicationAttributesGetter _)
          .expects()
          .returningFlow(arn.arnString, attributes)

        val result = PlatformApplication(arn.arnString).futureValue

        result shouldBe PlatformApplication(arn.arnString, attributes)
      }
    }
  }

  "a PlatformApplication instance" - {
    "can get its" - {
      val arn = arbitrarySample[PlatformApplicationArn].arnString
      val baseMap = Map("Enabled" → "true")

      "Enabled attribute" in {
        forAll { enabled: Boolean ⇒
          val application = PlatformApplication(arn, Map("Enabled" → enabled.toString))

          application.enabled shouldBe enabled
        }
      }

      "EventEndpointCreated attribute" in {
        forAll { topicArn: Option[TopicArn] ⇒
          val attrs = topicArn.map(t ⇒ baseMap + ("EventEndpointCreated" → t.arnString)).getOrElse(baseMap)
          val application = PlatformApplication(arn, attrs)

          application.eventEndpointCreated shouldBe topicArn.map(_.arnString)
        }
      }

      "EventEndpointDeleted attribute" in {
        forAll { topicArn: Option[TopicArn] ⇒
          val attrs = topicArn.map(t ⇒ baseMap + ("EventEndpointDeleted" → t.arnString)).getOrElse(baseMap)
          val application = PlatformApplication(arn, attrs)

          application.eventEndpointDeleted shouldBe topicArn.map(_.arnString)
        }
      }

      "EventEndpointUpdated attribute" in {
        forAll { topicArn: Option[TopicArn] ⇒
          val attrs = topicArn.map(t ⇒ baseMap + ("EventEndpointUpdated" → t.arnString)).getOrElse(baseMap)
          val application = PlatformApplication(arn, attrs)

          application.eventEndpointUpdated shouldBe topicArn.map(_.arnString)
        }
      }

      "EventDeliveryFailure attribute" in {
        forAll { topicArn: Option[TopicArn] ⇒
          val attrs = topicArn.map(t ⇒ baseMap + ("EventDeliveryFailure" → t.arnString)).getOrElse(baseMap)
          val application = PlatformApplication(arn, attrs)

          application.eventDeliveryFailure shouldBe topicArn.map(_.arnString)
        }
      }

      "SuccessFeedbackRoleArn attribute" - {
        "when possibly missing" in {
          forAll { roleArn: Option[RoleArn] ⇒
            val attrs = roleArn.map(t ⇒ baseMap + ("SuccessFeedbackRoleArn" → t.arnString)).getOrElse(baseMap)
            val application = PlatformApplication(arn, attrs)

            application.successFeedbackRoleArn shouldBe roleArn.map(_.arnString)
          }
        }

        "when possibly empty" in {
          forAll { roleArn: Option[RoleArn] ⇒
            val attrs = baseMap + ("SuccessFeedbackRoleArn" → roleArn.map(_.arnString).getOrElse(""))
            val application = PlatformApplication(arn, attrs)

            application.successFeedbackRoleArn shouldBe roleArn.map(_.arnString)
          }
        }
      }

      "FailureFeedbackRoleArn attribute" - {
        "when possibly missing" in {
          forAll { roleArn: Option[RoleArn] ⇒
            val attrs = roleArn.map(t ⇒ baseMap + ("FailureFeedbackRoleArn" → t.arnString)).getOrElse(baseMap)
            val application = PlatformApplication(arn, attrs)

            application.failureFeedbackRoleArn shouldBe roleArn.map(_.arnString)
          }
        }

        "when possibly empty" in {
          forAll { roleArn: Option[RoleArn] ⇒
            val attrs = baseMap + ("FailureFeedbackRoleArn" → roleArn.map(_.arnString).getOrElse(""))
            val application = PlatformApplication(arn, attrs)

            application.failureFeedbackRoleArn shouldBe roleArn.map(_.arnString)
          }
        }
      }

      "SuccessFeedbackSampleRate attribute" in {
        val sampleRateGen: Gen[Option[Int]] = Gen.option(Gen.choose(0, 100))
        forAll(sampleRateGen) { sampleRate ⇒
          val attrs = sampleRate.map(r ⇒ baseMap + ("SuccessFeedbackSampleRate" → r.toString)).getOrElse(baseMap)
          val application = PlatformApplication(arn, attrs)

          application.successFeedbackSampleRate shouldBe sampleRate
        }
      }

      "name from the ARN" in {
        forAll { arn: PlatformApplicationArn ⇒
          val application = PlatformApplication(arn.arnString, baseMap)

          application.name shouldBe arn.name
        }
      }

      "platform from the ARN" in {
        forAll { arn: PlatformApplicationArn ⇒
          val application = PlatformApplication(arn.arnString, baseMap)

          application.platform shouldBe arn.platform
        }
      }
    }

    "provides a refresh method" in {
      val args =
        for {
          app ← arbitrary[PlatformApplication]
          attrs ← SnsGen.platformApplicationAttributes(PlatformApplicationArn(app.arn))
        } yield (app, attrs)
      forAll(args) { case (platformApplication, newAttributes) ⇒
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
        forAll { (application: PlatformApplication, enabled: Boolean) ⇒
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
        forAll { (application: PlatformApplication, maybeTopicArnObj: Option[TopicArn]) ⇒
          val maybeTopicArn = maybeTopicArnObj.map(_.arnString)
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
        forAll { (application: PlatformApplication, maybeTopicArnObj: Option[TopicArn]) ⇒
          val maybeTopicArn = maybeTopicArnObj.map(_.arnString)
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
        forAll { (application: PlatformApplication, maybeTopicArnObj: Option[TopicArn]) ⇒
          val maybeTopicArn = maybeTopicArnObj.map(_.arnString)
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
        forAll { (application: PlatformApplication, maybeTopicArnObj: Option[TopicArn]) ⇒
          val maybeTopicArn = maybeTopicArnObj.map(_.arnString)
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
        forAll { (application: PlatformApplication, maybeRoleArnObj: Option[RoleArn]) ⇒
          val maybeRoleArn = maybeRoleArnObj.map(_.arnString)
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
        forAll { (application: PlatformApplication, maybeRoleArnObj: Option[RoleArn]) ⇒
          val maybeRoleArn = maybeRoleArnObj.map(_.arnString)
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
          arbitrary[PlatformApplication] → "application",
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
              application ← arbitrary[PlatformApplication]
              credentials ← SnsGen.platformApplicationCredentials(application.platform)
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
              application ← arbitrary[PlatformApplication]
              credentials ← arbitrary[PlatformApplicationCredentials].suchThat(_.platform != application.platform)
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
      forAll { application: PlatformApplication ⇒
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
          application ← arbitrary[PlatformApplication]
          endpoint ← endpointForPlatformApplication(application)
        } yield (application, endpoint)
      val token = UtilGen.nonEmptyString
      val customUserData = arbitrary[String]
      val attributes = SnsGen.platformEndpointAttributes

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
          application ← arbitrary[PlatformApplication]
          endpoints ← Gen.listOf(endpointForPlatformApplication(application))
        } yield (application, endpoints)
      forAll(argsGen) { case (application, endpoints) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.platformEndpointLister _)
          .expects()
          .returningConcatFlow(application.arn, endpoints)

        val result = application.listEndpoints().futureValue
        result shouldBe endpoints
      }
    }
  }

  private def endpointForPlatformApplication(application: PlatformApplication): Gen[PlatformEndpoint] = {
    val appArn = PlatformApplicationArn(application.arn)
    for {
      endpointId ← SnsGen.endpointId
      attributes ← SnsGen.platformEndpointAttributes
    } yield {
      val endpointArn = PlatformEndpointArn(appArn.account, appArn.region, appArn.platform, appArn.name, endpointId)
      PlatformEndpoint(endpointArn.arnString, attributes)
    }
  }
}
