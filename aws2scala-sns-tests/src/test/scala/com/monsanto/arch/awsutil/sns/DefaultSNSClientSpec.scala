package com.monsanto.arch.awsutil.sns

import akka.Done
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sns.{AmazonSNSAsync, model ⇒ aws}
import com.monsanto.arch.awsutil.sns.model.AwsConverters._
import com.monsanto.arch.awsutil.sns.model._
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.{AwsMockUtils, Materialised}
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.{SnsGen, UtilGen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import spray.json.{JsObject, JsString, JsonParser}

import scala.collection.JavaConverters._

class DefaultSNSClientSpec extends FreeSpec with MockFactory with AwsMockUtils with Materialised {
  private implicit val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 50, maxSize = 50)

  "the default SNS client can" - {
    "create a topic" in {
      forAll { topicArn: TopicArn ⇒
        val name = topicArn.name
        withFixture { f ⇒
          (f.sns.createTopicAsync(_: aws.CreateTopicRequest, _: AsyncHandler[aws.CreateTopicRequest,aws.CreateTopicResult]))
            .expects(whereRequest(_.getName == name))
            .withAwsSuccess(new aws.CreateTopicResult().withTopicArn(topicArn.arnString))

          val result = f.async.createTopic(name).futureValue
          result shouldBe topicArn.arnString
        }
      }
    }

    "list the topics" in {
      forAll { topics: List[TopicArn] ⇒
        withFixture { f ⇒
          val listings = if (topics.isEmpty) List(topics) else topics.grouped(5).toList
          listings.zipWithIndex.foreach { case (listing, i) ⇒
            (f.sns.listTopicsAsync(_: aws.ListTopicsRequest, _: AsyncHandler[aws.ListTopicsRequest,aws.ListTopicsResult]))
              .expects(whereRequest { r ⇒
                Option(r.getNextToken) match {
                  case None        ⇒ i == 0
                  case Some(token) ⇒ token == i.toString
                }
              })
              .withAwsSuccess {
                val result = new aws.ListTopicsResult
                if (i + 1 != listings.size) {
                  result.setNextToken((i + 1).toString)
                }
                result.withTopics(listing.map(arn ⇒ new aws.Topic().withTopicArn(arn.arnString)).asJavaCollection)
              }
          }

          val result = f.async.listTopics().futureValue
          result shouldBe topics.map(_.arnString)
        }
      }
    }

    "delete a topic" in {
      forAll { topicArn: TopicArn ⇒
        withFixture { f ⇒
          (f.sns.deleteTopicAsync(_: aws.DeleteTopicRequest, _: AsyncHandler[aws.DeleteTopicRequest,aws.DeleteTopicResult]))
            .expects(whereRequest(_.getTopicArn == topicArn.arnString))
            .withVoidAwsSuccess()

          val result = f.async.deleteTopic(topicArn.arnString).futureValue
          result shouldBe Done
        }
      }
    }

    "get all of a topic’s attributes" in {
      forAll { topic: Topic ⇒
        withFixture { f ⇒
          (f.sns.getTopicAttributesAsync(_: aws.GetTopicAttributesRequest, _: AsyncHandler[aws.GetTopicAttributesRequest,aws.GetTopicAttributesResult]))
            .expects(whereRequest(_.getTopicArn == topic.arn))
            .withAwsSuccess(new aws.GetTopicAttributesResult().withAttributes(topic.attributes.asJava))

          val result = f.async.getTopicAttributes(topic.arn).futureValue
          result shouldBe topic.attributes
        }
      }
    }

    "get a specific attribute from a topic" in {
      val topicAttribute = Gen.oneOf("DisplayName", "Policy", "DeliveryPolicy", "EffectiveDeliveryPolicy", "Missing")
      forAll(arbitrary[Topic] → "topic", topicAttribute → "attributeName") { (topic, attributeName) ⇒
        withFixture { f ⇒
          (f.sns.getTopicAttributesAsync(_: aws.GetTopicAttributesRequest, _: AsyncHandler[aws.GetTopicAttributesRequest,aws.GetTopicAttributesResult]))
            .expects(whereRequest(_.getTopicArn == topic.arn))
            .withAwsSuccess(new aws.GetTopicAttributesResult().withAttributes(topic.attributes.asJava))

          val result = f.async.getTopicAttribute(topic.arn, attributeName).futureValue
          result shouldBe topic.attributes.get(attributeName)
        }
      }
    }

    "set a topic attribute" - {
      "using a plain value" in {
        forAll { (topicArn: TopicArn, maybeDeliveryPolicy: Option[TopicDeliveryPolicy]) ⇒
          val deliveryPolicy = maybeDeliveryPolicy.map(_.toString).orNull
          withFixture { f ⇒
            (f.sns.setTopicAttributesAsync(_: aws.SetTopicAttributesRequest, _: AsyncHandler[aws.SetTopicAttributesRequest,aws.SetTopicAttributesResult]))
              .expects(whereRequest(r ⇒
                r.getTopicArn == topicArn.arnString &&
                  r.getAttributeName == "DeliveryPolicy" &&
                  r.getAttributeValue == deliveryPolicy
              ))
              .withVoidAwsSuccess()

            val result = f.async.setTopicAttribute(topicArn.arnString, "DeliveryPolicy", deliveryPolicy).futureValue
            result shouldBe Done
          }
        }
      }

      "using an option" in {
        forAll { (topicArn: TopicArn, maybeDeliveryPolicyObj: Option[TopicDeliveryPolicy]) ⇒
          val maybeDeliveryPolicy = maybeDeliveryPolicyObj.map(_.toString)
          withFixture { f ⇒
            (f.sns.setTopicAttributesAsync(_: aws.SetTopicAttributesRequest, _: AsyncHandler[aws.SetTopicAttributesRequest,aws.SetTopicAttributesResult]))
              .expects(whereRequest(r ⇒
                r.getTopicArn == topicArn.arnString &&
                  r.getAttributeName == "DeliveryPolicy" &&
                  r.getAttributeValue == maybeDeliveryPolicy.orNull
              ))
              .withVoidAwsSuccess()

            val result = f.async.setTopicAttribute(topicArn.arnString, "DeliveryPolicy", maybeDeliveryPolicy).futureValue
            result shouldBe Done
          }
        }
      }
    }

    "add a permission" in {
      forAll { request: AddPermissionRequest ⇒
        withFixture { f ⇒
          (f.sns.addPermissionAsync(_: aws.AddPermissionRequest, _: AsyncHandler[aws.AddPermissionRequest,aws.AddPermissionResult]))
            .expects(whereRequest(_ == request.asAws))
            .withVoidAwsSuccess()

          val result = f.async.addPermission(request.topicArn, request.label, request.accounts, request.actions).futureValue
          result shouldBe Done
        }
      }
    }

    "remove a permission" in {
      forAll { request: RemovePermissionRequest ⇒
        withFixture { f ⇒
          (f.sns.removePermissionAsync(_: aws.RemovePermissionRequest, _: AsyncHandler[aws.RemovePermissionRequest,aws.RemovePermissionResult]))
            .expects(whereRequest(_ == request.asAws))
            .withVoidAwsSuccess()

          val result = f.async.removePermission(request.topicArn, request.label).futureValue
          result shouldBe Done
        }
      }
    }

    "list subscriptions" - {
      "all of them" in {
        forAll(maxSize(20)) { subscriptionSummaries: List[SubscriptionSummary] ⇒
          withFixture { f ⇒
            val listings = if (subscriptionSummaries.isEmpty) List(subscriptionSummaries) else subscriptionSummaries.grouped(5).toList
            listings.zipWithIndex.foreach { case (listing, i) ⇒
              (f.sns.listSubscriptionsAsync(_: aws.ListSubscriptionsRequest, _: AsyncHandler[aws.ListSubscriptionsRequest,aws.ListSubscriptionsResult]))
                .expects(whereRequest { r ⇒
                  Option(r.getNextToken) match {
                    case None        ⇒ i == 0
                    case Some(token) ⇒ token == i.toString
                  }
                })
                .withAwsSuccess {
                  val result = new aws.ListSubscriptionsResult
                  if (i + 1 != listings.size) {
                    result.setNextToken((i + 1).toString)
                  }
                  result.withSubscriptions(listing.map(_.asAws).asJavaCollection)
                }
            }

            val result = f.async.listSubscriptions().futureValue
            result shouldBe subscriptionSummaries
          }
        }
      }

      "by topic" in {
        forAll(maxSize(20)) { (topicArn: TopicArn, subscriptionSummaries: List[SubscriptionSummary]) ⇒
          withFixture { f ⇒
            val listings = if (subscriptionSummaries.isEmpty) List(subscriptionSummaries) else subscriptionSummaries.grouped(5).toList
            listings.zipWithIndex.foreach { case (listing, i) ⇒
              (f.sns.listSubscriptionsByTopicAsync(_: aws.ListSubscriptionsByTopicRequest, _: AsyncHandler[aws.ListSubscriptionsByTopicRequest,aws.ListSubscriptionsByTopicResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn == topicArn.arnString &&
                    (
                      Option(r.getNextToken) match {
                        case None        ⇒ i == 0
                        case Some(token) ⇒ token == i.toString
                      }
                    )
                })
                .withAwsSuccess {
                  val result = new aws.ListSubscriptionsByTopicResult
                  if (i + 1 != listings.size) {
                    result.setNextToken((i + 1).toString)
                  }
                  result.withSubscriptions(listing.map(_.asAws).asJavaCollection)
                }
            }

            val result = f.async.listSubscriptions(topicArn.arnString).futureValue
            result shouldBe subscriptionSummaries
          }
        }
      }
    }

    "subscribe to a topic" - {
      "with a resulting subscription ARN" in {
        forAll { subscription: Subscription ⇒
          val subscriptionArn = subscription.arn
          val topicArn = subscription.topicArn
          val SubscriptionEndpoint(protocol, endpoint) = subscription.endpoint
          withFixture { f ⇒
            (f.sns.subscribeAsync(_: aws.SubscribeRequest, _: AsyncHandler[aws.SubscribeRequest,aws.SubscribeResult]))
              .expects(whereRequest(r ⇒
                r.getTopicArn == topicArn &&
                  r.getProtocol == protocol.asAws &&
                  r.getEndpoint == endpoint))
              .withAwsSuccess(new aws.SubscribeResult().withSubscriptionArn(subscriptionArn))

            val result = f.async.subscribe(topicArn, protocol.asAws, endpoint).futureValue
            result shouldBe Some(subscriptionArn)
          }
        }
      }

      "without a resulting subscription ARN" in {
        forAll { subscription: Subscription ⇒
          val topicArn = subscription.topicArn
          val SubscriptionEndpoint(protocol, endpoint) = subscription.endpoint
          withFixture { f ⇒
            (f.sns.subscribeAsync(_: aws.SubscribeRequest, _: AsyncHandler[aws.SubscribeRequest,aws.SubscribeResult]))
              .expects(whereRequest(r ⇒
                r.getTopicArn == topicArn &&
                  r.getProtocol == protocol.asAws &&
                  r.getEndpoint == endpoint))
              .withAwsSuccess(new aws.SubscribeResult().withSubscriptionArn("pending confirmation"))

            val result = f.async.subscribe(topicArn, protocol.asAws, endpoint).futureValue
            result shouldBe None
          }
        }
      }
    }

    "confirm a subscription" - {
      "without specifying authenticate on unsubscribe" in {
        forAll(
          arbitrary[SubscriptionArn] → "subscriptionArn",
          SnsGen.confirmationToken → "token"
        ) { (subscriptionArn, token) ⇒
          val topicArn = TopicArn(subscriptionArn.account, subscriptionArn.region, subscriptionArn.topicName)
          withFixture { f ⇒
            (f.sns.confirmSubscriptionAsync(_: aws.ConfirmSubscriptionRequest, _: AsyncHandler[aws.ConfirmSubscriptionRequest,aws.ConfirmSubscriptionResult]))
              .expects(whereRequest(r ⇒ r.getTopicArn == topicArn.arnString && r.getToken == token && r.getAuthenticateOnUnsubscribe == null))
              .withAwsSuccess(new aws.ConfirmSubscriptionResult().withSubscriptionArn(subscriptionArn.arnString))

            val result = f.async.confirmSubscription(topicArn.arnString, token).futureValue
            result shouldBe subscriptionArn.arnString
          }
        }
      }

      "with specifying authenticate on unsubscribe" in {
        forAll(
          arbitrary[SubscriptionArn] → "subscriptionArn",
          SnsGen.confirmationToken → "token",
          arbitrary[Boolean] → "authoOnUnsub"
        ) { (subscriptionArn, token, authOnUnsub) ⇒
          val topicArn = TopicArn(subscriptionArn.account, subscriptionArn.region, subscriptionArn.topicName)
          withFixture { f ⇒
            (f.sns.confirmSubscriptionAsync(_: aws.ConfirmSubscriptionRequest, _: AsyncHandler[aws.ConfirmSubscriptionRequest,aws.ConfirmSubscriptionResult]))
              .expects(whereRequest(r ⇒ r.getTopicArn == topicArn.arnString && r.getToken == token && r.getAuthenticateOnUnsubscribe == authOnUnsub.toString))
              .withAwsSuccess(new aws.ConfirmSubscriptionResult().withSubscriptionArn(subscriptionArn.arnString))

            val result = f.async.confirmSubscription(topicArn.arnString, token, authOnUnsub).futureValue
            result shouldBe subscriptionArn.arnString
          }
        }
      }
    }

    "get subscription attributes" - {
      "all of them" in {
        forAll { subscription: Subscription ⇒
          val arn = subscription.arn
          val attributes = subscription.attributes

          withFixture { f ⇒
            (f.sns.getSubscriptionAttributesAsync(_: aws.GetSubscriptionAttributesRequest, _: AsyncHandler[aws.GetSubscriptionAttributesRequest,aws.GetSubscriptionAttributesResult]))
              .expects(whereRequest(_.getSubscriptionArn == arn))
              .withAwsSuccess(new aws.GetSubscriptionAttributesResult().withAttributes(attributes.asJava))

            val result = f.async.getSubscriptionAttributes(arn).futureValue
            result shouldBe attributes
          }
        }
      }

      "a specific one" in {
        val attributeName = Gen.oneOf("SubscriptionArn", "TopicArn", "Protocol", "Endpoint", "Owner",
          "ConfirmationWasAuthenticated", "DeliveryPolicy", "EffectiveDeliveryPolicy", "RawMessageDeliver", "Bad")
        forAll(arbitrary[Subscription] → "subscription", attributeName → "attributeName") { (subscription, attributeName) ⇒
          val arn = subscription.arn
          val attributes = subscription.attributes

          withFixture { f ⇒
            (f.sns.getSubscriptionAttributesAsync(_: aws.GetSubscriptionAttributesRequest, _: AsyncHandler[aws.GetSubscriptionAttributesRequest,aws.GetSubscriptionAttributesResult]))
              .expects(whereRequest(_.getSubscriptionArn == arn))
              .withAwsSuccess(new aws.GetSubscriptionAttributesResult().withAttributes(attributes.asJava))

            val result = f.async.getSubscriptionAttribute(arn, attributeName).futureValue
            result shouldBe attributes.get(attributeName)
          }
        }
      }
    }

    "set a subscription attribute" - {
      "using a string value" in {
        forAll { (subscriptionArn: SubscriptionArn, maybeDeliveryPolicy: Option[SubscriptionDeliveryPolicy]) ⇒
          val deliveryPolicy = maybeDeliveryPolicy.map(_.toString).orNull
          withFixture { f ⇒
            (f.sns.setSubscriptionAttributesAsync(_: aws.SetSubscriptionAttributesRequest, _: AsyncHandler[aws.SetSubscriptionAttributesRequest,aws.SetSubscriptionAttributesResult]))
              .expects(whereRequest(r ⇒
                r.getSubscriptionArn == subscriptionArn.arnString &&
                  r.getAttributeName == "DeliveryPolicy" &&
                  r.getAttributeValue == deliveryPolicy
              ))
              .withVoidAwsSuccess()

            val result = f.async.setSubscriptionAttribute(subscriptionArn.arnString, "DeliveryPolicy", deliveryPolicy).futureValue
            result shouldBe Done
          }
        }
      }

      "using an option" in {
        forAll { (subscriptionArn: SubscriptionArn, maybeDeliveryPolicyObj: Option[SubscriptionDeliveryPolicy]) ⇒
          val maybeDeliveryPolicy = maybeDeliveryPolicyObj.map(_.toString)
          withFixture { f ⇒
            (f.sns.setSubscriptionAttributesAsync(_: aws.SetSubscriptionAttributesRequest, _: AsyncHandler[aws.SetSubscriptionAttributesRequest,aws.SetSubscriptionAttributesResult]))
              .expects(whereRequest(r ⇒
                r.getSubscriptionArn == subscriptionArn.arnString &&
                  r.getAttributeName == "DeliveryPolicy" &&
                  r.getAttributeValue == maybeDeliveryPolicy.orNull
              ))
              .withVoidAwsSuccess()

            val result = f.async.setSubscriptionAttribute(subscriptionArn.arnString, "DeliveryPolicy", maybeDeliveryPolicy).futureValue
            result shouldBe Done
          }
        }
      }
    }

    "unsubscribe" in {
      forAll { subscriptionArn: SubscriptionArn ⇒
        withFixture { f ⇒
          (f.sns.unsubscribeAsync(_: aws.UnsubscribeRequest, _: AsyncHandler[aws.UnsubscribeRequest,aws.UnsubscribeResult]))
            .expects(whereRequest(_.getSubscriptionArn == subscriptionArn.arnString))
            .withVoidAwsSuccess()

          val result = f.async.unsubscribe(subscriptionArn.arnString).futureValue
          result shouldBe Done
        }
      }
    }

    "create platform applications" - {
      "without additional attributes" in {
        forAll { (platformApplication: PlatformApplication, credentials: PlatformApplicationCredentials) ⇒
          import platformApplication._
          val PlatformApplicationCredentials(_, principal, credential) =  credentials
          withFixture { f ⇒
            (f.sns.createPlatformApplicationAsync(_: aws.CreatePlatformApplicationRequest, _: AsyncHandler[aws.CreatePlatformApplicationRequest,aws.CreatePlatformApplicationResult]))
              .expects(whereRequest(r ⇒
                r.getName == name &&
                  r.getPlatform == platform.name &&
                  r.getAttributes.asScala == Map("PlatformPrincipal" → principal, "PlatformCredential" → credential)))
              .withAwsSuccess(new aws.CreatePlatformApplicationResult().withPlatformApplicationArn(arn))

            val result = f.async.createPlatformApplication(name, platform.name, principal, credential).futureValue
            result shouldBe arn
          }
        }
      }

      "with additional attributes" in {
        forAll { (platformApplication: PlatformApplication, credentials: PlatformApplicationCredentials, attributes: Map[String,String]) ⇒
          import platformApplication.{attributes ⇒ _, _}
          val PlatformApplicationCredentials(_, principal, credential) =  credentials

          withFixture { f ⇒
            (f.sns.createPlatformApplicationAsync(_: aws.CreatePlatformApplicationRequest, _: AsyncHandler[aws.CreatePlatformApplicationRequest,aws.CreatePlatformApplicationResult]))
              .expects(whereRequest(r ⇒
                r.getName == name &&
                  r.getPlatform == platform.name &&
                  r.getAttributes.asScala == attributes ++ Map("PlatformPrincipal" → principal, "PlatformCredential" → credential)))
              .withAwsSuccess(new aws.CreatePlatformApplicationResult().withPlatformApplicationArn(arn))

            val result = f.async.createPlatformApplication(name, platform.name, principal, credential, attributes).futureValue
            result shouldBe arn
          }
        }
      }
    }

    "get a platform applications attributes" - {
      "all fo them" in {
        forAll { platformApplication: PlatformApplication ⇒
          withFixture { f ⇒
            (f.sns.getPlatformApplicationAttributesAsync(_: aws.GetPlatformApplicationAttributesRequest, _: AsyncHandler[aws.GetPlatformApplicationAttributesRequest,aws.GetPlatformApplicationAttributesResult]))
              .expects(whereRequest(_.getPlatformApplicationArn == platformApplication.arn))
              .withAwsSuccess(new aws.GetPlatformApplicationAttributesResult().withAttributes(platformApplication.attributes.asJava))

            val result = f.async.getPlatformApplicationAttributes(platformApplication.arn).futureValue
            result shouldBe platformApplication.attributes
          }
        }
      }

      "a specific one" in {
        val attribute = Gen.oneOf("Enabled", "EventEndpointCreated", "EventEndpointDeleted", "EventEndpointUpdated",
          "EventDeliveryFailure", "SuccessFeedbackRoleArn", "SuccessFeedbackSampleRate", "FailureFeedbackRoleArn",
          "Invalid")
        forAll(
          arbitrary[PlatformApplication] → "platformApplication",
          attribute → "attribute"
        ) { (platformApplication, attribute) ⇒
          withFixture { f ⇒
            (f.sns.getPlatformApplicationAttributesAsync(_: aws.GetPlatformApplicationAttributesRequest, _: AsyncHandler[aws.GetPlatformApplicationAttributesRequest,aws.GetPlatformApplicationAttributesResult]))
              .expects(whereRequest(_.getPlatformApplicationArn == platformApplication.arn))
              .withAwsSuccess(new aws.GetPlatformApplicationAttributesResult().withAttributes(platformApplication.attributes.asJava))

            val result = f.async.getPlatformApplicationAttribute(platformApplication.arn, attribute).futureValue
            result shouldBe platformApplication.attributes.get(attribute)
          }
        }
      }
    }

    "set platform application attributes" - {
      "using a map" in {
        forAll { (arn: PlatformApplicationArn, attributes: Map[String,String]) ⇒
          withFixture { f ⇒
            (f.sns.setPlatformApplicationAttributesAsync(_: aws.SetPlatformApplicationAttributesRequest, _: AsyncHandler[aws.SetPlatformApplicationAttributesRequest, aws.SetPlatformApplicationAttributesResult]))
              .expects(whereRequest(r ⇒
                r.getPlatformApplicationArn == arn.arnString &&
                  r.getAttributes.asScala == attributes))
              .withVoidAwsSuccess()

            val result = f.async.setPlatformApplicationAttributes(arn.arnString, attributes).futureValue
            result shouldBe Done
          }
        }
      }

      "using a plain value" in {
        forAll { (arn: PlatformApplicationArn, name: String, value: String) ⇒
          withFixture { f ⇒
            (f.sns.setPlatformApplicationAttributesAsync(_: aws.SetPlatformApplicationAttributesRequest, _: AsyncHandler[aws.SetPlatformApplicationAttributesRequest, aws.SetPlatformApplicationAttributesResult]))
              .expects(whereRequest(r ⇒
                r.getPlatformApplicationArn == arn.arnString &&
                  r.getAttributes.asScala == Map(name → value)))
              .withVoidAwsSuccess()

            val result = f.async.setPlatformApplicationAttribute(arn.arnString, name, value).futureValue
            result shouldBe Done
          }
        }
      }

      "using an optional value" in {
        forAll { (arn: PlatformApplicationArn, name: String, value: Option[String]) ⇒
          withFixture { f ⇒
            (f.sns.setPlatformApplicationAttributesAsync(_: aws.SetPlatformApplicationAttributesRequest, _: AsyncHandler[aws.SetPlatformApplicationAttributesRequest, aws.SetPlatformApplicationAttributesResult]))
              .expects(whereRequest(r ⇒
                r.getPlatformApplicationArn == arn.arnString &&
                  r.getAttributes.asScala == Map(name → value.getOrElse(""))))
              .withVoidAwsSuccess()

            val result = f.async.setPlatformApplicationAttribute(arn.arnString, name, value).futureValue
            result shouldBe Done
          }
        }
      }
    }

    "delete platform applications" in {
      forAll { arn: PlatformApplicationArn ⇒
        withFixture { f ⇒
          (f.sns.deletePlatformApplicationAsync(_: aws.DeletePlatformApplicationRequest, _: AsyncHandler[aws.DeletePlatformApplicationRequest, aws.DeletePlatformApplicationResult]))
            .expects(whereRequest(_.getPlatformApplicationArn == arn.arnString))
            .withVoidAwsSuccess()

          val result = f.async.deletePlatformApplication(arn.arnString).futureValue
          result shouldBe Done
        }
      }
    }

    "list platform applications" in {
      forAll { applications: List[PlatformApplication] ⇒
        val awsApplications = applications.map { app ⇒
          new aws.PlatformApplication()
            .withPlatformApplicationArn(app.arn)
            .withAttributes(app.attributes.asJava)
        }
        withFixture { f ⇒
          val listings = if (applications.isEmpty) List(awsApplications) else awsApplications.grouped(5).toList
          listings.zipWithIndex.foreach { case (listing, i) ⇒
            (f.sns.listPlatformApplicationsAsync(_: aws.ListPlatformApplicationsRequest, _: AsyncHandler[aws.ListPlatformApplicationsRequest,aws.ListPlatformApplicationsResult]))
              .expects(whereRequest { r ⇒
                Option(r.getNextToken) match {
                  case None        ⇒ i == 0
                  case Some(token) ⇒ token == i.toString
                }
              })
              .withAwsSuccess {
                val result = new aws.ListPlatformApplicationsResult
                if (i + 1 != listings.size) {
                  result.setNextToken((i + 1).toString)
                }
                result.withPlatformApplications(listing.asJavaCollection)
              }
          }

          val result = f.async.listPlatformApplications().futureValue
          result shouldBe applications
        }
      }
    }

    "create platform endpoints" - {
      "using only a token" in {
        forAll { (endpointArn: PlatformEndpointArn, token: String) ⇒
          val applicationArn = PlatformApplicationArn(endpointArn.account, endpointArn.region, endpointArn.platform,
            endpointArn.applicationName).arnString
          withFixture { f ⇒
            (f.sns.createPlatformEndpointAsync(_: aws.CreatePlatformEndpointRequest, _: AsyncHandler[aws.CreatePlatformEndpointRequest,aws.CreatePlatformEndpointResult]))
              .expects(whereRequest(r ⇒
                r.getPlatformApplicationArn == applicationArn &&
                r.getToken == token &&
                r.getCustomUserData == null &&
                r.getAttributes.isEmpty))
              .withAwsSuccess(new aws.CreatePlatformEndpointResult().withEndpointArn(endpointArn.arnString))

            val result = f.async.createPlatformEndpoint(applicationArn, token).futureValue
            result shouldBe endpointArn.arnString
          }
        }
      }

      "using a token and user data" in {
        forAll { (endpointArn: PlatformEndpointArn, token: String, userData: String) ⇒
          val applicationArn = PlatformApplicationArn(endpointArn.account, endpointArn.region, endpointArn.platform,
            endpointArn.applicationName).arnString
          withFixture { f ⇒
            (f.sns.createPlatformEndpointAsync(_: aws.CreatePlatformEndpointRequest, _: AsyncHandler[aws.CreatePlatformEndpointRequest,aws.CreatePlatformEndpointResult]))
              .expects(whereRequest(r ⇒
                r.getPlatformApplicationArn == applicationArn &&
                  r.getToken == token &&
                  r.getCustomUserData == userData &&
                  r.getAttributes.isEmpty))
              .withAwsSuccess(new aws.CreatePlatformEndpointResult().withEndpointArn(endpointArn.arnString))

            val result = f.async.createPlatformEndpoint(applicationArn, token, userData).futureValue
            result shouldBe endpointArn.arnString
          }
        }
      }

      "using a token and attributes" in {
        forAll { (endpointArn: PlatformEndpointArn, token: String, attributes: Map[String,String]) ⇒
          val applicationArn = PlatformApplicationArn(endpointArn.account, endpointArn.region, endpointArn.platform,
            endpointArn.applicationName).arnString
          withFixture { f ⇒
            (f.sns.createPlatformEndpointAsync(_: aws.CreatePlatformEndpointRequest, _: AsyncHandler[aws.CreatePlatformEndpointRequest,aws.CreatePlatformEndpointResult]))
              .expects(whereRequest(r ⇒
                r.getPlatformApplicationArn == applicationArn &&
                  r.getToken == token &&
                  r.getCustomUserData == null &&
                  r.getAttributes.asScala == attributes))
              .withAwsSuccess(new aws.CreatePlatformEndpointResult().withEndpointArn(endpointArn.arnString))

            val result = f.async.createPlatformEndpoint(applicationArn, token, attributes).futureValue
            result shouldBe endpointArn.arnString
          }
        }
      }

      "using a token, attributes, and user data" in {
        forAll { (endpointArn: PlatformEndpointArn, token: String, userData: String, attributes: Map[String,String]) ⇒
          val applicationArn = PlatformApplicationArn(endpointArn.account, endpointArn.region, endpointArn.platform,
            endpointArn.applicationName).arnString
          withFixture { f ⇒
            (f.sns.createPlatformEndpointAsync(_: aws.CreatePlatformEndpointRequest, _: AsyncHandler[aws.CreatePlatformEndpointRequest,aws.CreatePlatformEndpointResult]))
              .expects(whereRequest(r ⇒
                r.getPlatformApplicationArn == applicationArn &&
                  r.getToken == token &&
                  r.getCustomUserData == userData &&
                  r.getAttributes.asScala == attributes))
              .withAwsSuccess(new aws.CreatePlatformEndpointResult().withEndpointArn(endpointArn.arnString))

            val result = f.async.createPlatformEndpoint(applicationArn, token, userData, attributes).futureValue
            result shouldBe endpointArn.arnString
          }
        }
      }
    }

    "get platform endpoint attributes" - {
      "all of them" in {
        forAll { endpoint: PlatformEndpoint ⇒
          withFixture { f ⇒
            (f.sns.getEndpointAttributesAsync(_: aws.GetEndpointAttributesRequest, _: AsyncHandler[aws.GetEndpointAttributesRequest,aws.GetEndpointAttributesResult]))
              .expects(whereRequest(_.getEndpointArn == endpoint.arn))
              .withAwsSuccess(new aws.GetEndpointAttributesResult().withAttributes(endpoint.attributes.asJava))

            val result = f.async.getPlatformEndpointAttributes(endpoint.arn).futureValue
            result shouldBe endpoint.attributes
          }
        }
      }

      "just one of them" in {
        val attributeName = Gen.oneOf("CustomUserData", "Enabled", "Token")
        forAll(
          arbitrary[PlatformEndpoint] → "endpoint",
          attributeName → "attributeName"
        ) { (endpoint, attributeName) ⇒
          withFixture { f ⇒
            (f.sns.getEndpointAttributesAsync(_: aws.GetEndpointAttributesRequest, _: AsyncHandler[aws.GetEndpointAttributesRequest,aws.GetEndpointAttributesResult]))
              .expects(whereRequest(_.getEndpointArn == endpoint.arn))
              .withAwsSuccess(new aws.GetEndpointAttributesResult().withAttributes(endpoint.attributes.asJava))

            val result = f.async.getPlatformEndpointAttribute(endpoint.arn, attributeName).futureValue
            result shouldBe endpoint.attributes.get(attributeName)
          }
        }
      }
    }

    "set platform endpoint attributes using" - {
      "plain values" in {
        forAll { (endpointArn: PlatformEndpointArn, attributeName: String, attributeValue: String) ⇒
          withFixture { f ⇒
            (f.sns.setEndpointAttributesAsync(_: aws.SetEndpointAttributesRequest, _: AsyncHandler[aws.SetEndpointAttributesRequest,aws.SetEndpointAttributesResult]))
              .expects(whereRequest(r ⇒
                r.getEndpointArn == endpointArn.arnString &&
                  r.getAttributes.asScala == Map(attributeName → attributeValue)))
              .withVoidAwsSuccess()

            val result = f.async.setPlatformEndpointAttributes(endpointArn.arnString, attributeName, attributeValue).futureValue
            result shouldBe Done
          }
        }
      }

      "options" in {
        forAll { (endpointArn: PlatformEndpointArn, attributeName: String, attributeValue: Option[String]) ⇒
          withFixture { f ⇒
            (f.sns.setEndpointAttributesAsync(_: aws.SetEndpointAttributesRequest, _: AsyncHandler[aws.SetEndpointAttributesRequest,aws.SetEndpointAttributesResult]))
              .expects(whereRequest(r ⇒
                r.getEndpointArn == endpointArn.arnString &&
                  r.getAttributes.asScala == Map(attributeName → attributeValue.getOrElse(""))))
              .withVoidAwsSuccess()

            val result = f.async.setPlatformEndpointAttributes(endpointArn.arnString, attributeName, attributeValue).futureValue
            result shouldBe Done
          }
        }
      }

      "bulk updates" in {
        forAll { (endpointArn: PlatformEndpointArn, attributes: Map[String,String]) ⇒
          withFixture { f ⇒
            (f.sns.setEndpointAttributesAsync(_: aws.SetEndpointAttributesRequest, _: AsyncHandler[aws.SetEndpointAttributesRequest,aws.SetEndpointAttributesResult]))
              .expects(whereRequest(r ⇒
                r.getEndpointArn == endpointArn.arnString &&
                  r.getAttributes.asScala == attributes))
              .withVoidAwsSuccess()

            val result = f.async.setPlatformEndpointAttributes(endpointArn.arnString, attributes).futureValue
            result shouldBe Done
          }
        }
      }
    }

    "delete platform endpoints" in {
      forAll { arn: PlatformEndpointArn ⇒
        withFixture { f ⇒
          (f.sns.deleteEndpointAsync(_: aws.DeleteEndpointRequest, _: AsyncHandler[aws.DeleteEndpointRequest,aws.DeleteEndpointResult]))
            .expects(whereRequest(r ⇒ r.getEndpointArn == arn.arnString))
            .withVoidAwsSuccess()

          val result = f.async.deletePlatformEndpoint(arn.arnString).futureValue
          result shouldBe Done
        }
      }
    }

    "list platform endpoints" in {
      forAll { (applicationArn: PlatformApplicationArn, endpoints: List[PlatformEndpoint]) ⇒
        val awsEndpoints = endpoints.map(_.asAws)
        withFixture { f ⇒
          val listings = if (endpoints.isEmpty) List(awsEndpoints) else awsEndpoints.grouped(5).toList
          listings.zipWithIndex.foreach { case (listing, i) ⇒
            (f.sns.listEndpointsByPlatformApplicationAsync(_: aws.ListEndpointsByPlatformApplicationRequest, _: AsyncHandler[aws.ListEndpointsByPlatformApplicationRequest,aws.ListEndpointsByPlatformApplicationResult]))
              .expects(whereRequest { r ⇒
                Option(r.getNextToken) match {
                  case None        ⇒ i == 0
                  case Some(token) ⇒ token == i.toString
                }
              })
              .withAwsSuccess {
                val result = new aws.ListEndpointsByPlatformApplicationResult
                if (i + 1 != listings.size) {
                  result.setNextToken((i + 1).toString)
                }
                result.withEndpoints(listing.asJavaCollection)
              }
          }

          val result = f.async.listPlatformEndpoints(applicationArn.arnString).futureValue
          result shouldBe endpoints
        }
      }
    }

    "publish" - {
      "simple messages" - {
        "using a target and a message" in {
          forAll(
            SnsGen.targetArn → "targetArn",
            UtilGen.nonEmptyString → "message",
            SnsGen.messageId → "messageId"
          ) { (targetArn, message, messageId) ⇒
            withFixture { f ⇒
              (f.sns.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn.arnString
                  r.getMessage shouldBe message
                  r.getMessageStructure shouldBe null
                  r.getSubject shouldBe null
                  r.getMessageAttributes.asScala shouldBe Map.empty
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn.arnString, message).futureValue
              result shouldBe messageId
            }
          }
        }

        "using a target, a message, and a subject" in {
          forAll(
            SnsGen.targetArn → "targetArn",
            UtilGen.nonEmptyString → "message",
            UtilGen.nonEmptyString → "subject",
            SnsGen.messageId → "messageId"
          ) { (targetArn, message, subject, messageId) ⇒
            withFixture { f ⇒
              (f.sns.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn.arnString
                  r.getMessage shouldBe message
                  r.getMessageStructure shouldBe null
                  r.getSubject shouldBe subject
                  r.getMessageAttributes.asScala shouldBe Map.empty
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn.arnString, message, subject).futureValue
              result shouldBe messageId
            }
          }
        }

        "using a target, a message, and some attributes" in {
          forAll(
            SnsGen.targetArn → "targetArn",
            UtilGen.nonEmptyString → "message",
            arbitrary[Map[String,MessageAttributeValue]] → "attributes",
            SnsGen.messageId → "messageId"
          ) { (targetArn, message, attributes, messageId) ⇒
            withFixture { f ⇒
              (f.sns.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn.arnString
                  r.getMessage shouldBe message
                  r.getMessageStructure shouldBe null
                  r.getSubject shouldBe null
                  r.getMessageAttributes.asScala shouldBe attributes.mapValues(_.asAws)
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn.arnString, message, attributes).futureValue
              result shouldBe messageId
            }
          }
        }

        "using a target, a message, a subject, and some attributes" in {
          forAll(
            SnsGen.targetArn → "targetArn",
            UtilGen.nonEmptyString → "message",
            UtilGen.nonEmptyString → "subject",
            arbitrary[Map[String,MessageAttributeValue]] → "attributes",
            SnsGen.messageId → "messageId"
          ) { (targetArn, message, subject, attributes, messageId) ⇒
            withFixture { f ⇒
              (f.sns.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn.arnString
                  r.getMessage shouldBe message
                  r.getMessageStructure shouldBe null
                  r.getSubject shouldBe subject
                  r.getMessageAttributes.asScala shouldBe attributes.mapValues(_.asAws)
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn.arnString, message, subject, attributes).futureValue
              result shouldBe messageId
            }
          }
        }
      }

      "compound messages" - {
        "using a target and a message" in {
          forAll(
            SnsGen.targetArn → "targetArn",
            SnsGen.messageMap → "message",
            SnsGen.messageId → "messageId"
          ) { (targetArn, message, messageId) ⇒
            withFixture { f ⇒
              (f.sns.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn.arnString
                  JsonParser(r.getMessage) shouldBe JsObject(message.mapValues(JsString(_)))
                  r.getMessageStructure shouldBe "json"
                  r.getSubject shouldBe null
                  r.getMessageAttributes.asScala shouldBe Map.empty
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn.arnString, message).futureValue
              result shouldBe messageId
            }
          }
        }

        "using a target, a message, and a subject" in {
          forAll(
            SnsGen.targetArn → "targetArn",
            SnsGen.messageMap → "message",
            UtilGen.nonEmptyString → "subject",
            SnsGen.messageId → "messageId"
          ) { (targetArn, message, subject, messageId) ⇒
            withFixture { f ⇒
              (f.sns.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn.arnString
                  JsonParser(r.getMessage) shouldBe JsObject(message.mapValues(JsString(_)))
                  r.getMessageStructure shouldBe "json"
                  r.getSubject shouldBe subject
                  r.getMessageAttributes.asScala shouldBe Map.empty
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn.arnString, message, subject).futureValue
              result shouldBe messageId
            }
          }
        }

        "using a target, a message, and some attributes" in {
          forAll(
            SnsGen.targetArn → "targetArn",
            SnsGen.messageMap → "message",
            arbitrary[Map[String,MessageAttributeValue]] → "attributes",
            SnsGen.messageId → "messageId"
          ) { (targetArn, message, attributes, messageId) ⇒
            withFixture { f ⇒
              (f.sns.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn.arnString
                  JsonParser(r.getMessage) shouldBe JsObject(message.mapValues(JsString(_)))
                  r.getMessageStructure shouldBe "json"
                  r.getSubject shouldBe null
                  r.getMessageAttributes.asScala shouldBe attributes.mapValues(_.asAws)
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn.arnString, message, attributes).futureValue
              result shouldBe messageId
            }
          }
        }

        "using a target, a message, a subject, and some attributes" in {
          forAll(
            SnsGen.targetArn → "targetArn",
            SnsGen.messageMap → "message",
            UtilGen.nonEmptyString → "subject",
            arbitrary[Map[String,MessageAttributeValue]] → "attributes",
            SnsGen.messageId → "messageId"
          ) { (targetArn, message, subject, attributes, messageId) ⇒
            withFixture { f ⇒
              (f.sns.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn.arnString
                  JsonParser(r.getMessage) shouldBe JsObject(message.mapValues(JsString(_)))
                  r.getMessageStructure shouldBe "json"
                  r.getSubject shouldBe subject
                  r.getMessageAttributes.asScala shouldBe attributes.mapValues(_.asAws)
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn.arnString, message, subject, attributes).futureValue
              result shouldBe messageId
            }
          }
        }
      }
    }
  }

  private case class Fixture(sns: AmazonSNSAsync,
                             async: AsyncSNSClient)

  private def withFixture(test: Fixture ⇒ Unit): Unit = {
    val aws = mock[AmazonSNSAsync]("aws")
    val streaming = new DefaultStreamingSNSClient(aws)
    val async = new DefaultAsyncSNSClient(streaming)
    test(Fixture(aws, async))
  }
}
