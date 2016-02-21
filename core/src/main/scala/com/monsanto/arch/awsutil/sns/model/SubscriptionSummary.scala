package com.monsanto.arch.awsutil.sns.model

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.monsanto.arch.awsutil.sns.StreamingSNSClient

import scala.concurrent.Future

/** This class is analogues to AWS’ own `Subscription` class.  This class differs from
  * [[com.monsanto.arch.awsutil.sns.model.Subscription Subscription]] primarily in that it can also a represent a
  * subscription that is still pending confirmation (has no ARN, yet).
  *
  * @param arn the subscription’s ARN
  * @param topicArn the ARN of the subscription’s topic
  * @param endpoint the subscription‘s endpoint
  * @param owner the subscription’s owner
  */
case class SubscriptionSummary(arn: Option[String],
                               topicArn: String,
                               endpoint: SubscriptionEndpoint,
                               owner: String) {
  /** Returns true if the subscription is pending. */
  def isPending: Boolean = arn.isEmpty

  /** Returns true if the subscription is confirmed. */
  def isConfirmed: Boolean = arn.isDefined

  /** Returns a fully-realised [[com.monsanto.arch.awsutil.sns.model.Subscription Subscription]] instance of this
    * summary.  In the case that this is a pending subscription, the future will contain `None`.
    */
  def asSubscription()(implicit sns: StreamingSNSClient, m: Materializer): Future[Option[Subscription]] =
    Source.single(arn).runWith(Subscription.toSubscriptionOption)
}
