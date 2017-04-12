package com.monsanto.arch.awsutil.sns

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.sns.{AmazonSNSAsync, model ⇒ aws}
import com.monsanto.arch.awsutil.sns.model.AwsConverters._
import com.monsanto.arch.awsutil.sns.model._
import com.monsanto.arch.awsutil._

import scala.collection.JavaConverters._

private[awsutil] final class DefaultStreamingSNSClient(sns: AmazonSNSAsync) extends StreamingSNSClient {
  override val topicCreator =
    Flow[String]
      .map(name ⇒ new aws.CreateTopicRequest(name))
      .via(AWSFlow.simple[aws.CreateTopicRequest,aws.CreateTopicResult](sns.createTopicAsync))
      .map(_.getTopicArn)
      .named("SNS.topicCreator")

  override val topicDeleter =
    Flow[String]
      .map(arn ⇒ new aws.DeleteTopicRequest(arn))
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput[aws.DeleteTopicRequest, aws.DeleteTopicResult](sns.deleteTopicAsync)))
      .map(_.getTopicArn)
      .named("SNS.topicDeleter")

  override val topicLister =
    Source.single(new aws.ListTopicsRequest())
      .map(_.clone())
      .via(AWSFlow.pagedByNextToken[aws.ListTopicsRequest,aws.ListTopicsResult](sns.listTopicsAsync))
      .mapConcat(_.getTopics.asScala.toList)
      .map(_.getTopicArn)
      .named("SNS.topicLister")

  override val subscriber =
    Flow[SubscribeRequest]
      .map(_.toAws)
      .via[aws.SubscribeResult,NotUsed](AWSFlow.simple(sns.subscribeAsync))
      .map(result ⇒ Option(result.getSubscriptionArn).flatMap(s ⇒ if (s.startsWith("arn:")) Some(s) else None))
      .named("SNS.subscriber")

  override val topicAttributesGetter =
    Flow[String]
      .map(arn ⇒ new aws.GetTopicAttributesRequest(arn))
      .via(AWSFlow.simple[aws.GetTopicAttributesRequest,aws.GetTopicAttributesResult](sns.getTopicAttributesAsync))
      .map(_.getAttributes.asScala.toMap)
      .named("SNS.topicAttributeGetter")

  override val topicAttributeSetter =
    Flow[SetTopicAttributesRequest]
      .map(_.toAws)
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput[aws.SetTopicAttributesRequest,aws.SetTopicAttributesResult](sns.setTopicAttributesAsync)))
      .map(_.getTopicArn)
      .named("SNS.topicAttributeSetter")

  override val permissionAdder =
    Flow[AddPermissionRequest]
      .map(_.asAws)
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput[aws.AddPermissionRequest,aws.AddPermissionResult](sns.addPermissionAsync)))
      .map(_.getTopicArn)
      .named("SNS.permissionAdder")

  override val permissionRemover =
    Flow[RemovePermissionRequest]
      .map(_.asAws)
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput[aws.RemovePermissionRequest,aws.RemovePermissionResult](sns.removePermissionAsync)))
      .map(_.getTopicArn)
      .named("SNS.permissionRemover")

  private val allSubscriptionsLister =
    Flow[aws.ListSubscriptionsRequest]
      .via(AWSFlow.pagedByNextToken[aws.ListSubscriptionsRequest,aws.ListSubscriptionsResult](sns.listSubscriptionsAsync))
      .mapConcat(_.getSubscriptions.asScala.toList)
      .named("SNS.allSubscriptionsLister")

  private val byTopicSubscriptionsLister =
    Flow[aws.ListSubscriptionsByTopicRequest]
      .via(AWSFlow.pagedByNextToken[aws.ListSubscriptionsByTopicRequest,aws.ListSubscriptionsByTopicResult](sns.listSubscriptionsByTopicAsync))
      .mapConcat(_.getSubscriptions.asScala.toList)
      .named("SNS.topicSubscriptions")

  override val subscriptionLister =
    Flow[ListSubscriptionsRequest]
      .map(_.toEitherAws)
      .flatMapConcat {
        case Left(listAllRequest)      ⇒ Source.single(listAllRequest).via(allSubscriptionsLister)
        case Right(listByTopicRequest) ⇒ Source.single(listByTopicRequest).via(byTopicSubscriptionsLister)
      }
      .map( sns ⇒
        SubscriptionSummary(
          if (sns.getSubscriptionArn.startsWith("arn")) Some(sns.getSubscriptionArn) else None,
          sns.getTopicArn,
          sns.getProtocol.asScala(sns.getEndpoint),
          sns.getOwner))
      .named("SNS.subscriptionLister")

  override val subscriptionConfirmer =
    Flow[ConfirmSubscriptionRequest]
      .map(_.asAws)
      .via[aws.ConfirmSubscriptionResult,NotUsed](AWSFlow.simple(sns.confirmSubscriptionAsync))
      .map(_.getSubscriptionArn)
      .named("SNS.subscriptionConfirmer")

  override val subscriptionAttributesGetter =
    Flow[String]
      .map(arn ⇒ new aws.GetSubscriptionAttributesRequest(arn))
      .via(AWSFlow.simple[aws.GetSubscriptionAttributesRequest,aws.GetSubscriptionAttributesResult](sns.getSubscriptionAttributesAsync))
      .map(_.getAttributes.asScala.toMap)
      .named("SNS.subscriptionAttributesGetter")

  override val subscriptionAttributeSetter =
    Flow[SetSubscriptionAttributesRequest]
      .map(_.toAws)
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput[aws.SetSubscriptionAttributesRequest,aws.SetSubscriptionAttributesResult](sns.setSubscriptionAttributesAsync)))
      .map(_.getSubscriptionArn)
      .named("SNS.subscriptionAttributesSetter")

  override val unsubscriber =
    Flow[String]
      .map(arn ⇒ new aws.UnsubscribeRequest(arn))
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput[aws.UnsubscribeRequest,aws.UnsubscribeResult](sns.unsubscribeAsync)))
      .map(_.getSubscriptionArn)
      .named("SNS.unsubscriber")

  override val platformApplicationCreator =
    Flow[CreatePlatformApplicationRequest]
      .map(_.toAws)
      .via(AWSFlow.simple[aws.CreatePlatformApplicationRequest,aws.CreatePlatformApplicationResult](sns.createPlatformApplicationAsync))
      .map(_.getPlatformApplicationArn)
      .named("SNS.platformApplicationCreator")

  override val platformApplicationAttributesGetter =
    Flow[String]
      .map(arn ⇒ new aws.GetPlatformApplicationAttributesRequest().withPlatformApplicationArn(arn))
      .via[aws.GetPlatformApplicationAttributesResult,NotUsed](AWSFlow.simple(sns.getPlatformApplicationAttributesAsync))
      .map(_.getAttributes.asScala.toMap)
      .named("SNS.platformApplicationAttributeGetter")

  override val platformApplicationAttributesSetter =
    Flow[SetPlatformApplicationAttributesRequest]
      .map(_.toAws)
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput[aws.SetPlatformApplicationAttributesRequest,aws.SetPlatformApplicationAttributesResult](sns.setPlatformApplicationAttributesAsync)))
      .map(_.getPlatformApplicationArn)
      .named("SNS.platformApplicationAttributesSetter")

  override val platformApplicationDeleter =
    Flow[String]
      .map(arn ⇒ new aws.DeletePlatformApplicationRequest().withPlatformApplicationArn(arn))
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput[aws.DeletePlatformApplicationRequest,aws.DeletePlatformApplicationResult](sns.deletePlatformApplicationAsync)))
      .map(_.getPlatformApplicationArn)
      .named("SNS.platformApplicationDeleter")

  override val platformApplicationLister =
    Source.single(new aws.ListPlatformApplicationsRequest)
      .map(_.clone())
      .via[aws.ListPlatformApplicationsResult,NotUsed](AWSFlow.pagedByNextToken(sns.listPlatformApplicationsAsync))
      .mapConcat(_.getPlatformApplications.asScala.toList)
      .map(sns ⇒ PlatformApplication(sns.getPlatformApplicationArn, sns.getAttributes.asScala.toMap))
      .named("SNS.platformApplicationLister")

  override val platformEndpointCreator =
    Flow[CreatePlatformEndpointRequest]
      .map { r ⇒
        val request = new aws.CreatePlatformEndpointRequest
        request.setPlatformApplicationArn(r.platformApplicationArn)
        request.setToken(r.token)
        r.customUserData.foreach(d ⇒ request.setCustomUserData(d))
        if (r.attributes.nonEmpty) {
          request.setAttributes(r.attributes.asJava)
        }
        request
      }
      .via[aws.CreatePlatformEndpointResult,NotUsed](AWSFlow.simple(sns.createPlatformEndpointAsync))
      .map(_.getEndpointArn)
      .named("SNS.platformEndpointCreator")

  override val platformEndpointAttributesGetter =
    Flow[String]
      .map(arn ⇒ new aws.GetEndpointAttributesRequest().withEndpointArn(arn))
      .via[aws.GetEndpointAttributesResult, NotUsed](AWSFlow.simple(sns.getEndpointAttributesAsync))
      .map(_.getAttributes.asScala.toMap)
      .named("SNS.platformEndpointAttributesGetter")

  override val platformEndpointAttributesSetter =
    Flow[SetPlatformEndpointAttributesRequest]
      .map(r ⇒ new aws.SetEndpointAttributesRequest().withEndpointArn(r.platformEndpointArn).withAttributes(r.attributes.asJava))
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput[aws.SetEndpointAttributesRequest,aws.SetEndpointAttributesResult](sns.setEndpointAttributesAsync)))
      .map(_.getEndpointArn)
      .named("SNS.platformEndpointAttributesSetter")

  override val platformEndpointDeleter =
    Flow[String]
      .map(arn ⇒ new aws.DeleteEndpointRequest().withEndpointArn(arn))
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput[aws.DeleteEndpointRequest,aws.DeleteEndpointResult](sns.deleteEndpointAsync)))
      .map(_.getEndpointArn)
      .named("SNS.platformEndpointDeleter")

  override val platformEndpointLister =
    Flow[String]
      .map(arn ⇒ new aws.ListEndpointsByPlatformApplicationRequest().withPlatformApplicationArn(arn))
      .via[aws.ListEndpointsByPlatformApplicationResult,NotUsed](AWSFlow.pagedByNextToken(sns.listEndpointsByPlatformApplicationAsync))
      .mapConcat(_.getEndpoints.asScala.toList)
      .map(_.asScala)
      .named("SNS.listPlatformEndpoints")

  override val publisher =
    Flow[PublishRequest]
      .map { r ⇒
        val request = new aws.PublishRequest()
        request.setTargetArn(r.targetArn)
        request.setMessage(r.message)
        r.subject.foreach(s ⇒ request.setSubject(s))
        if (r.attributes.nonEmpty) {
          request.setMessageAttributes(r.attributes.asAws)
        }
        r.messageStructure.foreach(s ⇒ request.setMessageStructure(s))
        request
      }
      .via[aws.PublishResult,NotUsed](AWSFlow.simple(sns.publishAsync))
      .map(_.getMessageId)
      .named("SNS.publisher")
}
