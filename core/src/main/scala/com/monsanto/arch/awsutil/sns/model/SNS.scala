package com.monsanto.arch.awsutil.sns.model

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.monsanto.arch.awsutil.sns.StreamingSNSClient

import scala.concurrent.Future

object SNS {
  /** Creates a topic with the given name. */
  def createTopic(name: String)(implicit sns: StreamingSNSClient, m: Materializer): Future[Topic] =
    Source.single(name)
      .via(sns.topicCreator)
      .via(sns.topicAttributesGetter)
      .map(Topic.apply)
      .runWith(Sink.head)

  /** Lists all available topics. */
  def listTopics()(implicit sns: StreamingSNSClient, m: Materializer): Future[Seq[Topic]] =
    sns.topicLister
      .via(sns.topicAttributesGetter)
      .map(Topic.apply)
      .runFold(Seq.empty[Topic])(_ :+ _)

  /** Lists all available subscriptions, including unconfirmed subscriptions. */
  def listSubscriptions()(implicit sns: StreamingSNSClient, m: Materializer): Future[Seq[SubscriptionSummary]] =
    Source.single(ListSubscriptionsRequest.allSubscriptions)
      .via(sns.subscriptionLister)
      .runFold(Seq.empty[SubscriptionSummary])(_ :+ _)

  /** Creates a platform application for one of the supported push notification services, such as APNS and GCM, to
    * which devices and mobile apps may register.  Note that the easiest way to create credentials is via one of the
    * [[Platform]] instances.
    *
    * {{{
    *   SNS.createPlatformApplication("myApp", Platform.GCM("secretApiKey"))
    * }}}
    *
    * @param name the application name, which must be made up of only uppercase and lowercase ASCII letters, number,
    *             underscores, hyphens, and periods, and must be between 1 and 256 characters long
    * @param credentials identifies the platform and contains the necessary credentials to identify with the platform
    * @return a snapshot of the newly-created platform application
    * @see [[http://docs.aws.amazon.com/sns/latest/dg/SNSMobilePush.html Using SNS Mobile Push Notifications]]
    */
  def createPlatformApplication(name: String, credentials: PlatformApplicationCredentials)
                               (implicit sns: StreamingSNSClient, m: Materializer): Future[PlatformApplication] =
    Source.single(CreatePlatformApplicationRequest(name, credentials.platform.name, credentials.principal, credentials.credential))
      .via(sns.platformApplicationCreator)
      .runWith(PlatformApplication.toPlatformApplication)

  /** Creates a platform application for one of the supported push notification services, such as APNS and GCM, to
    * which devices and mobile apps may register.  Note that the easiest way to create credentials is via one of the
    * [[Platform]] instances.
    *
    * {{{
    *   SNS.createPlatformApplication("myApp", Platform.GCM("secretApiKey"))
    * }}}
    *
    * @param name the application name, which must be made up of only uppercase and lowercase ASCII letters, number,
    *             underscores, hyphens, and periods, and must be between 1 and 256 characters long
    * @param credentials identifies the platform and contains the necessary credentials to identify with the platform
    * @param attributes additional attributes to set on the platform application
    * @return a snapshot of the newly-created platform application
    * @see [[http://docs.aws.amazon.com/sns/latest/dg/SNSMobilePush.html Using SNS Mobile Push Notifications]]
    */
  def createPlatformApplication(name: String, credentials: PlatformApplicationCredentials,
                                attributes: Map[String,String])
                               (implicit sns: StreamingSNSClient, m: Materializer): Future[PlatformApplication] =
    Source.single(CreatePlatformApplicationRequest(name, credentials.platform.name, credentials.principal, credentials.credential, attributes))
      .via(sns.platformApplicationCreator)
      .runWith(PlatformApplication.toPlatformApplication)

  /** Lists all of the platform applications. */
  def listPlatformApplications()(implicit sns: StreamingSNSClient, m: Materializer): Future[Seq[PlatformApplication]] =
    sns.platformApplicationLister
      .runWith(Sink.seq)
}
