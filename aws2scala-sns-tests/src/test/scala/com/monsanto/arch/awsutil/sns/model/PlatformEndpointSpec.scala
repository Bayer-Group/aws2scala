package com.monsanto.arch.awsutil.sns.model

import akka.Done
import com.monsanto.arch.awsutil.sns.StreamingSNSClient
import com.monsanto.arch.awsutil.sns.model.AwsConverters._
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Samplers.EnhancedGen
import com.monsanto.arch.awsutil.test_support.{AwsMockUtils, FlowMockUtils, Materialised}
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.{SnsGen, UtilGen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class PlatformEndpointSpec extends FreeSpec with MockFactory with Materialised with FlowMockUtils with AwsMockUtils {
  "a PlatformEndpoint" - {
    "can round-trip through its AWS equivalent" in {
      forAll { endpoint: PlatformEndpoint ⇒
        endpoint.asAws.asScala shouldBe endpoint
      }
    }

    "can get its" - {
      val arn = arbitrary[PlatformEndpointArn].reallySample.value
      val baseAttributes = Map("Enabled" → "true", "Token" → "aToken")

      "Enabled attribute" in {
        forAll { enabled: Boolean ⇒
          val attributes = baseAttributes.updated("Enabled", enabled.toString)
          val endpoint = PlatformEndpoint(arn, attributes)

          endpoint.enabled shouldBe enabled
        }
      }

      "Token attribute" in {
        forAll(UtilGen.nonEmptyString) { token ⇒
          val attributes = baseAttributes.updated("Token", token)
          val endpoint = PlatformEndpoint(arn, attributes)

          endpoint.token shouldBe token
        }
      }

      "CustomUserData attribute" in {
        forAll(Gen.option(UtilGen.nonEmptyString)) { data ⇒
          val attributes = data.map(baseAttributes.updated("CustomUserData", _)).getOrElse(baseAttributes)
          val endpoint = PlatformEndpoint(arn, attributes)

          endpoint.customUserData shouldBe data
        }
      }
    }

    "can refresh" in {
      forAll(
        arbitrary[PlatformEndpoint] → "endpoint",
        SnsGen.platformEndpointAttributes → "newAttributes"
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
      forAll { (endpoint: PlatformEndpoint, enabled: Boolean) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.platformEndpointAttributesSetter _)
          .expects()
          .returningFlow(SetPlatformEndpointAttributesRequest(endpoint.arn, "Enabled", enabled.toString), endpoint.arn)

        val result = endpoint.setEnabled(enabled).futureValue
        result shouldBe Done
      }
    }

    "can set the token attribute" in {
      forAll(arbitrary[PlatformEndpoint] → "endpoint", UtilGen.nonEmptyString → "token") { (endpoint, token) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.platformEndpointAttributesSetter _)
          .expects()
          .returningFlow(SetPlatformEndpointAttributesRequest(endpoint.arn, "Token", token), endpoint.arn)

        val result = endpoint.setToken(token).futureValue
        result shouldBe Done
      }
    }

    "can set the custom user data attribute" in {
      forAll(arbitrary[PlatformEndpoint] → "endpoint", UtilGen.nonEmptyString → "customUserData") { (endpoint, customUserData) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

        (sns.platformEndpointAttributesSetter _)
          .expects()
          .returningFlow(SetPlatformEndpointAttributesRequest(endpoint.arn, "CustomUserData", customUserData), endpoint.arn)

        val result = endpoint.setCustomUserData(customUserData).futureValue
        result shouldBe Done
      }
    }

    "can set arbitrary attributes using" - {
      val nameGen = Gen.alphaStr.suchThat(_.nonEmpty)
      val valueGen = Gen.listOf(arbitrary[Char]).map(_.mkString)
      "plain values" in {
        forAll(
          arbitrary[PlatformEndpoint] → "endpoint",
          nameGen → "name",
          valueGen → "value"
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
          arbitrary[PlatformEndpoint] → "endpoint",
          nameGen → "name",
          Gen.option(valueGen) → "value"
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
          arbitrary[PlatformEndpoint] → "endpoint",
          Gen.mapOf(Gen.zip(nameGen, valueGen)) → "newAttributes"
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
      forAll { endpoint: PlatformEndpoint ⇒
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
          arbitrary[PlatformEndpoint] → "endpoint",
          UtilGen.nonEmptyString → "message",
          SnsGen.messageId → "messageId"
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
          arbitrary[PlatformEndpoint] → "endpoint",
          UtilGen.nonEmptyString → "message",
          arbitrary[Map[String,MessageAttributeValue]] → "attributes",
          SnsGen.messageId → "messageId"
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
          arbitrary[PlatformEndpoint] → "endpoint",
          SnsGen.jsonMessagePayload → "jsonMessage",
          SnsGen.messageId → "messageId"
        ) { (endpoint, jsonMessage, messageId) ⇒
          val message = jsonMessage.toString
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.publisher _)
            .expects()
            .returningFlow(PublishRequest(endpoint, message), messageId)

          val result = endpoint.publishJson(message).futureValue
          result shouldBe messageId
        }
      }

      "a JSON message with attributes" in {
        forAll(
          arbitrary[PlatformEndpoint] → "endpoint",
          SnsGen.jsonMessagePayload → "jsonMessage",
          arbitrary[Map[String,MessageAttributeValue]] → "attributes",
          SnsGen.messageId → "messageId"
        ) { (endpoint, jsonMessage, attributes, messageId) ⇒
          val message = jsonMessage.toString
          implicit val sns = mock[StreamingSNSClient]("sns")

          (sns.publisher _)
            .expects()
            .returningFlow(PublishRequest(endpoint, message, attributes), messageId)

          val result = endpoint.publishJson(message, attributes).futureValue
          result shouldBe messageId
        }
      }
    }
  }
}
