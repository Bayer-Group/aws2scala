package com.monsanto.arch.awsutil.sns.model

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.monsanto.arch.awsutil.auth.policy.action.SNSAction
import com.monsanto.arch.awsutil.sns.StreamingSNSClient
import com.monsanto.arch.awsutil.sns.model.AwsConverters._

import scala.concurrent.Future

/** Represents the snapshot of the state of a topic.  Note that this is a immutable object and is not live, i.e.
  * updates to the topic in AWS are not reflected here.  Additionally, mutation methods will not modify the object.
  * To download the latest state from AWS use the [[refresh]] method, which will return a new instance.
  *
  * @param attributes a raw attributes of this topic
  */
case class Topic private[awsutil] (attributes: Map[String,String]) {
  /** The topic’s ARN. */
  val arn: String = attributes("TopicArn")
  /** The human-readable name used in the _From_ field for notifications to `email` and `email-json` endpoints. */
  val displayName: String = attributes("DisplayName")
  /** The AWS account ID of the topic’s owner. */
  val owner: String = attributes("Owner")
  /** The JSON serialisation of the topic’s access control policy. */
  val policy: String = attributes("Policy")
  /** The number of subscriptions pending confirmation on this topic. */
  val subscriptionsPending: Int = attributes("SubscriptionsPending").toInt
  /** The number of confirmed subscriptions on this topic. */
  val subscriptionsConfirmed: Int = attributes("SubscriptionsConfirmed").toInt
  /** The number of deleted subscriptions on this topic. */
  val subscriptionsDeleted: Int = attributes("SubscriptionsDeleted").toInt
  /** The JSON serialisation of the topic’s delivery policy, if any. */
  val deliveryPolicy: Option[String] = attributes.get("DeliveryPolicy")
  /** The JSON serialisation of the effective delivery policy that takes into account system defaults. */
  val effectiveDeliveryPolicy: String = attributes("EffectiveDeliveryPolicy")

  /** Returns the name of the topic as extracted from the ARN. */
  def name: String = TopicArn(arn).name

  /** Requests that AWS delete the topic. */
  def delete()(implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    Source.single(arn).via(sns.topicDeleter).runWith(Sink.ignore)

  /** Provides a custom representation that shows only the topic ARN. */
  override def toString = s"Topic($arn)"

  /** Updates the display name on the topic and returns a new snapshot of the topic from AWS. */
  def setDisplayName(displayName: String)
                    (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("DisplayName", Some(displayName))

  /** Updates the policy on the topic and returns a new snapshot of the topic from AWS. */
  def setPolicy(policy: String)
               (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("Policy", Some(policy))

  /** Updates the delivery policy on the topic and returns a new snapshot of the topic from AWS. */
  def setDeliveryPolicy(deliveryPolicy: Option[String])
                       (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("DeliveryPolicy", deliveryPolicy)

  /** Sets the value of an arbitrary attribute of this topic.
    *
    * @param attributeName the name of the attribute to update
    * @param attributeValue an option containing the new value of the attribute
    * @return a new snapshot of the
    */
  def setAttribute(attributeName: String, attributeValue: Option[String])
                          (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    Source.single(SetTopicAttributesRequest(arn, attributeName, attributeValue))
      .via(sns.topicAttributeSetter)
      .runWith(Sink.ignore)

  /** Adds a statement to the topic’s access control policy, granting access for the specified AWS accounts to the
    * specified actions.
    *
    * @param label a unique identifier for the new policy statement
    * @param awsAccountIds the AWS account IDs of the users (principals) who will be given access to the specified
    *                      actions
    * @param actions the actions you want to allow for the specified principals
    * @return a new topic snapshot with the resulting policy change
    */
  def addPermission(label: String, awsAccountIds: Seq[String], actions: Seq[SNSAction])
                   (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    Source.single(AddPermissionRequest(arn, label, awsAccountIds, actions))
      .via(sns.permissionAdder)
      .runWith(Sink.ignore)

  /** Removes a statement from a topic’s access control policy.
    *
    * @param label the unique identifier of the statement you want to remove
    */
  def removePermission(label: String)
                      (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    Source.single(RemovePermissionRequest(arn, label))
      .via(sns.permissionRemover)
      .runWith(Sink.ignore)

  /** Returns a new snapshot of this topic from AWS. */
  def refresh()(implicit sns: StreamingSNSClient, m: Materializer): Future[Topic] = Topic(arn)

  /** Lists all of the subscriptions to this topic. */
  def listSubscriptions()(implicit sns: StreamingSNSClient, m: Materializer): Future[Seq[SubscriptionSummary]] =
    Source.single(ListSubscriptionsRequest.forTopic(arn))
      .via(sns.subscriptionLister)
      .runWith(Sink.seq)

  /** Prepares to subscribe an endpoint by sending the endpoint a confirmation message.
    *
    * @param subscriptionEndpoint the endpoint that will receive notifications
    * @return the ARN of the subscription if the service was able to create a subscription immediately.
    */
  def subscribe(subscriptionEndpoint: SubscriptionEndpoint)
               (implicit sns: StreamingSNSClient, m: Materializer): Future[Option[Subscription]] =
    Source.single(SubscribeRequest(arn, subscriptionEndpoint.protocol.asAws, subscriptionEndpoint.endpoint))
      .via(sns.subscriber)
      .runWith(Subscription.toSubscriptionOption)

  /** Verifies an endpoint owner‘s intent to receive messages by validating the token sent to the endpoint by an
    * earlier `Subscribe` action.  If the token is valid, the action creates a new subscription and return its Amazon
    * Resource Name (ARN).
    *
    * @param token a short-lived token sent to an endpoint during the `Subscribe` action
    * @return a snapshot of the new subscription
    */
  def confirmSubscription(token: String)(implicit sns: StreamingSNSClient, m: Materializer): Future[Subscription] =
    Source.single(ConfirmSubscriptionRequest(arn, token))
      .via(sns.subscriptionConfirmer)
      .runWith(Subscription.toSubscription)

  /** Verifies an endpoint owner‘s intent to receive messages by validating the token sent to the endpoint by an
    * earlier `Subscribe` action.  If the token is valid, the action creates a new subscription and return its Amazon
    * Resource Name (ARN).
    *
    * @param token a short-lived token sent to an endpoint during the `Subscribe` action
    * @param authenticateOnUnsubscribe if set, disallows unauthenticated unsubscribes of the subscription
    * @return a snapshot of the new subscription
    */
  def confirmSubscription(token: String, authenticateOnUnsubscribe: Boolean)
                         (implicit sns: StreamingSNSClient, m: Materializer): Future[Subscription] =
    Source.single(ConfirmSubscriptionRequest(arn, token, Some(authenticateOnUnsubscribe)))
      .via(sns.subscriptionConfirmer)
      .runWith(Subscription.toSubscription)

  /** Sends a message to all of the topic’s subscribed endpoints.
    *
    * If you need to send different messages for each transport protocol or a custom JSON payload, use one of the
    * versions of `publish` that take a map for the message.
    *
    * @param message the message to send
    * @return the unique identifier of the published message
    */
  def publish(message: String)(implicit m: Materializer, sns: StreamingSNSClient): Future[String] =
    Source.single(PublishRequest(arn, message))
      .via(sns.publisher)
      .runWith(Sink.head)

  /** Sends a message to all of the topic’s subscribed endpoints.
    *
    * If you need to send different messages for each transport protocol or a custom JSON payload, use one of the
    * versions of `publish` that take a map for the message.
    *
    * @param message the message to send
    * @param subject a string to be used as the ''Subject'' line when the message is delivered to e-mail endpoints
    * @return the unique identifier of the published message
    */
  def publish(message: String, subject: String)
             (implicit m: Materializer, sns: StreamingSNSClient): Future[String] =
    Source.single(PublishRequest(arn, message, subject))
      .via(sns.publisher)
      .runWith(Sink.head)

  /** Sends a message to all of the topic’s subscribed endpoints.
    *
    * If you need to send different messages for each transport protocol or a custom JSON payload, use one of the
    * versions of `publish` that take a map for the message.
    *
    * @param message the message to send
    * @param attributes a map of additional attributes that should be attached to the message
    * @return the unique identifier of the published message
    */
  def publish(message: String, attributes: Map[String,MessageAttributeValue])
             (implicit m: Materializer, sns: StreamingSNSClient): Future[String] =
    Source.single(PublishRequest(arn, message, attributes))
      .via(sns.publisher)
      .runWith(Sink.head)

  /** Sends a message to all of the topic’s subscribed endpoints.
    *
    * If you need to send different messages for each transport protocol or a custom JSON payload, use one of the
    * versions of `publish` that take a map for the message.
    *
    * @param message the message to send
    * @param subject a string to be used as the ''Subject'' line when the message is delivered to e-mail endpoints
    * @param attributes a map of additional attributes that should be attached to the message
    * @return the unique identifier of the published message
    */
  def publish(message: String, subject: String, attributes: Map[String,MessageAttributeValue])
             (implicit m: Materializer, sns: StreamingSNSClient): Future[String] =
    Source.single(PublishRequest(arn, message, subject, attributes))
      .via(sns.publisher)
      .runWith(Sink.head)

  /** Sends a message to all of the topic’s subscribed endpoints.  This variation allows sending different messages
    * to different transport protocols.
    *
    * @param message the message to send, where the keys are the different transport protocols, i.e. `email`, `http`,
    *                `APNS_SANDBOX`, or `GCM`, and the values are strings that will be JSON-escaped
    * @return the unique identifier of the published message
    */
  def publish(message: Map[String,String])
             (implicit m: Materializer, sns: StreamingSNSClient): Future[String] =
    Source.single(PublishRequest(arn, message))
      .via(sns.publisher)
      .runWith(Sink.head)

  /** Sends a message to all of the topic’s subscribed endpoints.  This variation allows sending different messages
    * to different transport protocols.
    *
    * @param message the message to send, where the keys are the different transport protocols, i.e. `email`, `http`,
    *                `APNS_SANDBOX`, or `GCM`, and the values are strings that will be JSON-escaped
    * @param subject a string to be used as the ''Subject'' line when the message is delivered to e-mail endpoints
    * @return the unique identifier of the published message
    */
  def publish(message: Map[String,String], subject: String)
             (implicit m: Materializer, sns: StreamingSNSClient): Future[String] =
    Source.single(PublishRequest(arn, message, subject))
      .via(sns.publisher)
      .runWith(Sink.head)

  /** Sends a message to all of the topic’s subscribed endpoints.  This variation allows sending different messages
    * to different transport protocols.
    *
    * @param message the message to send, where the keys are the different transport protocols, i.e. `email`, `http`,
    *                `APNS_SANDBOX`, or `GCM`, and the values are strings that will be JSON-escaped
    * @param attributes a map of additional attributes that should be attached to the message
    * @return the unique identifier of the published message
    */
  def publish(message: Map[String,String], attributes: Map[String,MessageAttributeValue])
             (implicit m: Materializer, sns: StreamingSNSClient): Future[String] =
    Source.single(PublishRequest(arn, message, attributes))
      .via(sns.publisher)
      .runWith(Sink.head)

  /** Sends a message to all of the topic’s subscribed endpoints.  This variation allows sending different messages
    * to different transport protocols.
    *
    * @param message the message to send, where the keys are the different transport protocols, i.e. `email`, `http`,
    *                `APNS_SANDBOX`, or `GCM`, and the values are strings that will be JSON-escaped
    * @param subject a string to be used as the ''Subject'' line when the message is delivered to e-mail endpoints
    * @param attributes a map of additional attributes that should be attached to the message
    * @return the unique identifier of the published message
    */
  def publish(message: Map[String,String], subject: String, attributes: Map[String,MessageAttributeValue])
             (implicit m: Materializer, sns: StreamingSNSClient): Future[String] =
    Source.single(PublishRequest(arn, message, subject, attributes))
      .via(sns.publisher)
      .runWith(Sink.head)
}

object Topic {
  private[sns] def toTopic(implicit sns: StreamingSNSClient) =
    Flow[String]
      .via(sns.topicAttributesGetter)
      .map(attrs ⇒ Topic(attrs))
      .toMat(Sink.head)(Keep.right)

  def apply(topicArn: String)(implicit sns: StreamingSNSClient, m: Materializer): Future[Topic] = {
    // though this looks superfluous, it does impose a check that the topic ARN can parse
    val arn = TopicArn(topicArn).arnString
    Source.single(arn).runWith(toTopic)
  }
}
