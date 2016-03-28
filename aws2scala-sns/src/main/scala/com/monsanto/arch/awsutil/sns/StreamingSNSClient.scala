package com.monsanto.arch.awsutil.sns

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.monsanto.arch.awsutil.StreamingAwsClient
import com.monsanto.arch.awsutil.sns.model._

trait StreamingSNSClient extends StreamingAwsClient {
  /** Returns a flow that given a topic name, it will create a topic in AWS and emit the resulting topic ARN. */
  def topicCreator: Flow[String, String, NotUsed]

  /** Returns a flow that given a topic ARN will remove it from AWS.  Emits the topic ARN of the deleted topic. */
  def topicDeleter: Flow[String, String, NotUsed]

  /** Returns a source that emit all available topic ARNs. */
  def topicLister: Source[String, NotUsed]

  /** Returns a flow that given a topic ARN will return a map of all of its attributes. */
  def topicAttributesGetter: Flow[String, Map[String,String], NotUsed]

  /** Returns a flow takes executes requests to set a topic’s attribute.  Emits the topic ARN from the request. */
  def topicAttributeSetter: Flow[SetTopicAttributesRequest, String, NotUsed]

  /** Returns a flow that processes requests to add a permission to a topic.  Emits the topic ARN from the processed
    * request.
    */
  def permissionAdder: Flow[AddPermissionRequest, String, NotUsed]

  /** Returns a flow that processes requests to remove a permission from a topic.  Emits the topic ARN from the
    * processed request.
    */
  def permissionRemover: Flow[RemovePermissionRequest, String, NotUsed]

  /** Returns a flow that will emit either all subscriptions (given a `None`) or all of the subscription to a given
    * topic (given some topic ARN).
    */
  def subscriptionLister: Flow[ListSubscriptionsRequest, SubscriptionSummary, NotUsed]

  /** Returns a flow that will submit subscription request to AWS, emitting an optional subscription ARN if the
    * subscription completed without further confirmation. */
  def subscriber: Flow[SubscribeRequest, Option[String], NotUsed]

  /** Returns a flow that will process subscription confirmation requests, emitting the resulting subscription ARNs. */
  def subscriptionConfirmer: Flow[ConfirmSubscriptionRequest, String, NotUsed]

  /** Returns a flow that given a subscription ARN will return a map of all of its attributes. */
  def subscriptionAttributesGetter: Flow[String, Map[String,String], NotUsed]

  /** Returns a flow that processes requests to set a subscription attribute.  Emits the subscription ARN from the
    * request.
    */
  def subscriptionAttributeSetter: Flow[SetSubscriptionAttributesRequest, String, NotUsed]

  /** Returns a flow that given a subscription ARN will request deletion of the subscription and emit the ARN. */
  def unsubscriber: Flow[String, String, NotUsed]

  /** Returns a flow that will process platform application creation requests, emitting the resulting platform
    * application ARNs.
    */
  def platformApplicationCreator: Flow[CreatePlatformApplicationRequest, String, NotUsed]

  /** Returns a flow that given a platform application ARN will retrieve and emit the application’s attributes. */
  def platformApplicationAttributesGetter: Flow[String, Map[String,String], NotUsed]

  /** Returns a flow that processes requests to set platform application attributes.  Emits the platform application
    * ARN from the request.
    */
  def platformApplicationAttributesSetter: Flow[SetPlatformApplicationAttributesRequest, String, NotUsed]

  /** Returns a flow that given a platform application ARN will delete the platform application and emit the ARN. */
  def platformApplicationDeleter: Flow[String, String, NotUsed]

  /** Returns a source that will list all available platform applications. */
  def platformApplicationLister: Source[PlatformApplication, NotUsed]

  /** Returns a flow that will process platform endpoint creation requests, emitting the resulting platform
    * endpoint ARNs.
    */
  def platformEndpointCreator: Flow[CreatePlatformEndpointRequest, String, NotUsed]

  /** Returns a flow that given a platform endpoint ARN will return a map of all of its attributes. */
  def platformEndpointAttributesGetter: Flow[String, Map[String,String], NotUsed]

  /** Returns a flow that given a platform endpoint ARN and a map of attributes, will request that Amazon SNS modify
    * the attributes for the endpoint and emits the endpoint ARN.
    */
  def platformEndpointAttributesSetter: Flow[SetPlatformEndpointAttributesRequest, String, NotUsed]

  /** Returns a flow that given a platform endpoint ARN will delete the platform endpoint and emit the ARN. */
  def platformEndpointDeleter: Flow[String, String, NotUsed]

  /** Returns a flow that given a platform application ARN emit a sequence of endpoints. */
  def platformEndpointLister: Flow[String, PlatformEndpoint, NotUsed]

  /** Returns a flow that will publish a message and emit its unique identifier. */
  def publisher: Flow[PublishRequest, String, NotUsed]
}
