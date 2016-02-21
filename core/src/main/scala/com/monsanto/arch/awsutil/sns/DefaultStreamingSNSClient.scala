package com.monsanto.arch.awsutil.sns

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.sns.{AmazonSNSAsync, model ⇒ am}
import com.monsanto.arch.awsutil.sns.model._
import com.monsanto.arch.awsutil.{AWSFlow, AWSFlowAdapter}

import scala.collection.JavaConverters._

private[awsutil] final class DefaultStreamingSNSClient(aws: AmazonSNSAsync) extends StreamingSNSClient {
  override val topicCreator =
    Flow[String]
      .map(name ⇒ new am.CreateTopicRequest(name))
      .via(AWSFlow.simple[am.CreateTopicRequest,am.CreateTopicResult](aws.createTopicAsync))
      .map(_.getTopicArn)
      .named("SNS.topicCreator")

  override val topicDeleter =
    Flow[String]
      .map(arn ⇒ new am.DeleteTopicRequest(arn))
      .via(AWSFlow.simple(AWSFlowAdapter.devoid[am.DeleteTopicRequest](aws.deleteTopicAsync)))
      .map(_.getTopicArn)
      .named("SNS.topicDeleter")

  override val topicLister =
    Source.single(new am.ListTopicsRequest())
      .map(_.clone())
      .via(AWSFlow.pagedByNextToken[am.ListTopicsRequest,am.ListTopicsResult](aws.listTopicsAsync))
      .mapConcat(_.getTopics.asScala.toList)
      .map(_.getTopicArn)
      .named("SNS.topicLister")

  override val subscriber =
    Flow[SubscribeRequest]
      .map(_.toAws)
      .via[am.SubscribeResult,NotUsed](AWSFlow.simple(aws.subscribeAsync))
      .map(result ⇒ Option(result.getSubscriptionArn).flatMap(s ⇒ if (s.startsWith("arn:aws:sns")) Some(s) else None))
      .named("SNS.subscriber")

  override val topicAttributesGetter =
    Flow[String]
      .map(arn ⇒ new am.GetTopicAttributesRequest(arn))
      .via(AWSFlow.simple[am.GetTopicAttributesRequest,am.GetTopicAttributesResult](aws.getTopicAttributesAsync))
      .map(_.getAttributes.asScala.toMap)
      .named("SNS.topicAttributeGetter")

  override val topicAttributeSetter =
    Flow[SetTopicAttributesRequest]
      .map(_.toAws)
      .via(AWSFlow.simple(AWSFlowAdapter.devoid[am.SetTopicAttributesRequest](aws.setTopicAttributesAsync)))
      .map(_.getTopicArn)
      .named("SNS.topicAttributeSetter")

  override val permissionAdder =
    Flow[AddPermissionRequest]
      .map(_.toAws)
      .via(AWSFlow.simple(AWSFlowAdapter.devoid[am.AddPermissionRequest](aws.addPermissionAsync)))
      .map(_.getTopicArn)
      .named("SNS.permissionAdder")

  override val permissionRemover =
    Flow[RemovePermissionRequest]
      .map(_.toAws)
      .via(AWSFlow.simple(AWSFlowAdapter.devoid[am.RemovePermissionRequest](aws.removePermissionAsync)))
      .map(_.getTopicArn)
      .named("SNS.permissionRemover")

  private val allSubscriptionsLister =
    Flow[am.ListSubscriptionsRequest]
      .via(AWSFlow.pagedByNextToken[am.ListSubscriptionsRequest,am.ListSubscriptionsResult](aws.listSubscriptionsAsync))
      .mapConcat(_.getSubscriptions.asScala.toList)
      .named("SNS.allSubscriptionsLister")

  private val byTopicSubscriptionsLister =
    Flow[am.ListSubscriptionsByTopicRequest]
      .via(AWSFlow.pagedByNextToken[am.ListSubscriptionsByTopicRequest,am.ListSubscriptionsByTopicResult](aws.listSubscriptionsByTopicAsync))
      .mapConcat(_.getSubscriptions.asScala.toList)
      .named("SNS.topicSubscriptions")

  override val subscriptionLister =
    Flow[ListSubscriptionsRequest]
      .map(_.toEitherAws)
      .flatMapConcat {
        case Left(listAllRequest)      ⇒ Source.single(listAllRequest).via(allSubscriptionsLister)
        case Right(listByTopicRequest) ⇒ Source.single(listByTopicRequest).via(byTopicSubscriptionsLister)
      }
      .map( aws ⇒
        SubscriptionSummary(
          if (aws.getSubscriptionArn.startsWith("arn")) Some(aws.getSubscriptionArn) else None,
          aws.getTopicArn,
          Protocol(aws.getProtocol)(aws.getEndpoint),
          aws.getOwner))
      .named("SNS.subscriptionLister")

  override val subscriptionConfirmer =
    Flow[ConfirmSubscriptionRequest]
      .map(_.toAws)
      .via[am.ConfirmSubscriptionResult,NotUsed](AWSFlow.simple(aws.confirmSubscriptionAsync))
      .map(_.getSubscriptionArn)
      .named("SNS.subscriptionConfirmer")

  override val subscriptionAttributesGetter =
    Flow[String]
      .map(arn ⇒ new am.GetSubscriptionAttributesRequest(arn))
      .via(AWSFlow.simple[am.GetSubscriptionAttributesRequest,am.GetSubscriptionAttributesResult](aws.getSubscriptionAttributesAsync))
      .map(_.getAttributes.asScala.toMap)
      .named("SNS.subscriptionAttributesGetter")

  override val subscriptionAttributeSetter =
    Flow[SetSubscriptionAttributesRequest]
      .map(_.toAws)
      .via(AWSFlow.simple(AWSFlowAdapter.devoid[am.SetSubscriptionAttributesRequest](aws.setSubscriptionAttributesAsync)))
      .map(_.getSubscriptionArn)
      .named("SNS.subscriptionAttributesSetter")

  override val unsubscriber =
    Flow[String]
      .map(arn ⇒ new am.UnsubscribeRequest(arn))
      .via(AWSFlow.simple(AWSFlowAdapter.devoid[am.UnsubscribeRequest](aws.unsubscribeAsync)))
      .map(_.getSubscriptionArn)
      .named("SNS.unsubscriber")

  override val platformApplicationCreator =
    Flow[CreatePlatformApplicationRequest]
      .map(_.toAws)
      .via(AWSFlow.simple[am.CreatePlatformApplicationRequest,am.CreatePlatformApplicationResult](aws.createPlatformApplicationAsync))
      .map(_.getPlatformApplicationArn)
      .named("SNS.platformApplicationCreator")

  override val platformApplicationAttributesGetter =
    Flow[String]
      .map(arn ⇒ new am.GetPlatformApplicationAttributesRequest().withPlatformApplicationArn(arn))
      .via[am.GetPlatformApplicationAttributesResult,NotUsed](AWSFlow.simple(aws.getPlatformApplicationAttributesAsync))
      .map(_.getAttributes.asScala.toMap)
      .named("SNS.platformApplicationAttributeGetter")

  override val platformApplicationAttributesSetter =
    Flow[SetPlatformApplicationAttributesRequest]
      .map(_.toAws)
      .via(AWSFlow.simple(AWSFlowAdapter.devoid[am.SetPlatformApplicationAttributesRequest](aws.setPlatformApplicationAttributesAsync)))
      .map(_.getPlatformApplicationArn)
      .named("SNS.platformApplicationAttributesSetter")

  override val platformApplicationDeleter =
    Flow[String]
      .map(arn ⇒ new am.DeletePlatformApplicationRequest().withPlatformApplicationArn(arn))
      .via(AWSFlow.simple(AWSFlowAdapter.devoid(aws.deletePlatformApplicationAsync)))
      .map(_.getPlatformApplicationArn)
      .named("SNS.platformApplicationDeleter")

  override val platformApplicationLister =
    Source.single(new am.ListPlatformApplicationsRequest)
      .map(_.clone())
      .via[am.ListPlatformApplicationsResult,NotUsed](AWSFlow.pagedByNextToken(aws.listPlatformApplicationsAsync))
      .mapConcat(_.getPlatformApplications.asScala.toList)
      .map(aws ⇒ PlatformApplication(aws.getPlatformApplicationArn, aws.getAttributes.asScala.toMap))
      .named("SNS.platformApplicationLister")

  override val platformEndpointCreator =
    Flow[CreatePlatformEndpointRequest]
      .map { r ⇒
        val request = new am.CreatePlatformEndpointRequest
        request.setPlatformApplicationArn(r.platformApplicationArn)
        request.setToken(r.token)
        r.customUserData.foreach(d ⇒ request.setCustomUserData(d))
        if (r.attributes.nonEmpty) {
          request.setAttributes(r.attributes.asJava)
        }
        request
      }
      .via[am.CreatePlatformEndpointResult,NotUsed](AWSFlow.simple(aws.createPlatformEndpointAsync))
      .map(_.getEndpointArn)
      .named("SNS.platformEndpointCreator")

  override val platformEndpointAttributesGetter =
    Flow[String]
      .map(arn ⇒ new am.GetEndpointAttributesRequest().withEndpointArn(arn))
      .via[am.GetEndpointAttributesResult, NotUsed](AWSFlow.simple(aws.getEndpointAttributesAsync))
      .map(_.getAttributes.asScala.toMap)
      .named("SNS.platformEndpointAttributesGetter")

  override val platformEndpointAttributesSetter =
    Flow[SetPlatformEndpointAttributesRequest]
      .map(r ⇒ new am.SetEndpointAttributesRequest().withEndpointArn(r.platformEndpointArn).withAttributes(r.attributes.asJava))
      .via(AWSFlow.simple(AWSFlowAdapter.devoid(aws.setEndpointAttributesAsync)))
      .map(_.getEndpointArn)
      .named("SNS.platformEndpointAttributesSetter")

  override val platformEndpointDeleter =
    Flow[String]
      .map(arn ⇒ new am.DeleteEndpointRequest().withEndpointArn(arn))
      .via(AWSFlow.simple(AWSFlowAdapter.devoid(aws.deleteEndpointAsync)))
      .map(_.getEndpointArn)
      .named("SNS.platformEndpointDeleter")

  override val platformEndpointLister =
    Flow[String]
      .map(arn ⇒ new am.ListEndpointsByPlatformApplicationRequest().withPlatformApplicationArn(arn))
      .via[am.ListEndpointsByPlatformApplicationResult,NotUsed](AWSFlow.pagedByNextToken(aws.listEndpointsByPlatformApplicationAsync))
      .mapConcat(_.getEndpoints.asScala.toList)
      .map(e ⇒ PlatformEndpoint(e))
      .named("SNS.listPlatformEndpoints")

  override val publisher =
    Flow[PublishRequest]
      .map { r ⇒
        val request = new am.PublishRequest()
        request.setTargetArn(r.targetArn)
        request.setMessage(r.message)
        r.subject.foreach(s ⇒ request.setSubject(s))
        if (r.attributes.nonEmpty) {
          request.setMessageAttributes(r.attributes.mapValues(_.toAws).asJava)
        }
        r.messageStructure.foreach(s ⇒ request.setMessageStructure(s))
        request
      }
      .via[am.PublishResult,NotUsed](AWSFlow.simple(aws.publishAsync))
      .map(_.getMessageId)
      .named("SNS.publisher")
}
