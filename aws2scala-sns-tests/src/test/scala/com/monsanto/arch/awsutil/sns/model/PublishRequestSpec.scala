package com.monsanto.arch.awsutil.sns.model

import com.amazonaws.util.json.JSONObject
import com.monsanto.arch.awsutil.sns.model.AwsConverters._
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.{SnsGen, UtilGen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class PublishRequestSpec extends FreeSpec {
  "a PublishRequest should" - {
    "convert to the correct AWS object" in {
      forAll { request: PublishRequest ⇒
        request.asAws should have (
          'Message (request.message),
          'MessageAttributes (request.attributes.asAws),
          'MessageStructure (request.messageStructure.orNull),
          'Subject (request.subject.orNull),
          'TargetArn (request.targetArn),
          'TopicArn (null)
        )
      }
    }

    "be created from" - {
      implicit class Map2JsonString(messageMap: Map[String,String]) {
        def asJson: String = {
          val jsonMessage = new JSONObject()
          messageMap.foreach(entry ⇒ jsonMessage.put(entry._1, entry._2))
          jsonMessage.toString
        }
      }

      "a target ARN and a message" in {
        forAll(
          SnsGen.targetArn → "targetArn",
          UtilGen.nonEmptyString → "message"
        ) { (targetArn, message) ⇒
          PublishRequest(targetArn.arnString, message) shouldBe
            PublishRequest(targetArn.arnString, message, None, None, Map.empty)
        }
      }

      "a target ARN, a message, and a subject" in {
        forAll(
          SnsGen.targetArn → "targetArn",
          UtilGen.nonEmptyString → "message",
          UtilGen.nonEmptyString → "subject"
        ) { (targetArn, message, subject) ⇒
          PublishRequest(targetArn.arnString, message, subject) shouldBe
            PublishRequest(targetArn.arnString, message, Some(subject), None, Map.empty)
        }
      }

      "a target ARN, a message, and some attributes" in {
        forAll(
          SnsGen.targetArn → "targetArn",
          UtilGen.nonEmptyString → "message",
          arbitrary[Map[String,MessageAttributeValue]] → "attributes"
        ) { (targetArn, message, attributes) ⇒
          PublishRequest(targetArn.arnString, message, attributes) shouldBe
            PublishRequest(targetArn.arnString, message, None, None, attributes)
        }
      }

      "a target ARN, a message, a subject, and some attributes" in {
        forAll(
          SnsGen.targetArn → "targetArn",
          UtilGen.nonEmptyString → "message",
          UtilGen.nonEmptyString → "subject",
          arbitrary[Map[String,MessageAttributeValue]] → "attributes"
        ) { (targetArn, message, subject, attributes) ⇒
          PublishRequest(targetArn.arnString, message, subject, attributes) shouldBe
            PublishRequest(targetArn.arnString, message, Some(subject), None, attributes)
        }
      }

      "a target ARN and a message map" in {
        forAll(
          SnsGen.targetArn → "targetArn",
          SnsGen.messageMap → "messageMap"
        ) { (targetArn, messageMap) ⇒
          PublishRequest(targetArn.arnString, messageMap) shouldBe
            PublishRequest(targetArn.arnString, messageMap.asJson, None, Some("json"), Map.empty)
        }
      }

      "a target ARN, a message map, and a subject" in {
        forAll(
          SnsGen.targetArn → "targetArn",
          SnsGen.messageMap → "messageMap",
          UtilGen.nonEmptyString → "subject"
        ) { (targetArn, messageMap, subject) ⇒
          PublishRequest(targetArn.arnString, messageMap, subject) shouldBe
            PublishRequest(targetArn.arnString, messageMap.asJson, Some(subject), Some("json"), Map.empty)
        }
      }

      "a target ARN, a message map, and an attribute map" in {
        forAll(
          SnsGen.targetArn → "targetArn",
          SnsGen.messageMap → "messageMap",
          arbitrary[Map[String,MessageAttributeValue]] → "attributes"
        ) { (targetArn, messageMap, attributes) ⇒
          PublishRequest(targetArn.arnString, messageMap, attributes) shouldBe
            PublishRequest(targetArn.arnString, messageMap.asJson, None, Some("json"), attributes)
        }
      }

      "a target ARN, a message map, a subject, and an attribute map" in {
        forAll(
          SnsGen.targetArn → "targetArn",
          SnsGen.messageMap → "messageMap",
          UtilGen.nonEmptyString → "subject",
          arbitrary[Map[String,MessageAttributeValue]] → "attributes"
        ) { (targetArn, messageMap, subject, attributes) ⇒
          PublishRequest(targetArn.arnString, messageMap, subject, attributes) shouldBe
            PublishRequest(targetArn.arnString, messageMap.asJson, Some(subject), Some("json"), attributes)
        }
      }

      "a platform endpoint and a JSON message" in {
        forAll(
          arbitrary[PlatformEndpoint] → "endpoint",
          SnsGen.jsonMessagePayload → "jsonMessage"
        ) { (endpoint, jsonMessage) ⇒
          PublishRequest(endpoint, jsonMessage.toString()) shouldBe
            PublishRequest(endpoint.arn, Map(endpoint.platform.name → jsonMessage.toString()).asJson, None, Some("json"), Map.empty)
        }
      }

      "a platform endpoint, a JSON message, and some attributes" in {
        forAll(
          arbitrary[PlatformEndpoint] → "endpoint",
          SnsGen.jsonMessagePayload → "jsonMessage",
          arbitrary[Map[String,MessageAttributeValue]] → "attributes"
        ) { (endpoint, jsonMessage, attributes) ⇒
          PublishRequest(endpoint, jsonMessage.toString(), attributes) shouldBe
              PublishRequest(endpoint.arn, Map(endpoint.platform.name → jsonMessage.toString()).asJson, None, Some("json"), attributes)
        }
      }
    }
  }
}
