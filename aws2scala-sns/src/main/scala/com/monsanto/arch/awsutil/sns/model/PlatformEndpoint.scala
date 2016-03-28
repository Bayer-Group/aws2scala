package com.monsanto.arch.awsutil.sns.model

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.monsanto.arch.awsutil.sns.StreamingSNSClient

import scala.concurrent.Future

/** Represents a snapshot of the state of a platform endpoint.  Note that this is a immutable object and is not live,
  * i.e. updates to the endpoint in AWS are not reflected here.  Additionally, mutation methods will not modify the
  * object.  To download the latest state from AWS use the [[refresh]] method, which will return a new instance.
  *
  * @param attributes a raw attributes of this topic
  */
case class PlatformEndpoint private[awsutil] (arn: String, attributes: Map[String,String]) {
  /** Returns whether or not delivery to the endpoint is enabled. */
  val enabled = attributes("Enabled").toBoolean
  /** Returns the device token or registration ID that is provided by the notification service to identify an app and
    * mobile device within the service.
    */
  val token = attributes("Token")
  /** Arbitrary user data associated with the endpoint.  Amazon SNS does not use this data. */
  val customUserData = attributes.get("CustomUserData")
  /** Returns the push notification platform of this endpoint as extracted from the ARN. */
  def platform: Platform = PlatformEndpointArn(arn).platform

  /** Enables/disables delivery to the endpoint. */
  def setEnabled(enabled: Boolean)(implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("Enabled", enabled.toString)

  /** Updates the device token for the endpoint. */
  def setToken(token: String)(implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("Token", token)

  /** Sets the arbitrary user data associated with this endpoint. */
  def setCustomUserData(customUserData: String)(implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("CustomUserData", customUserData)

  /** Sets an attribute for the endpoint. */
  def setAttribute(name: String, value: String)(implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    Source.single(SetPlatformEndpointAttributesRequest(arn, name, value))
      .via(sns.platformEndpointAttributesSetter)
      .runWith(Sink.ignore)

  /** Sets an attribute for the endpoint. `None` values will be converted to an empty string. */
  def setAttribute(name: String, value: Option[String])
                  (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    Source.single(SetPlatformEndpointAttributesRequest(arn, name, value))
      .via(sns.platformEndpointAttributesSetter)
      .runWith(Sink.ignore)

  /** Sets the attributes for the endpoint. */
  def setAttributes(attributes: Map[String,String])(implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    Source.single(SetPlatformEndpointAttributesRequest(arn, attributes))
      .via(sns.platformEndpointAttributesSetter)
      .runWith(Sink.ignore)

  /** Retrieves the latest state of this endpoint and returns a refreshed `PlatformEndpoint`. */
  def refresh()(implicit sns: StreamingSNSClient, m: Materializer): Future[PlatformEndpoint] =
    Source.single(arn).runWith(PlatformEndpoint.toPlatformEndpoint)

  /** Requests that the platform endpoint be deleted. */
  def delete()(implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    Source.single(arn)
      .via(sns.platformEndpointDeleter)
      .runWith(Sink.ignore)

  /** Sends a plain string message to this platform endpoint.  If you need to send JSON to a mobile endpoint, use
    * `publishJson` instead.
    *
    * @param message the message to send
    * @return the unique identifier of the published message
    */
  def publish(message: String)(implicit m: Materializer, sns: StreamingSNSClient): Future[String] =
    Source.single(PublishRequest(arn, message))
      .via(sns.publisher)
      .runWith(Sink.head)

  /** Sends a plain string message to this platform endpoint.  If you need to send JSON to a mobile endpoint, use
    * `publishJson` instead.
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

  /** Sends a JSON message to this platform endpoint.  If you would rather send a plain string, use `publish` instead.
    * `publishJson` instead.  Using this method will automatically structure the message so that it is a JSON object
    * with this endpoint’s platform as a key and the message value will be properly escaped for Amazon SNS.
    *
    * @param jsonMessage the message to send, which should be a JSON object which has been serialised to a string.  No
    *                    extra escaping is necessary.
    * @return the unique identifier of the published message
    */
  def publishJson(jsonMessage: String)(implicit m: Materializer, sns: StreamingSNSClient): Future[String] =
    Source.single(PublishRequest(this, jsonMessage))
      .via(sns.publisher)
      .runWith(Sink.head)

  /** Sends a JSON message to this platform endpoint.  If you would rather send a plain string, use `publish` instead.
    * `publishJson` instead.  Using this method will automatically structure the message so that it is a JSON object
    * with this endpoint’s platform as a key and the message value will be properly escaped for Amazon SNS.
    *
    * @param jsonMessage the message to send, which should be a JSON object which has been serialised to a string.  No
    *                    extra escaping is necessary.
    * @param attributes a map of additional attributes that should be attached to the message
    * @return the unique identifier of the published message
    */
  def publishJson(jsonMessage: String, attributes: Map[String,MessageAttributeValue])
                 (implicit m: Materializer, sns: StreamingSNSClient): Future[String] =
    Source.single(PublishRequest(this, jsonMessage, attributes))
      .via(sns.publisher)
      .runWith(Sink.head)
}

object PlatformEndpoint {
  /** A sink that consumes a single platform endpoint ARN to materialise a `Future[PlatformEndpoint]`. */
  private[model] def toPlatformEndpoint(implicit sns: StreamingSNSClient) =
    Flow[String]
      .flatMapConcat { arn ⇒
        Source.single(arn)
          .via(sns.platformEndpointAttributesGetter)
          .map(attrs ⇒ PlatformEndpoint(arn, attrs))
      }
      .toMat(Sink.head)(Keep.right)
}
