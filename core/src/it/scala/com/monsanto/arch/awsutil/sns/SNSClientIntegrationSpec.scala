package com.monsanto.arch.awsutil.sns

import akka.Done
import com.amazonaws.services.sns.model.EndpointDisabledException
import com.amazonaws.services.sqs.AmazonSQSClient
import com.monsanto.arch.awsutil.auth.policy.action.SNSAction
import com.monsanto.arch.awsutil.sns.model.Protocol.{Http, SQS}
import com.monsanto.arch.awsutil.sns.model._
import com.monsanto.arch.awsutil.test.AwsScalaFutures._
import com.monsanto.arch.awsutil.test.{AwsIntegrationSpec, IntegrationCleanup, IntegrationTest}
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually._
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import spray.json.{JsArray, JsObject, JsString, JsonParser}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

@IntegrationTest
class SNSClientIntegrationSpec extends FreeSpec with AwsIntegrationSpec with StrictLogging with IntegrationCleanup {
  // TODO: get credentials from main source
  private val sqs = new AmazonSQSClient()
  private implicit val streaming = streamingAwsClient.sns
  private implicit val async = asyncAwsClient.sns

  private val testPrefix = "aws2scala-it-sns"
  private val creationMillis = System.currentTimeMillis()

  private var sqsQueueUrl: String = _
  private var sqsQueueArn: String = _

  private val testName = s"$testPrefix-$creationMillis-$testId"
  private var testTopic: Topic = _
  private var testSubscription: Subscription = _
  private var testPlatformApplication: PlatformApplication = _
  private var testPlatformEndpoint: PlatformEndpoint = _

  "the default SNS client" - {
    "can create a topic" in {
      logger.info(s"Creating topic with name $testName")
      val result = SNS.createTopic(testName).futureValue
      result.name shouldBe testName

      testTopic = result

      logger.info(s"Created $testTopic")
    }

    "can list topics" in {
      val result = SNS.listTopics().futureValue
      result should contain (testTopic)
    }

    "can add a permission" in {
      extractStatementIds(testTopic.policy) should not contain "test"

      val result = testTopic.addPermission("test", Seq(testTopic.owner), Seq(SNSAction.GetTopicAttributes)).futureValue
      result shouldBe Done

      val updatedTopic = testTopic.refresh().futureValue
      extractStatementIds(updatedTopic.policy) should contain ("test")

      testTopic = updatedTopic
    }

    "can remove a permission" in {
      extractStatementIds(testTopic.policy) should contain ("test")

      val result = testTopic.removePermission("test").futureValue
      result shouldBe Done

      val updatedTopic = testTopic.refresh().futureValue
      extractStatementIds(updatedTopic.policy) should not contain "test"

      testTopic = updatedTopic
    }

    "can subscribe to the topic with an endpoint" - {
      "requiring confirmation" in {
        val result = async.subscribe(testTopic.arn, "http", "http://example.com").futureValue
        result shouldBe None
      }

      "that is immediately available" in {
        logger.info("Retrieving SQS queue ARN")
        sqsQueueArn = sqs.getQueueAttributes(sqsQueueUrl, List("QueueArn").asJava).getAttributes.get("QueueArn")
        logger.info(s"Queue ARN is $sqsQueueArn")

        logger.info("Setting policy to allow SNS to post to the queue")
        val policy =
          JsObject(
            "Version" → JsString("2012-10-17"),
            "Statement" → JsArray(
              JsObject(
                "Sid" → JsString(s"$testPrefix-$testId-policy"),
                "Effect" → JsString("Allow"),
                "Principal" → JsString("*"),
                "Action" → JsString("sqs:SendMessage"),
                "Resource" → JsString(sqsQueueArn),
                "Condition" → JsObject(
                  "ArnEquals" → JsObject("aws:SourceArn" → JsString(testTopic.arn))
                )
              )
            )
          ).compactPrint
        sqs.setQueueAttributes(sqsQueueUrl, Map("Policy" → policy).asJava)
        logger.info("Policy set, proceeding with subscription test")

        val result = async.subscribe(testTopic.arn, "sqs", sqsQueueArn).futureValue
        result shouldBe defined

        testSubscription = Subscription(result.get).futureValue

        logger.info(s"Subscription ARN is ${testSubscription.arn}")
      }
    }

    "can find the subscriptions in a listing of" - {
      lazy val httpSubscription = SubscriptionSummary(None, testTopic.arn, Http("http://example.com"), testTopic.owner)
      lazy val sqsSubscription = SubscriptionSummary(Some(testSubscription.arn), testTopic.arn, SQS(sqsQueueArn),
        testTopic.owner)

      "all subscriptions" in {
        val result = async.listSubscriptions().futureValue
        result should contain (httpSubscription)
        result should contain (sqsSubscription)
      }

      "subscriptions to the test topic" in {
        val result = async.listSubscriptions(testTopic.arn).futureValue
        result should have size 2
        result should contain (httpSubscription)
        result should contain (sqsSubscription)
      }
    }

    "can get a subscription’s attributes" in {
      val result = async.getSubscriptionAttributes(testSubscription.arn).futureValue
      result shouldBe testSubscription.attributes
    }

    "can set a subscription attribute" in {
      testSubscription.rawMessageDelivery shouldBe false

      val result = async.setSubscriptionAttribute(testSubscription.arn, "RawMessageDelivery", "true").futureValue
      result shouldBe Done
    }

    "can verify the subscription attribute was set" in {
      val result = async.getSubscriptionAttribute(testSubscription.arn, "RawMessageDelivery").futureValue
      result shouldBe Some("true")
    }

    "can publish to a topic" in {
      val result = async.publish(testTopic.arn, "This is a message").futureValue
      result should not be empty
    }

    "can create a platform application" in {
      testPlatformApplication = SNS.createPlatformApplication(testName, Platform.MPNS()).futureValue
      logger.info(s"Created platform application with ARN: ${testPlatformApplication.arn}")

      testPlatformApplication.name shouldBe testName
      testPlatformApplication.platform shouldBe Platform.MPNS
    }

    "can find the platform in a listing" in {
      val result = async.listPlatformApplications().futureValue
      result should contain (testPlatformApplication)
    }

    "can set attributes on the platform application" - {
      lazy val applicationArn = testPlatformApplication.arn
      lazy val eventTopicArn = Some(testTopic.arn)

      "the endpoint created event topic" in {
        val result = testPlatformApplication.setEventEndpointCreated(eventTopicArn).futureValue
        result shouldBe Done

        eventually(Timeout(1.second), Interval(200.milliseconds)) {
          val topic = async.getPlatformApplicationAttribute(applicationArn, "EventEndpointCreated").futureValue
          topic shouldBe eventTopicArn
        }
      }

      "the endpoint updated event topic" in {
        val result = testPlatformApplication.setEventEndpointUpdated(eventTopicArn).futureValue
        result shouldBe Done

        eventually(Timeout(1.second), Interval(200.milliseconds)) {
          val topic = async.getPlatformApplicationAttribute(applicationArn, "EventEndpointUpdated").futureValue
          topic shouldBe eventTopicArn
        }
      }

      "the endpoint deleted event topic" in {
        val result = testPlatformApplication.setEventEndpointDeleted(eventTopicArn).futureValue
        result shouldBe Done

        eventually(Timeout(1.second), Interval(200.milliseconds)) {
          val topic = async.getPlatformApplicationAttribute(applicationArn, "EventEndpointDeleted").futureValue
          topic shouldBe eventTopicArn
        }
      }
    }

    "can create a platform endpoint" in {
      val result = testPlatformApplication.createEndpoint("http://example.com", testName).futureValue
      result.arn should include (testName)
      result.enabled shouldBe true
      result.customUserData shouldBe Some(testName)

      logger.info(s"Created new platform endpoint at ${result.arn}")

      testPlatformEndpoint = result
    }

    "list the platform endpoints for the application" in {
      val result = async.listPlatformEndpoints(testPlatformApplication.arn).futureValue
      result should contain (testPlatformEndpoint)
    }

    "modify the platform endpoint’s attributes" in {
      //noinspection NameBooleanParameters
      val result = testPlatformEndpoint.setEnabled(false).futureValue
      result shouldBe Done

      eventually(Timeout(1.second), Interval(200.milliseconds)) {
        val enabled = async.getPlatformApplicationAttribute(testPlatformApplication.arn, "Enabled").futureValue
        enabled shouldBe Some("true")
      }
    }

    "can fail to publish to the platform endpoint" in {
      val eventualFailure = async.publish(testPlatformEndpoint.arn, "This is a message")
      an [EndpointDisabledException] shouldBe thrownBy {
        Await.result(eventualFailure, 5.seconds)
      }
    }

    "can delete the platform endpoint" in {
      val result = testPlatformEndpoint.delete().futureValue
      result shouldBe Done

      eventually(Timeout(5.seconds), Interval(200.milliseconds)) {
        val result = async.listPlatformEndpoints(testPlatformApplication.arn).futureValue
        result shouldBe empty
      }

      logger.info(s"Removed platform endpoint at ${testPlatformEndpoint.arn}")
    }

    "can delete the platform application" in {
      val result = async.deletePlatformApplication(testPlatformApplication.arn).futureValue
      result shouldBe Done

      logger.info(s"Deleted platform application with ARN: ${testPlatformApplication.arn}")
    }

    "unsubscribe" in {
      val deleteResult = async.unsubscribe(testSubscription.arn).futureValue
      deleteResult shouldBe Done

      val listResult = async.listSubscriptions(testTopic.arn).futureValue
      listResult should have size 1
    }

    "can delete the topic" in {
      logger.info(s"Removing topic ${testTopic.name} (${testTopic.arn})")
      val result = testTopic.delete().futureValue
      result shouldBe Done
      logger.info(s"Removed topic ${testTopic.name}")
    }

    behave like cleanupSNSPlatformApplications(testPrefix)
    behave like cleanupSNSSubscriptions(testPrefix)
    behave like cleanupSNSTopics(testPrefix)
    behave like cleanupSQSQueues(testPrefix)
  }

  private def extractStatementIds(policy: String): Set[String] =
    JsonParser(policy).asJsObject
      .fields("Statement").asInstanceOf[JsArray]
      .elements.map(_.asJsObject.fields("Sid").asInstanceOf[JsString].value)
      .toSet

  override protected def beforeAll() = {
    super.beforeAll()
    logger.info("Creating test SQS queue")
    sqsQueueUrl = sqs.createQueue(s"$testPrefix-$testId").getQueueUrl
    logger.info(s"Created queue with URl $sqsQueueUrl")
  }

  override protected def afterAll() = {
    try {
      logger.info(s"Deleting SQS queue with URL $sqsQueueUrl")
      sqs.deleteQueue(sqsQueueUrl)
      sqs.shutdown()
    } finally super.afterAll()
  }
}
