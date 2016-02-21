package com.monsanto.arch.awsutil.sns.model

import com.amazonaws.services.sns.{model ⇒ aws}

/** Contains all of the information necessary to list either all of the subscriptions or just the subscriptions for a
  * given topic.
  *
  * @param topicArn the ARN of the topic for which to list subscriptions
  */
case class ListSubscriptionsRequest private (topicArn: Option[String]) {
  /** Since AWS uses two different endpoints for listing subscriptions, this will return either a
    * `Left[ListSubscriptionsRequest]` or a `Right[ListSubscriptionsByTopicRequest]`.
    */
  def toEitherAws: Either[aws.ListSubscriptionsRequest, aws.ListSubscriptionsByTopicRequest] =
    topicArn match {
      case None ⇒ Left(new aws.ListSubscriptionsRequest)
      case Some(arn) ⇒  Right(new aws.ListSubscriptionsByTopicRequest(arn))
    }
}

object ListSubscriptionsRequest {
  /** Lists all subscriptions. */
  val allSubscriptions: ListSubscriptionsRequest = ListSubscriptionsRequest(None)

  /** Lists only subscriptions to the given topic. */
  def forTopic(topicArn: String): ListSubscriptionsRequest = ListSubscriptionsRequest(Some(topicArn))
}
