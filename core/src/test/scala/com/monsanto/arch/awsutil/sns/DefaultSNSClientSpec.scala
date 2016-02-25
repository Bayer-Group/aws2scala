package com.monsanto.arch.awsutil.sns

import akka.Done
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sns.{AmazonSNSAsync, model ⇒ aws}
import com.monsanto.arch.awsutil.sns.model.SNSGen.TopicArn
import com.monsanto.arch.awsutil.sns.model._
import com.monsanto.arch.awsutil.test.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.{AwsGen, AwsMockUtils, Materialised}
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
      forAll(SNSGen.topicArn → "topicArn") { topicArn ⇒
        val name = TopicArn(topicArn).name
        withFixture { f ⇒
          (f.aws.createTopicAsync(_: aws.CreateTopicRequest, _: AsyncHandler[aws.CreateTopicRequest,aws.CreateTopicResult]))
            .expects(whereRequest(_.getName == name))
            .withAwsSuccess(new aws.CreateTopicResult().withTopicArn(topicArn))

          val result = f.async.createTopic(name).futureValue
          result shouldBe topicArn
        }
      }
    }

    "list the topics" in {
      forAll(Gen.listOf(SNSGen.topicArn) → "topics") { topics ⇒
        withFixture { f ⇒
          val listings = if (topics.isEmpty) List(topics) else topics.grouped(5).toList
          listings.zipWithIndex.foreach { case (listing, i) ⇒
            (f.aws.listTopicsAsync(_: aws.ListTopicsRequest, _: AsyncHandler[aws.ListTopicsRequest,aws.ListTopicsResult]))
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
                result.withTopics(listing.map(arn ⇒ new aws.Topic().withTopicArn(arn)).asJavaCollection)
              }
          }

          val result = f.async.listTopics().futureValue
          result shouldBe topics
        }
      }
    }

    "delete a topic" in {
      forAll(SNSGen.topicArn → "topicArn") { topicArn ⇒
        withFixture { f ⇒
          (f.aws.deleteTopicAsync(_: aws.DeleteTopicRequest, _: AsyncHandler[aws.DeleteTopicRequest,Void]))
            .expects(whereRequest(_.getTopicArn == topicArn))
            .withVoidAwsSuccess()

          val result = f.async.deleteTopic(topicArn).futureValue
          result shouldBe Done
        }
      }
    }

    "get all of a topic’s attributes" in {
      forAll(SNSGen.topic → "topic") { topic ⇒
        withFixture { f ⇒
          (f.aws.getTopicAttributesAsync(_: aws.GetTopicAttributesRequest, _: AsyncHandler[aws.GetTopicAttributesRequest,aws.GetTopicAttributesResult]))
            .expects(whereRequest(_.getTopicArn == topic.arn))
            .withAwsSuccess(new aws.GetTopicAttributesResult().withAttributes(topic.attributes.asJava))

          val result = f.async.getTopicAttributes(topic.arn).futureValue
          result shouldBe topic.attributes
        }
      }
    }

    "get a specific attribute from a topic" in {
      val topicAttribute = Gen.oneOf("DisplayName", "Policy", "DeliveryPolicy", "EffectiveDeliveryPolicy", "Missing")
      forAll(SNSGen.topic → "topic", topicAttribute → "attributeName") { (topic, attributeName) ⇒
        withFixture { f ⇒
          (f.aws.getTopicAttributesAsync(_: aws.GetTopicAttributesRequest, _: AsyncHandler[aws.GetTopicAttributesRequest,aws.GetTopicAttributesResult]))
            .expects(whereRequest(_.getTopicArn == topic.arn))
            .withAwsSuccess(new aws.GetTopicAttributesResult().withAttributes(topic.attributes.asJava))

          val result = f.async.getTopicAttribute(topic.arn, attributeName).futureValue
          result shouldBe topic.attributes.get(attributeName)
        }
      }
    }

    "set a topic attribute" - {
      "using a plain value" in {
        forAll(
          SNSGen.topicArn → "topicArn",
          SNSGen.maybeTopicDeliveryPolicy → "maybeDeliveryPolicy"
        ) { (topicArn, maybeDeliveryPolicy) ⇒
          withFixture { f ⇒
            (f.aws.setTopicAttributesAsync(_: aws.SetTopicAttributesRequest, _: AsyncHandler[aws.SetTopicAttributesRequest,Void]))
              .expects(whereRequest(r ⇒
                r.getTopicArn == topicArn &&
                  r.getAttributeName == "DeliveryPolicy" &&
                  r.getAttributeValue == maybeDeliveryPolicy.orNull
              ))
              .withVoidAwsSuccess()

            val result = f.async.setTopicAttribute(topicArn, "DeliveryPolicy", maybeDeliveryPolicy).futureValue
            result shouldBe Done
          }
        }
      }

      "using an option" in {
        forAll(
          SNSGen.topicArn → "topicArn",
          SNSGen.maybeTopicDeliveryPolicy.map(_.orNull) → "deliveryPolicy"
        ) { (topicArn, deliveryPolicy) ⇒
          withFixture { f ⇒
            (f.aws.setTopicAttributesAsync(_: aws.SetTopicAttributesRequest, _: AsyncHandler[aws.SetTopicAttributesRequest,Void]))
              .expects(whereRequest(r ⇒
                r.getTopicArn == topicArn &&
                  r.getAttributeName == "DeliveryPolicy" &&
                  r.getAttributeValue == deliveryPolicy
              ))
              .withVoidAwsSuccess()

            val result = f.async.setTopicAttribute(topicArn, "DeliveryPolicy", deliveryPolicy).futureValue
            result shouldBe Done
          }
        }
      }
    }

    "add a permission" in {
      forAll(
        SNSGen.topicArn → "arn",
        Gen.alphaStr.suchThat(_.nonEmpty) → "label",
        Gen.nonEmptyListOf(SNSGen.snsAction) → "actions",
        Gen.nonEmptyListOf(arbitrary[AwsGen.Account].map(_.value)) → "accounts"
      ) { (arn, label, actions, accounts) ⇒
        withFixture { f ⇒
          (f.aws.addPermissionAsync(_: aws.AddPermissionRequest, _: AsyncHandler[aws.AddPermissionRequest,Void]))
            .expects(whereRequest(r ⇒
              r.getTopicArn == arn &&
                r.getLabel == label &&
                r.getAWSAccountIds.asScala == accounts &&
                r.getActionNames.asScala == actions.map(_.toString)))
            .withVoidAwsSuccess()

          val result = f.async.addPermission(arn, label, accounts, actions).futureValue
          result shouldBe Done
        }
      }
    }

    "remove a permission" in {
      forAll(SNSGen.topicArn → "arn", Gen.alphaStr.suchThat(_.nonEmpty) → "label") { (arn, label) ⇒
        withFixture { f ⇒
          (f.aws.removePermissionAsync(_: aws.RemovePermissionRequest, _: AsyncHandler[aws.RemovePermissionRequest,Void]))
            .expects(whereRequest(r ⇒ r.getTopicArn == arn && r.getLabel == label))
            .withVoidAwsSuccess()

          val result = f.async.removePermission(arn, label).futureValue
          result shouldBe Done
        }
      }
    }

    "list subscriptions" - {
      implicit class Awsifier(subscriptionSummary: SubscriptionSummary) {
        def toAws =
          new aws.Subscription()
            .withSubscriptionArn(subscriptionSummary.arn.getOrElse("PendingConfirmation"))
            .withTopicArn(subscriptionSummary.topicArn)
            .withProtocol(subscriptionSummary.endpoint.protocol.toAws)
            .withEndpoint(subscriptionSummary.endpoint.endpoint)
            .withOwner(subscriptionSummary.owner)
      }

      "all of them" in {
        forAll(Gen.listOf(SNSGen.subscriptionSummary) → "subscriptionSummaries", maxSize(20)) { subscriptionSummaries ⇒
          withFixture { f ⇒
            val listings = if (subscriptionSummaries.isEmpty) List(subscriptionSummaries) else subscriptionSummaries.grouped(5).toList
            listings.zipWithIndex.foreach { case (listing, i) ⇒
              (f.aws.listSubscriptionsAsync(_: aws.ListSubscriptionsRequest, _: AsyncHandler[aws.ListSubscriptionsRequest,aws.ListSubscriptionsResult]))
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
                  result.withSubscriptions(listing.map(_.toAws).asJavaCollection)
                }
            }

            val result = f.async.listSubscriptions().futureValue
            result shouldBe subscriptionSummaries
          }
        }
      }

      "by topic" in {
        val topicAndSubscriptions =
          for {
            topicArn ← SNSGen.topicArn
            subscriptionSummaries ← Gen.listOf(SNSGen.subscriptionSummary(topicArn))
          } yield (topicArn, subscriptionSummaries)
        forAll(topicAndSubscriptions → "topicAndSubscriptions", maxSize(20)) { topicAndSummaries ⇒
          val (topicArn, subscriptionSummaries) = topicAndSummaries
          withFixture { f ⇒
            val listings = if (subscriptionSummaries.isEmpty) List(subscriptionSummaries) else subscriptionSummaries.grouped(5).toList
            listings.zipWithIndex.foreach { case (listing, i) ⇒
              (f.aws.listSubscriptionsByTopicAsync(_: aws.ListSubscriptionsByTopicRequest, _: AsyncHandler[aws.ListSubscriptionsByTopicRequest,aws.ListSubscriptionsByTopicResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn == topicArn &&
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
                  result.withSubscriptions(listing.map(_.toAws).asJavaCollection)
                }
            }

            val result = f.async.listSubscriptions(topicArn).futureValue
            result shouldBe subscriptionSummaries
          }
        }
      }
    }

    "subscribe to a topic" - {
      "with a resulting subscription ARN" in {
        forAll(SNSGen.subscription → "subscription") { subscription ⇒
          val subscriptionArn = subscription.arn
          val topicArn = subscription.topicArn
          val SubscriptionEndpoint(protocol, endpoint) = subscription.endpoint
          withFixture { f ⇒
            (f.aws.subscribeAsync(_: aws.SubscribeRequest, _: AsyncHandler[aws.SubscribeRequest,aws.SubscribeResult]))
              .expects(whereRequest(r ⇒
                r.getTopicArn == topicArn &&
                  r.getProtocol == protocol.toAws &&
                  r.getEndpoint == endpoint))
              .withAwsSuccess(new aws.SubscribeResult().withSubscriptionArn(subscriptionArn))

            val result = f.async.subscribe(topicArn, protocol.toAws, endpoint).futureValue
            result shouldBe Some(subscriptionArn)
          }
        }
      }

      "without a resulting subscription ARN" in {
        forAll(SNSGen.subscription → "subscription") { subscription ⇒
          val topicArn = subscription.topicArn
          val SubscriptionEndpoint(protocol, endpoint) = subscription.endpoint
          withFixture { f ⇒
            (f.aws.subscribeAsync(_: aws.SubscribeRequest, _: AsyncHandler[aws.SubscribeRequest,aws.SubscribeResult]))
              .expects(whereRequest(r ⇒
                r.getTopicArn == topicArn &&
                  r.getProtocol == protocol.toAws &&
                  r.getEndpoint == endpoint))
              .withAwsSuccess(new aws.SubscribeResult().withSubscriptionArn("pending confirmation"))

            val result = f.async.subscribe(topicArn, protocol.toAws, endpoint).futureValue
            result shouldBe None
          }
        }
      }
    }

    "confirm a subscription" - {
      "without specifying authenticate on unsubscribe" - {
        forAll(SNSGen.topicArn → "arn", SNSGen.confirmationToken → "token", Gen.uuid → "uuid" ) { (arn, token, uuid) ⇒
          withFixture { f ⇒
            val subscriptionArn = s"$arn:$uuid"

            (f.aws.confirmSubscriptionAsync(_: aws.ConfirmSubscriptionRequest, _: AsyncHandler[aws.ConfirmSubscriptionRequest,aws.ConfirmSubscriptionResult]))
              .expects(whereRequest(r ⇒ r.getTopicArn == arn && r.getToken == token && r.getAuthenticateOnUnsubscribe == null))
              .withAwsSuccess(new aws.ConfirmSubscriptionResult().withSubscriptionArn(subscriptionArn))

            val result = f.async.confirmSubscription(arn, token).futureValue
            result shouldBe subscriptionArn
          }
        }
      }

      "with specifying authenticate on unsubscribe" - {
        forAll(
          SNSGen.topicArn → "arn",
          SNSGen.confirmationToken → "token",
          Gen.uuid → "uuid",
          SNSGen.boolean → "authoOnUnsub"
        ) { (arn, token, uuid, authOnUnsub) ⇒
          withFixture { f ⇒
            val subscriptionArn = s"$arn:$uuid"
            val authOnUnsubStr = if (authOnUnsub) "true" else "false"

            (f.aws.confirmSubscriptionAsync(_: aws.ConfirmSubscriptionRequest, _: AsyncHandler[aws.ConfirmSubscriptionRequest,aws.ConfirmSubscriptionResult]))
              .expects(whereRequest(r ⇒ r.getTopicArn == arn && r.getToken == token && r.getAuthenticateOnUnsubscribe == authOnUnsubStr))
              .withAwsSuccess(new aws.ConfirmSubscriptionResult().withSubscriptionArn(subscriptionArn))

            val result = f.async.confirmSubscription(arn, token, authOnUnsub).futureValue
            result shouldBe subscriptionArn
          }
        }
      }
    }

    "get subscription attributes" - {
      "all of them" in {
        forAll(SNSGen.subscription → "subscription") { subscription ⇒
          val arn = subscription.arn
          val attributes = subscription.attributes

          withFixture { f ⇒
            (f.aws.getSubscriptionAttributesAsync(_: aws.GetSubscriptionAttributesRequest, _: AsyncHandler[aws.GetSubscriptionAttributesRequest,aws.GetSubscriptionAttributesResult]))
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
        forAll(SNSGen.subscription → "subscription", attributeName → "attributeName") { (subscription, attributeName) ⇒
          val arn = subscription.arn
          val attributes = subscription.attributes

          withFixture { f ⇒
            (f.aws.getSubscriptionAttributesAsync(_: aws.GetSubscriptionAttributesRequest, _: AsyncHandler[aws.GetSubscriptionAttributesRequest,aws.GetSubscriptionAttributesResult]))
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
        forAll(
          SNSGen.subscriptionArn → "subscriptionArn",
          SNSGen.maybeSubscriptionDeliveryPolicy.map(_.orNull) → "deliveryPolicy"
        ) { (subscriptionArn, deliveryPolicy) ⇒
          withFixture { f ⇒
            (f.aws.setSubscriptionAttributesAsync(_: aws.SetSubscriptionAttributesRequest, _: AsyncHandler[aws.SetSubscriptionAttributesRequest,Void]))
              .expects(whereRequest(r ⇒
                r.getSubscriptionArn == subscriptionArn &&
                  r.getAttributeName == "DeliveryPolicy" &&
                  r.getAttributeValue == deliveryPolicy
              ))
              .withVoidAwsSuccess()

            val result = f.async.setSubscriptionAttribute(subscriptionArn, "DeliveryPolicy", deliveryPolicy).futureValue
            result shouldBe Done
          }
        }
      }

      "using an option" in {
        forAll(
          SNSGen.subscriptionArn → "subscriptionArn",
          SNSGen.maybeSubscriptionDeliveryPolicy → "maybeDeliveryPolicy"
        ) { (subscriptionArn, maybeDeliveryPolicy) ⇒
          withFixture { f ⇒
            (f.aws.setSubscriptionAttributesAsync(_: aws.SetSubscriptionAttributesRequest, _: AsyncHandler[aws.SetSubscriptionAttributesRequest,Void]))
              .expects(whereRequest(r ⇒
                r.getSubscriptionArn == subscriptionArn &&
                  r.getAttributeName == "DeliveryPolicy" &&
                  r.getAttributeValue == maybeDeliveryPolicy.orNull
              ))
              .withVoidAwsSuccess()

            val result = f.async.setSubscriptionAttribute(subscriptionArn, "DeliveryPolicy", maybeDeliveryPolicy).futureValue
            result shouldBe Done
          }
        }
      }
    }

    "unsubscribe" in {
      forAll(SNSGen.subscriptionArn → "subscriptionArn") { subscriptionArn ⇒
        withFixture { f ⇒
          (f.aws.unsubscribeAsync(_: aws.UnsubscribeRequest, _: AsyncHandler[aws.UnsubscribeRequest,Void]))
            .expects(whereRequest(_.getSubscriptionArn == subscriptionArn))
            .withVoidAwsSuccess()

          val result = f.async.unsubscribe(subscriptionArn).futureValue
          result shouldBe Done
        }
      }
    }

    "create platform applications" - {
      val createArgs =
        for {
          name ← SNSGen.applicationName
          credentials ← SNSGen.platformApplicationCredentials
          arn ← SNSGen.platformApplicationArn(credentials.platform, name)
        } yield (name, credentials, arn)

      "without additional attributes" in {
        forAll(createArgs) { case (name, PlatformApplicationCredentials(platform, principal, credential), arn) ⇒
          withFixture { f ⇒
            (f.aws.createPlatformApplicationAsync(_: aws.CreatePlatformApplicationRequest, _: AsyncHandler[aws.CreatePlatformApplicationRequest,aws.CreatePlatformApplicationResult]))
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
        val attributesGen = Gen.mapOf(Gen.zip(SNSGen.nonEmptyString, SNSGen.nonEmptyString))
        forAll(createArgs, attributesGen) { (createArgs, attributes) ⇒
          val (name, PlatformApplicationCredentials(platform, principal, credential), arn) = createArgs

          withFixture { f ⇒
            (f.aws.createPlatformApplicationAsync(_: aws.CreatePlatformApplicationRequest, _: AsyncHandler[aws.CreatePlatformApplicationRequest,aws.CreatePlatformApplicationResult]))
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
        forAll(SNSGen.platformApplication → "platformApplication") { platformApplication ⇒
          withFixture { f ⇒
            (f.aws.getPlatformApplicationAttributesAsync(_: aws.GetPlatformApplicationAttributesRequest, _: AsyncHandler[aws.GetPlatformApplicationAttributesRequest,aws.GetPlatformApplicationAttributesResult]))
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
          SNSGen.platformApplication → "platformApplication",
          attribute → "attribute"
        ) { (platformApplication, attribute) ⇒
          withFixture { f ⇒
            (f.aws.getPlatformApplicationAttributesAsync(_: aws.GetPlatformApplicationAttributesRequest, _: AsyncHandler[aws.GetPlatformApplicationAttributesRequest,aws.GetPlatformApplicationAttributesResult]))
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
        val attributeGen = Gen.zip(SNSGen.nonEmptyString, SNSGen.nonEmptyString)
        val attributesGen = Gen.mapOf(attributeGen)
        forAll(SNSGen.platformApplicationArn → "arn", attributesGen → "attributes") { (arn, attributes) ⇒
          withFixture { f ⇒
            (f.aws.setPlatformApplicationAttributesAsync(_: aws.SetPlatformApplicationAttributesRequest, _: AsyncHandler[aws.SetPlatformApplicationAttributesRequest, Void]))
              .expects(whereRequest(r ⇒
                r.getPlatformApplicationArn == arn &&
                  r.getAttributes.asScala == attributes))
              .withVoidAwsSuccess()

            val result = f.async.setPlatformApplicationAttributes(arn, attributes).futureValue
            result shouldBe Done
          }
        }
      }

      "using a plain value" in {
        forAll(
          SNSGen.platformApplicationArn → "arn",
          SNSGen.nonEmptyString → "name",
          SNSGen.nonEmptyString → "value"
        ) { (arn, name, value) ⇒
          withFixture { f ⇒
            (f.aws.setPlatformApplicationAttributesAsync(_: aws.SetPlatformApplicationAttributesRequest, _: AsyncHandler[aws.SetPlatformApplicationAttributesRequest, Void]))
              .expects(whereRequest(r ⇒
                r.getPlatformApplicationArn == arn &&
                  r.getAttributes.asScala == Map(name → value)))
              .withVoidAwsSuccess()

            val result = f.async.setPlatformApplicationAttribute(arn, name, value).futureValue
            result shouldBe Done
          }
        }
      }

      "using an optional value" in {
        forAll(
          SNSGen.platformApplicationArn → "arn",
          SNSGen.nonEmptyString → "name",
          Gen.option(SNSGen.nonEmptyString) → "value"
        ) { (arn, name, value) ⇒
          withFixture { f ⇒
            (f.aws.setPlatformApplicationAttributesAsync(_: aws.SetPlatformApplicationAttributesRequest, _: AsyncHandler[aws.SetPlatformApplicationAttributesRequest, Void]))
              .expects(whereRequest(r ⇒
                r.getPlatformApplicationArn == arn &&
                  r.getAttributes.asScala == Map(name → value.getOrElse(""))))
              .withVoidAwsSuccess()

            val result = f.async.setPlatformApplicationAttribute(arn, name, value).futureValue
            result shouldBe Done
          }
        }
      }
    }

    "delete platform applications" in {
      forAll(SNSGen.platformApplicationArn → "arn") { arn ⇒
        withFixture { f ⇒
          (f.aws.deletePlatformApplicationAsync(_: aws.DeletePlatformApplicationRequest, _: AsyncHandler[aws.DeletePlatformApplicationRequest, Void]))
            .expects(whereRequest(_.getPlatformApplicationArn == arn))
            .withVoidAwsSuccess()

          val result = f.async.deletePlatformApplication(arn).futureValue
          result shouldBe Done
        }
      }
    }

    "list platform applications" in {
      forAll(Gen.listOf(SNSGen.platformApplication) → "applications") { applications ⇒
        val awsApplications = applications.map { app ⇒
          new aws.PlatformApplication()
            .withPlatformApplicationArn(app.arn)
            .withAttributes(app.attributes.asJava)
        }
        withFixture { f ⇒
          val listings = if (applications.isEmpty) List(awsApplications) else awsApplications.grouped(5).toList
          listings.zipWithIndex.foreach { case (listing, i) ⇒
            (f.aws.listPlatformApplicationsAsync(_: aws.ListPlatformApplicationsRequest, _: AsyncHandler[aws.ListPlatformApplicationsRequest,aws.ListPlatformApplicationsResult]))
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
      val arns =
        for {
          platformApplicationArn ← SNSGen.platformApplicationArn
          endpointArn ← Gen.uuid.map(uuid ⇒ s"$platformApplicationArn/$uuid")
        } yield (platformApplicationArn, endpointArn)
      val token = SNSGen.nonEmptyString
      val userData = Gen.alphaStr
      val attributes = Gen.mapOf(Gen.zip(SNSGen.nonEmptyString, SNSGen.nonEmptyString))

      "using only a token" in {
        forAll(arns → "arns", token → "token") { (arns, token) ⇒
          val (applicationArn, endpointArn) = arns
          withFixture { f ⇒
            (f.aws.createPlatformEndpointAsync(_: aws.CreatePlatformEndpointRequest, _: AsyncHandler[aws.CreatePlatformEndpointRequest,aws.CreatePlatformEndpointResult]))
              .expects(whereRequest(r ⇒
                r.getPlatformApplicationArn == applicationArn &&
                r.getToken == token &&
                r.getCustomUserData == null &&
                r.getAttributes.isEmpty))
              .withAwsSuccess(new aws.CreatePlatformEndpointResult().withEndpointArn(endpointArn))

            val result = f.async.createPlatformEndpoint(applicationArn, token).futureValue
            result shouldBe endpointArn
          }
        }
      }

      "using a token and user data" in {
        forAll(arns → "arns", token → "token", userData → "userData") { (arns, token, userData) ⇒
          val (applicationArn, endpointArn) = arns
          withFixture { f ⇒
            (f.aws.createPlatformEndpointAsync(_: aws.CreatePlatformEndpointRequest, _: AsyncHandler[aws.CreatePlatformEndpointRequest,aws.CreatePlatformEndpointResult]))
              .expects(whereRequest(r ⇒
                r.getPlatformApplicationArn == applicationArn &&
                  r.getToken == token &&
                  r.getCustomUserData == userData &&
                  r.getAttributes.isEmpty))
              .withAwsSuccess(new aws.CreatePlatformEndpointResult().withEndpointArn(endpointArn))

            val result = f.async.createPlatformEndpoint(applicationArn, token, userData).futureValue
            result shouldBe endpointArn
          }
        }
      }

      "using a token and attributes" in {
        forAll(arns → "arns", token → "token", attributes → "attributes") { (arns, token, attributes) ⇒
          val (applicationArn, endpointArn) = arns
          withFixture { f ⇒
            (f.aws.createPlatformEndpointAsync(_: aws.CreatePlatformEndpointRequest, _: AsyncHandler[aws.CreatePlatformEndpointRequest,aws.CreatePlatformEndpointResult]))
              .expects(whereRequest(r ⇒
                r.getPlatformApplicationArn == applicationArn &&
                  r.getToken == token &&
                  r.getCustomUserData == null &&
                  r.getAttributes.asScala == attributes))
              .withAwsSuccess(new aws.CreatePlatformEndpointResult().withEndpointArn(endpointArn))

            val result = f.async.createPlatformEndpoint(applicationArn, token, attributes).futureValue
            result shouldBe endpointArn
          }
        }
      }

      "using a token, attributes, and user data" in {
        forAll(arns → "arns", token → "token", userData → "userData", attributes → "attributes") { (arns, token, userData, attributes) ⇒
          val (applicationArn, endpointArn) = arns
          withFixture { f ⇒
            (f.aws.createPlatformEndpointAsync(_: aws.CreatePlatformEndpointRequest, _: AsyncHandler[aws.CreatePlatformEndpointRequest,aws.CreatePlatformEndpointResult]))
              .expects(whereRequest(r ⇒
                r.getPlatformApplicationArn == applicationArn &&
                  r.getToken == token &&
                  r.getCustomUserData == userData &&
                  r.getAttributes.asScala == attributes))
              .withAwsSuccess(new aws.CreatePlatformEndpointResult().withEndpointArn(endpointArn))

            val result = f.async.createPlatformEndpoint(applicationArn, token, userData, attributes).futureValue
            result shouldBe endpointArn
          }
        }
      }
    }

    "get platform endpoint attributes" - {
      val attributes =
        for {
          enabled ← SNSGen.boolean
          userData ← Gen.option(Gen.alphaStr)
          token ← SNSGen.nonEmptyString
        } yield {
          userData.map(x ⇒ Map("CustomUserData" → x)).getOrElse(Map.empty) +
            ("Enabled" → enabled.toString) +
            ("Token" → token)
        }

      "all of them" in {
        forAll(SNSGen.platformEndpointArn → "endpointArn", attributes → "attributes") { (endpointArn, attributes) ⇒
          withFixture { f ⇒
            (f.aws.getEndpointAttributesAsync(_: aws.GetEndpointAttributesRequest, _: AsyncHandler[aws.GetEndpointAttributesRequest,aws.GetEndpointAttributesResult]))
              .expects(whereRequest(_.getEndpointArn == endpointArn))
              .withAwsSuccess(new aws.GetEndpointAttributesResult().withAttributes(attributes.asJava))

            val result = f.async.getPlatformEndpointAttributes(endpointArn).futureValue
            result shouldBe attributes
          }
        }
      }

      "just one of them" in {
        val attributeName = Gen.oneOf("CustomUserData", "Enabled", "Token")
        forAll(
          SNSGen.platformEndpointArn → "endpointArn",
          attributes → "attributes",
          attributeName → "attributeName"
        ) { (endpointArn, attributes, attributeName) ⇒
          withFixture { f ⇒
            (f.aws.getEndpointAttributesAsync(_: aws.GetEndpointAttributesRequest, _: AsyncHandler[aws.GetEndpointAttributesRequest,aws.GetEndpointAttributesResult]))
              .expects(whereRequest(_.getEndpointArn == endpointArn))
              .withAwsSuccess(new aws.GetEndpointAttributesResult().withAttributes(attributes.asJava))

            val result = f.async.getPlatformEndpointAttribute(endpointArn, attributeName).futureValue
            result shouldBe attributes.get(attributeName)
          }
        }
      }
    }

    "set platform endpoint attributes using" - {
      "plain values" in {
        forAll(
          SNSGen.platformEndpointArn → "endpointArn",
          SNSGen.nonEmptyString → "attributeName",
          Gen.alphaStr → "attributeValue"
        ) { (endpointArn, attributeName, attributeValue) ⇒
          withFixture { f ⇒
            (f.aws.setEndpointAttributesAsync(_: aws.SetEndpointAttributesRequest, _: AsyncHandler[aws.SetEndpointAttributesRequest,Void]))
              .expects(whereRequest(r ⇒
                r.getEndpointArn == endpointArn &&
                  r.getAttributes.asScala == Map(attributeName → attributeValue)))
              .withVoidAwsSuccess()

            val result = f.async.setPlatformEndpointAttributes(endpointArn, attributeName, attributeValue).futureValue
            result shouldBe Done
          }
        }
      }

      "options" in {
        forAll(
          SNSGen.platformEndpointArn → "endpointArn",
          SNSGen.nonEmptyString → "attributeName",
          Gen.option(Gen.alphaStr) → "attributeValue"
        ) { (endpointArn, attributeName, attributeValue) ⇒
          withFixture { f ⇒
            (f.aws.setEndpointAttributesAsync(_: aws.SetEndpointAttributesRequest, _: AsyncHandler[aws.SetEndpointAttributesRequest,Void]))
              .expects(whereRequest(r ⇒
                r.getEndpointArn == endpointArn &&
                  r.getAttributes.asScala == Map(attributeName → attributeValue.getOrElse(""))))
              .withVoidAwsSuccess()

            val result = f.async.setPlatformEndpointAttributes(endpointArn, attributeName, attributeValue).futureValue
            result shouldBe Done
          }
        }
      }

      "bulk updates" in {
        forAll(
          SNSGen.platformEndpointArn → "endpointArn",
          Gen.mapOf(Gen.zip(SNSGen.nonEmptyString, Gen.alphaStr)) → "attributes"
        ) { (endpointArn, attributes) ⇒
          withFixture { f ⇒
            (f.aws.setEndpointAttributesAsync(_: aws.SetEndpointAttributesRequest, _: AsyncHandler[aws.SetEndpointAttributesRequest,Void]))
              .expects(whereRequest(r ⇒
                r.getEndpointArn == endpointArn &&
                  r.getAttributes.asScala == attributes))
              .withVoidAwsSuccess()

            val result = f.async.setPlatformEndpointAttributes(endpointArn, attributes).futureValue
            result shouldBe Done
          }
        }
      }
    }

    "delete platform endpoints" in {
      forAll(SNSGen.platformEndpointArn → "arn") { arn ⇒
        withFixture { f ⇒
          (f.aws.deleteEndpointAsync(_: aws.DeleteEndpointRequest, _: AsyncHandler[aws.DeleteEndpointRequest,Void]))
            .expects(whereRequest(r ⇒ r.getEndpointArn == arn))
            .withVoidAwsSuccess()

          val result = f.async.deletePlatformEndpoint(arn).futureValue
          result shouldBe Done
        }
      }
    }

    "list platform endpoints" in {
      val argsGen =
        for {
          applicationArn ← SNSGen.platformApplicationArn
          endpoints ← Gen.listOf(SNSGen.platformEndpoint(applicationArn))
        } yield (applicationArn, endpoints)
      forAll(argsGen) { args ⇒
        val (applicationArn, endpoints) = args

        val awsEndpoints = endpoints.map(pe ⇒ new aws.Endpoint().withEndpointArn(pe.arn).withAttributes(pe.attributes.asJava))
        withFixture { f ⇒
          val listings = if (endpoints.isEmpty) List(awsEndpoints) else awsEndpoints.grouped(5).toList
          listings.zipWithIndex.foreach { case (listing, i) ⇒
            (f.aws.listEndpointsByPlatformApplicationAsync(_: aws.ListEndpointsByPlatformApplicationRequest, _: AsyncHandler[aws.ListEndpointsByPlatformApplicationRequest,aws.ListEndpointsByPlatformApplicationResult]))
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

          val result = f.async.listPlatformEndpoints(applicationArn).futureValue
          result shouldBe endpoints
        }
      }
    }

    "publish" - {
      val targetArn = Gen.oneOf(SNSGen.topicArn, SNSGen.platformEndpointArn)
      def toAws(attributes: Map[String,MessageAttributeValue[_]]) = attributes.mapValues(_.toAws)

      "simple messages" - {
        "using a target and a message" in {
          forAll(
            targetArn → "targetArn",
            SNSGen.nonEmptyString → "message",
            SNSGen.messageId → "messageId"
          ) { (targetArn, message, messageId) ⇒
            withFixture { f ⇒
              (f.aws.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn
                  r.getMessage shouldBe message
                  r.getMessageStructure shouldBe null
                  r.getSubject shouldBe null
                  r.getMessageAttributes.asScala shouldBe Map.empty
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn, message).futureValue
              result shouldBe messageId
            }
          }
        }

        "using a target, a message, and a subject" in {
          forAll(
            targetArn → "targetArn",
            SNSGen.nonEmptyString → "message",
            SNSGen.nonEmptyString → "subject",
            SNSGen.messageId → "messageId"
          ) { (targetArn, message, subject, messageId) ⇒
            withFixture { f ⇒
              (f.aws.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn
                  r.getMessage shouldBe message
                  r.getMessageStructure shouldBe null
                  r.getSubject shouldBe subject
                  r.getMessageAttributes.asScala shouldBe Map.empty
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn, message, subject).futureValue
              result shouldBe messageId
            }
          }
        }

        "using a target, a message, and some attributes" in {
          forAll(
            targetArn → "targetArn",
            SNSGen.nonEmptyString → "message",
            SNSGen.messageAttributes → "attributes",
            SNSGen.messageId → "messageId"
          ) { (targetArn, message, attributes, messageId) ⇒
            withFixture { f ⇒
              (f.aws.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn
                  r.getMessage shouldBe message
                  r.getMessageStructure shouldBe null
                  r.getSubject shouldBe null
                  r.getMessageAttributes.asScala shouldBe toAws(attributes)
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn, message, attributes).futureValue
              result shouldBe messageId
            }
          }
        }

        "using a target, a message, a subject, and some attributes" in {
          forAll(
            targetArn → "targetArn",
            SNSGen.nonEmptyString → "message",
            SNSGen.nonEmptyString → "subject",
            SNSGen.messageAttributes → "attributes",
            SNSGen.messageId → "messageId"
          ) { (targetArn, message, subject, attributes, messageId) ⇒
            withFixture { f ⇒
              (f.aws.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn
                  r.getMessage shouldBe message
                  r.getMessageStructure shouldBe null
                  r.getSubject shouldBe subject
                  r.getMessageAttributes.asScala shouldBe toAws(attributes)
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn, message, subject, attributes).futureValue
              result shouldBe messageId
            }
          }
        }
      }

      "compound messages" - {
        "using a target and a message" in {
          forAll(
            targetArn → "targetArn",
            SNSGen.messageMap → "message",
            SNSGen.messageId → "messageId"
          ) { (targetArn, message, messageId) ⇒
            withFixture { f ⇒
              (f.aws.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn
                  JsonParser(r.getMessage) shouldBe JsObject(message.mapValues(JsString(_)))
                  r.getMessageStructure shouldBe "json"
                  r.getSubject shouldBe null
                  r.getMessageAttributes.asScala shouldBe Map.empty
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn, message).futureValue
              result shouldBe messageId
            }
          }
        }

        "using a target, a message, and a subject" in {
          forAll(
            targetArn → "targetArn",
            SNSGen.messageMap → "message",
            SNSGen.nonEmptyString → "subject",
            SNSGen.messageId → "messageId"
          ) { (targetArn, message, subject, messageId) ⇒
            withFixture { f ⇒
              (f.aws.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn
                  JsonParser(r.getMessage) shouldBe JsObject(message.mapValues(JsString(_)))
                  r.getMessageStructure shouldBe "json"
                  r.getSubject shouldBe subject
                  r.getMessageAttributes.asScala shouldBe Map.empty
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn, message, subject).futureValue
              result shouldBe messageId
            }
          }
        }

        "using a target, a message, and some attributes" in {
          forAll(
            targetArn → "targetArn",
            SNSGen.messageMap → "message",
            SNSGen.messageAttributes → "attributes",
            SNSGen.messageId → "messageId"
          ) { (targetArn, message, attributes, messageId) ⇒
            withFixture { f ⇒
              (f.aws.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn
                  JsonParser(r.getMessage) shouldBe JsObject(message.mapValues(JsString(_)))
                  r.getMessageStructure shouldBe "json"
                  r.getSubject shouldBe null
                  r.getMessageAttributes.asScala shouldBe toAws(attributes)
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn, message, attributes).futureValue
              result shouldBe messageId
            }
          }
        }

        "using a target, a message, a subject, and some attributes" in {
          forAll(
            targetArn → "targetArn",
            SNSGen.messageMap → "message",
            SNSGen.nonEmptyString → "subject",
            SNSGen.messageAttributes → "attributes",
            SNSGen.messageId → "messageId"
          ) { (targetArn, message, subject, attributes, messageId) ⇒
            withFixture { f ⇒
              (f.aws.publishAsync(_: aws.PublishRequest, _: AsyncHandler[aws.PublishRequest,aws.PublishResult]))
                .expects(whereRequest { r ⇒
                  r.getTopicArn shouldBe null
                  r.getTargetArn shouldBe targetArn
                  JsonParser(r.getMessage) shouldBe JsObject(message.mapValues(JsString(_)))
                  r.getMessageStructure shouldBe "json"
                  r.getSubject shouldBe subject
                  r.getMessageAttributes.asScala shouldBe toAws(attributes)
                  true
                })
                .withAwsSuccess(new aws.PublishResult().withMessageId(messageId))

              val result = f.async.publish(targetArn, message, subject, attributes).futureValue
              result shouldBe messageId
            }
          }
        }
      }
    }
  }

  private case class Fixture(aws: AmazonSNSAsync,
                             async: AsyncSNSClient)

  private def withFixture(test: Fixture ⇒ Unit): Unit = {
    val aws = mock[AmazonSNSAsync]("aws")
    val streaming = new DefaultStreamingSNSClient(aws)
    val async = new DefaultAsyncSNSClient(streaming)
    test(Fixture(aws, async))
  }
}
