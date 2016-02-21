package com.monsanto.arch.awsutil.sns.model

import com.amazonaws.services.sns.model.{ConfirmSubscriptionRequest ⇒ AwsConfirmSubscriptionRequest}

/** Contains the information needed to confirm a subscription.
  *
  * @param topicArn the ARN of the topic for which to confirm a subscription
  * @param token the short-lived token sent to an endpoint to subscribe
  * @param authenticateOnUnsubscribe if set to `true`, disallows unauthenticated unsubscribes of the subscription
  */
case class ConfirmSubscriptionRequest(topicArn: String, token: String, authenticateOnUnsubscribe: Option[Boolean] = None) {
  def toAws: AwsConfirmSubscriptionRequest = {
    authenticateOnUnsubscribe match {
      case None ⇒ new AwsConfirmSubscriptionRequest(topicArn, token)
      case Some(b) ⇒ new AwsConfirmSubscriptionRequest(topicArn, token, b.toString)
    }
  }
}
