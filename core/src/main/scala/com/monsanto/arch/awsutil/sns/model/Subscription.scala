package com.monsanto.arch.awsutil.sns.model

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.monsanto.arch.awsutil.sns.StreamingSNSClient

import scala.concurrent.Future

/** Represents an immutable snapshot of a topic subscription at a point in time.  Note that this object is not live,
  * i.e. updates to the subscription in AWS will not be reflected in this object (a [[refresh]] is required).  Also,
  * any ‘mutation’ methods will not update the Subscription instance.  Note that this class differs from AWS’ own
  * [[com.amazonaws.services.sns.model.Subscription Subscription]] and our [[SubscriptionSummary]] by only representing
  * confirmed subscriptions.
  *
  * @param attributes a map of the raw subscription attributes
  */
case class Subscription private[sns] (attributes: Map[String,String]) {
  /** Returns the subscription’s ARN. */
  val arn: String = attributes("SubscriptionArn")

  /** Returns the subscription’s endpoint. */
  val endpoint: SubscriptionEndpoint = Protocol(attributes("Protocol"))(attributes("Endpoint"))

  /** Returns the ARN of the subscription’s topic. */
  val topicArn: String = attributes("TopicArn")

  /** Returns the AWS account number of the subscription’s owner. */
  val owner: String = attributes("Owner")

  /** Returns whether the subscriptions confirmation included authentication. */
  val confirmationWasAuthenticated: Boolean = attributes("ConfirmationWasAuthenticated").toBoolean

  /** Returns delivery policy specific to this subscription, if any. */
  val deliveryPolicy: Option[String] = attributes.get("DeliveryPolicy")

  /** Returns the effective delivery policy resulting for applying all applicable policies, if any. */
  val effectiveDeliveryPolicy: Option[String] = attributes.get("EffectiveDeliveryPolicy")

  /** Returns whether raw message delivery has been enabled for this subscription. */
  val rawMessageDelivery: Boolean = attributes("RawMessageDelivery").toBoolean

  /** Provides a more sane string representation. */
  override def toString = s"Subscription($arn)"

  /** Allows setting the delivery policy for the subscription.
    *
    * @param rawMessageDelivery whether or not to deliver raw messages to this subscription
    * @return a fresh snapshot of the subscription
    */
  def setRawMessageDelivery(rawMessageDelivery: Boolean)
                           (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("RawMessageDelivery", Some(rawMessageDelivery.toString))

  /** Allows setting the delivery policy for the subscription.
    *
    * @param newDeliveryPolicy the new delivery policy, if any
    * @return a fresh snapshot of the subscription
    */
  def setDeliveryPolicy(newDeliveryPolicy: Option[String])
                       (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("DeliveryPolicy", newDeliveryPolicy)

  /** Allows setting of an arbitrary subscription attribute.
    *
    * @param attributeName the name of the attribute to set
    * @param attributeValue the new value, if any
    * @return a fresh snapshot of the subscription
    */
  def setAttribute(attributeName: String, attributeValue: Option[String])
                  (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    Source.single(SetSubscriptionAttributesRequest(arn, attributeName, attributeValue))
      .via(sns.subscriptionAttributeSetter)
      .runWith(Sink.ignore)

  /** Returns a new `Subscription` instance with the latest values from AWS. */
  def refresh()(implicit sns: StreamingSNSClient, m: Materializer): Future[Subscription] = Subscription(arn)

  /** Deletes this subscription. */
  def unsubscribe()(implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    Source.single(arn).via(sns.unsubscriber).runWith(Sink.ignore)
}

object Subscription {
  /** Given the ARN of a subscription, get its attributes and build a new `Subscription` object. */
  def apply(subscriptionArn: String)(implicit sns: StreamingSNSClient, m: Materializer): Future[Subscription] =
    Source.single(subscriptionArn).runWith(toSubscription)

  private[sns] def toSubscription(implicit sns: StreamingSNSClient) =
    Flow[String]
      .via(sns.subscriptionAttributesGetter)
      .map(attrs ⇒ Subscription(attrs))
      .toMat(Sink.head)(Keep.right)
      .named("Subscription.toSubscription")

  private[sns] def toSubscriptionOption(implicit sns: StreamingSNSClient) =
    Flow[Option[String]]
      .filter(_.isDefined)
      .map(_.get)
      .via(sns.subscriptionAttributesGetter)
      .map(attrs ⇒ Subscription(attrs))
      .toMat(Sink.headOption)(Keep.right)
      .named("Subscription.toSubscriptionOption")
}