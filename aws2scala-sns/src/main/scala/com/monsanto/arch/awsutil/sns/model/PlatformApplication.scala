package com.monsanto.arch.awsutil.sns.model

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.monsanto.arch.awsutil.sns.StreamingSNSClient

import scala.concurrent.Future

/** Represents a snapshot of the state of a platform application. Note that this is a immutable object and is not live,
  * i.e. updates to the platform application in AWS are not reflected here.  Additionally, mutation methods will not
  * result in an updated instance.  It is necessary to manually call [[refresh]], though keep in mind that there
  * may be a delay from when Amazon accepts the mutation until it is actually visible.
  *
  * @param arn the ARN for the platform application
  * @param attributes all of the raw attributes for the application
  */
case class PlatformApplication private[awsutil] (arn: String,
                                                 attributes: Map[String,String]) {
  /** Returns whether or not the application is enabled. */
  val enabled: Boolean = attributes("Enabled").toBoolean
  /** The topic that will be notified when an endpoint is added to this platform application. */
  val eventEndpointCreated: Option[String] = attributes.get("EventEndpointCreated")
  /** The topic that will be notified when an endpoint is deleted from this platform application. */
  val eventEndpointDeleted: Option[String] = attributes.get("EventEndpointDeleted")
  /** The topic that will be notified when an endpoint from this platform application is updated. */
  val eventEndpointUpdated: Option[String] = attributes.get("EventEndpointUpdated")
  /** The topic that will be notified when a direct push notification to an endpoint fails. */
  val eventDeliveryFailure: Option[String] = attributes.get("EventDeliveryFailure")
  /** The IAM role ARN used to give Amazon SNS write access to log successful delivery notifications to CloudWatch. */
  val successFeedbackRoleArn: Option[String] = attributes.get("SuccessFeedbackRoleArn").filter(_.nonEmpty)
  /** The IAM role ARN used to give Amazon SNS write access to log failed delivery notifications to CloudWatch. */
  val failureFeedbackRoleArn: Option[String] = attributes.get("FailureFeedbackRoleArn").filter(_.nonEmpty)
  /** The sample rate percentage for logging successfully delivered messages to CloudWatch. */
  val successFeedbackSampleRate: Option[Int] = attributes.get("SuccessFeedbackSampleRate").map(_.toInt)
  /** Returns the name that was used to create the application. */
  def name: String = PlatformApplicationArn.fromArnString(arn).name
  /** Returns the push notification platform for this application. */
  def platform: Platform = PlatformApplicationArn.fromArnString(arn).platform

  /** Updates the enabled attribute for the platform application. */
  def setEnabled(enabled: Boolean)(implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("Enabled", Some(enabled.toString))

  /** Updates the topic that will be notified when new endpoints are created for this application. */
  def setEventEndpointCreated(topicArn: Option[String])
                             (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("EventEndpointCreated", topicArn)

  /** Updates the topic that will be notified when endpoints are deleted from this application. */
  def setEventEndpointDeleted(topicArn: Option[String])
                             (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("EventEndpointDeleted", topicArn)

  /** Updates the topic that will be notified when endpoints are updated in this application. */
  def setEventEndpointUpdated(topicArn: Option[String])
                             (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("EventEndpointUpdated", topicArn)

  /** Updates the topic that will be notified when direct push notifications to an endpoint fail. */
  def setEventDeliveryFailure(topicArn: Option[String])
                             (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("EventDeliveryFailure", topicArn)

  /** Updates the IAM role ARN used to give Amazon SNS write access to log successful delivery notifications in
    *  CloudWatch.
    */
  def setSuccessFeedbackRoleArn(roleArn: Option[String])
                               (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("SuccessFeedbackRoleArn", roleArn)

  /** Updates the IAM role ARN used to give Amazon SNS write access to log failed delivery notifications in
    * CloudWatch.
    */
  def setFailureFeedbackRoleArn(roleArn: Option[String])
                               (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("FailureFeedbackRoleArn", roleArn)

  /** The sample rate percentage for logging successfully delivered messages to CloudWatch (must be between 0 and 100
    * inclusive).
    */
  def setSuccessFeedbackSampleRate(percentage: Int)
                                  (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    setAttribute("SuccessFeedbackSampleRate", Some(percentage.toString))

  /** Updates the credentials that Amazon SNS will use when communicating with the push notification platform. */
  def setCredentials(credentials: PlatformApplicationCredentials)
                    (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] = {
    require(credentials.platform == platform, "New credentials must match the application’s platform.")
    setAttributes(Map("PlatformPrincipal" → credentials.principal, "PlatformCredential" → credentials.credential))
  }

  /** Sets the attribute of the platform application to the given value.
    *
    * @param name the name of the attribute to modify
    * @param value the new value for the attribute, note that a `None` value will be set to the empty string
    * @return either this instance or a new instance containing refreshed data depending on the `updateRefreshStrategy`
    */
  def setAttribute(name: String, value: Option[String])
                  (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    Source.single(SetPlatformApplicationAttributesRequest(arn, name, value))
      .via(sns.platformApplicationAttributesSetter)
      .runWith(Sink.ignore)

  /** Updates the attributes of the platform application with the given values.
    *
    * @param attributes the map of attributes to use to update the platform application
    * @return either this instance or a new instance containing refreshed data depending on the `updateRefreshStrategy`
    */
  def setAttributes(attributes: Map[String,String])
                   (implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    Source.single(SetPlatformApplicationAttributesRequest(arn, attributes))
      .via(sns.platformApplicationAttributesSetter)
      .runWith(Sink.ignore)

  /** Creates an endpoint for a device and mobile app for this platform application. Note that for Baidu it is
    * necessary to include the `ChannelID` and `UserID` attributes.
    *
    * @param token a unique identifier created by the notification service for an app on a device.  For ADM and GCM
    *              this is a ''registration ID'', for APNs this is a ''device token'', for Baidu this is a
    *              ''platform token'' (which must also be included in the attributes), for MPNS and WNS this is a
    *              ''push notification URI''
    * @return a snapshot of the newly created platform endpoint
    */
  def createEndpoint(token: String)(implicit sns: StreamingSNSClient, m: Materializer): Future[PlatformEndpoint] =
    Source.single(CreatePlatformEndpointRequest(arn, token))
      .via(sns.platformEndpointCreator)
      .runWith(PlatformEndpoint.toPlatformEndpoint)

  /** Creates an endpoint for a device and mobile app for this platform application. Note that for Baidu it is
    * necessary to include the `ChannelID` and `UserID` attributes.
    *
    * @param token a unique identifier created by the notification service for an app on a device.  For ADM and GCM
    *              this is a ''registration ID'', for APNs this is a ''device token'', for Baidu this is a
    *              ''platform token'' (which must also be included in the attributes), for MPNS and WNS this is a
    *              ''push notification URI''
    * @param customUserData arbitrary user data to associate with the endpoint
    * @return a snapshot of the newly created platform endpoint
    */
  def createEndpoint(token: String, customUserData: String)
                    (implicit sns: StreamingSNSClient, m: Materializer): Future[PlatformEndpoint] =
    Source.single(CreatePlatformEndpointRequest(arn, token, customUserData))
      .via(sns.platformEndpointCreator)
      .runWith(PlatformEndpoint.toPlatformEndpoint)

  /** Creates an endpoint for a device and mobile app for this platform application. Note that for Baidu it is
    * necessary to include the `ChannelID` and `UserID` attributes.
    *
    * @param token a unique identifier created by the notification service for an app on a device.  For ADM and GCM
    *              this is a ''registration ID'', for APNs this is a ''device token'', for Baidu this is a
    *              ''platform token'' (which must also be included in the attributes), for MPNS and WNS this is a
    *              ''push notification URI''
    * @param attributes additional attributes to include in the request
    * @return a snapshot of the newly created platform endpoint
    */
  def createEndpoint(token: String, attributes: Map[String,String])
                    (implicit sns: StreamingSNSClient, m: Materializer): Future[PlatformEndpoint] =
    Source.single(CreatePlatformEndpointRequest(arn, token, attributes))
      .via(sns.platformEndpointCreator)
      .runWith(PlatformEndpoint.toPlatformEndpoint)

  /** Creates an endpoint for a device and mobile app for this platform application. Note that for Baidu it is
    * necessary to include the `ChannelID` and `UserID` attributes.
    *
    * @param token a unique identifier created by the notification service for an app on a device.  For ADM and GCM
    *              this is a ''registration ID'', for APNs this is a ''device token'', for Baidu this is a
    *              ''platform token'' (which must also be included in the attributes), for MPNS and WNS this is a
    *              ''push notification URI''
    * @param customUserData arbitrary user data to associate with the endpoint
    * @param attributes additional attributes to include in the request
    * @return a snapshot of the newly created platform endpoint
    */
  def createEndpoint(token: String, customUserData: String, attributes: Map[String,String])
                    (implicit sns: StreamingSNSClient, m: Materializer): Future[PlatformEndpoint] =
    Source.single(CreatePlatformEndpointRequest(arn, token, customUserData, attributes))
      .via(sns.platformEndpointCreator)
      .runWith(PlatformEndpoint.toPlatformEndpoint)

  /** Lists all of the endpoints registered with this application. */
  def listEndpoints()(implicit sns: StreamingSNSClient, m: Materializer): Future[Seq[PlatformEndpoint]] =
    Source.single(arn)
      .via(sns.platformEndpointLister)
      .runWith(Sink.seq)

  /** Returns a new `PlatformApplication` instance with values refreshed from AWs. */
  def refresh()(implicit sns: StreamingSNSClient, m: Materializer): Future[PlatformApplication] =
    PlatformApplication(arn)

  /** Requests deletion of this platform application. */
  def delete()(implicit sns: StreamingSNSClient, m: Materializer): Future[Done] =
    Source.single(arn)
      .via(sns.platformApplicationDeleter)
      .runWith(Sink.ignore)
}

object PlatformApplication {
  /** Given a platform application ARN, get its attributes from AWS and return a new `PlatformApplication` instance. */
  def apply(arn: String)(implicit sns: StreamingSNSClient, m: Materializer): Future[PlatformApplication] =
    Source.single(arn).runWith(toPlatformApplication)

  /** Returns a sink that given a platform application ARN will build a corresponding `PlatformApplication` instance. */
  private[sns] def toPlatformApplication(implicit sns: StreamingSNSClient) =
    Flow[String]
      .flatMapConcat { arn ⇒
        Source.single(arn)
          .via(sns.platformApplicationAttributesGetter)
          .map(attrs ⇒ PlatformApplication(arn, attrs))
      }
      .toMat(Sink.head)(Keep.right)
      .named("PlatformApplication.toPlatformApplication")
}
