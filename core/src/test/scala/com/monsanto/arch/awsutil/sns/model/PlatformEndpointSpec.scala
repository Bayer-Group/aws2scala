package com.monsanto.arch.awsutil.sns.model

import akka.Done
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sns.{AmazonSNSAsync, model ⇒ am}
import com.monsanto.arch.awsutil.sns.{DefaultStreamingSNSClient, StreamingSNSClient}
import com.monsanto.arch.awsutil.test.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.{AwsMockUtils, FlowMockUtils, Materialised}
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import spray.json.{JsObject, JsString, JsonParser}

import scala.collection.JavaConverters._

class PlatformEndpointSpec extends FreeSpec with MockFactory with Materialised with FlowMockUtils with AwsMockUtils {
  "the PlatformEndpoint companion object" - {
    "builds PlatformEndpoint instances from" - {
      "an ARN and an attribute map" in {
        val endpointPlatformAndArn =
          for {
            platform ← SNSGen.platform
            appName ← SNSGen.applicationName
            appArn ← SNSGen.platformApplicationArn(platform, appName)
            endpointArn ← SNSGen.platformEndpointArn(appArn)
          } yield  (platform, endpointArn)
        forAll(
          endpointPlatformAndArn → "endpointPlatformAndArn",
          SNSGen.nonEmptyString → "token",
          SNSGen.boolean → "enabled",
          Gen.option(Gen.alphaStr) → "customUserData"
        ) { (endpointPlatformAndArn, token, enabled, customUserData) ⇒
          val (platform, arn) = endpointPlatformAndArn
          val attributes =
            customUserData.map(x ⇒ Map("CustomUserData" → x)).getOrElse(Map.empty) ++
              Map(
                "Token" → token,
                "Enabled" → enabled.toString)

          val result = PlatformEndpoint(arn, attributes)
          result.arn shouldBe arn
          result.attributes shouldBe attributes
          result.token shouldBe token
          result.enabled shouldBe enabled
          result.customUserData shouldBe customUserData
          result.platform shouldBe platform
        }
      }

      "an AWS object" in {
        forAll(
          SNSGen.platformEndpointArn → "arn",
          SNSGen.platformEndpointAttributes → "attributes"
        ) { (arn, attributes) ⇒
          val endpoint = new am.Endpoint()
            .withEndpointArn(arn)
            .withAttributes(attributes.asJava)
          val expected = PlatformEndpoint(arn, attributes)

          val result = PlatformEndpoint(endpoint)
          result shouldBe expected
        }
      }
    }
  }

  "a PlatformEndpoint" - {
    "can refresh" in {
      forAll(
        SNSGen.platformEndpoint → "endpoint",
        SNSGen.platformEndpointAttributes → "newAttributes"
      ) { (endpoint, newAttributes) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.platformEndpointAttributesGetter _)
          .expects()
          .returningFlow(endpoint.arn, newAttributes)

        val result = endpoint.refresh().futureValue
        result shouldBe PlatformEndpoint(endpoint.arn, newAttributes)
      }
    }

    "can set the enabled attribute" in {
      forAll(SNSGen.platformEndpoint → "endpoint", SNSGen.boolean → "enabled") { (endpoint, enabled) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.platformEndpointAttributesSetter _)
          .expects()
          .returningFlow(SetPlatformEndpointAttributesRequest(endpoint.arn, "Enabled", enabled.toString), endpoint.arn)

        val result = endpoint.setEnabled(enabled).futureValue
        result shouldBe Done
      }
    }

    "can set the token attribute" in {
      forAll(SNSGen.platformEndpoint → "endpoint", SNSGen.nonEmptyString → "token") { (endpoint, token) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.platformEndpointAttributesSetter _)
          .expects()
          .returningFlow(SetPlatformEndpointAttributesRequest(endpoint.arn, "Token", token), endpoint.arn)

        val result = endpoint.setToken(token).futureValue
        result shouldBe Done
      }
    }

    "can set the custom user data attribute" in {
      forAll(SNSGen.platformEndpoint → "endpoint", Gen.alphaStr → "customUserData") { (endpoint, customUserData) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.platformEndpointAttributesSetter _)
          .expects()
          .returningFlow(SetPlatformEndpointAttributesRequest(endpoint.arn, "CustomUserData", customUserData), endpoint.arn)

        val result = endpoint.setCustomUserData(customUserData).futureValue
        result shouldBe Done
      }
    }

    "can set arbitrary attributes using" - {
      "plain values" in {
        forAll(
          SNSGen.platformEndpoint → "endpoint",
          SNSGen.nonEmptyString → "name",
          Gen.alphaStr → "value"
        ) { (endpoint, name, value) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.platformEndpointAttributesSetter _)
            .expects()
            .returningFlow(SetPlatformEndpointAttributesRequest(endpoint.arn, name, value), endpoint.arn)

          val result = endpoint.setAttribute(name, value).futureValue
          result shouldBe Done
        }
      }

      "option values" in {
        forAll(
          SNSGen.platformEndpoint → "endpoint",
          SNSGen.nonEmptyString → "name",
          Gen.option(Gen.alphaStr) → "value"
        ) { (endpoint, name, value) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.platformEndpointAttributesSetter _)
            .expects()
            .returningFlow(SetPlatformEndpointAttributesRequest(endpoint.arn, name, value), endpoint.arn)

          val result = endpoint.setAttribute(name, value).futureValue
          result shouldBe Done
        }
      }

      "a map" in {
        forAll(
          SNSGen.platformEndpoint → "endpoint",
          SNSGen.platformEndpointAttributes → "newAttributes"
        ) { (endpoint, newAttributes) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.platformEndpointAttributesSetter _)
            .expects()
            .returningFlow(SetPlatformEndpointAttributesRequest(endpoint.arn, newAttributes), endpoint.arn)

          val result = endpoint.setAttributes(newAttributes).futureValue
          result shouldBe Done
        }
      }
    }

    "delete itself" in {
      forAll(SNSGen.platformEndpoint → "endpoint") { endpoint ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.platformEndpointDeleter _)
          .expects()
          .returningFlow(endpoint.arn, endpoint.arn)

        val result = endpoint.delete().futureValue
        result shouldBe Done
      }
    }

    "can publish" - {
      "a plain message" in {
        forAll(
          SNSGen.platformEndpoint → "endpoint",
          SNSGen.nonEmptyString → "message",
          SNSGen.messageId → "messageId"
        ) { (endpoint, message, messageId) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.publisher _)
            .expects()
            .returningFlow(PublishRequest(endpoint.arn, message), messageId)

          val result = endpoint.publish(message).futureValue
          result shouldBe messageId
        }
      }

      "a plain message with attributes" in {
        forAll(
          SNSGen.platformEndpoint → "endpoint",
          SNSGen.nonEmptyString → "message",
          SNSGen.messageAttributes → "attributes",
          SNSGen.messageId → "messageId"
        ) { (endpoint, message, attributes, messageId) ⇒
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.publisher _)
            .expects()
            .returningFlow(PublishRequest(endpoint.arn, message, attributes), messageId)

          val result = endpoint.publish(message, attributes).futureValue
          result shouldBe messageId
        }
      }

      "a JSON message" in {
        forAll(
          SNSGen.platformEndpoint → "endpoint",
          SNSGen.jsonMessagePayload → "message",
          SNSGen.messageId → "messageId"
        ) { (endpoint, message, messageId) ⇒
          val aws = mock[AmazonSNSAsync]("aws")
          implicit val streaming = new DefaultStreamingSNSClient(aws)
          val jsMessage = JsObject(endpoint.platform.name → JsString(message))

          (aws.publishAsync(_: am.PublishRequest, _: AsyncHandler[am.PublishRequest,am.PublishResult]))
            .expects(whereRequest { r ⇒
              r.getTopicArn shouldBe null
              r.getTargetArn shouldBe endpoint.arn
              JsonParser(r.getMessage) shouldBe jsMessage
              r.getMessageStructure shouldBe "json"
              r.getSubject shouldBe null
              r.getMessageAttributes.asScala shouldBe empty
              true
            })
            .withAwsSuccess(new am.PublishResult().withMessageId(messageId))

          val result = endpoint.publishJson(message).futureValue
          result shouldBe messageId
        }
      }

      "a JSON message with attributes" in {
        forAll(
          SNSGen.platformEndpoint → "endpoint",
          SNSGen.jsonMessagePayload → "message",
          SNSGen.messageAttributes → "attributes",
          SNSGen.messageId → "messageId"
        ) { (endpoint, message, attributes, messageId) ⇒
          val aws = mock[AmazonSNSAsync]("aws")
          implicit val streaming = new DefaultStreamingSNSClient(aws)
          val jsMessage = JsObject(endpoint.platform.name → JsString(message))

          (aws.publishAsync(_: am.PublishRequest, _: AsyncHandler[am.PublishRequest,am.PublishResult]))
            .expects(whereRequest { r ⇒
              r.getTopicArn shouldBe null
              r.getTargetArn shouldBe endpoint.arn
              JsonParser(r.getMessage) shouldBe jsMessage
              r.getMessageStructure shouldBe "json"
              r.getSubject shouldBe null
              r.getMessageAttributes.asScala shouldBe attributes.mapValues(_.toAws)
              true
            })
            .withAwsSuccess(new am.PublishResult().withMessageId(messageId))

          val result = endpoint.publishJson(message, attributes).futureValue
          result shouldBe messageId
        }
      }
    }
  }
}
