package com.monsanto.arch.awsutil.sns.model

/** Contains the information needed to confirm a subscription.
  *
  * @param topicArn the ARN of the topic for which to confirm a subscription
  * @param token the short-lived token sent to an endpoint to subscribe
  * @param authenticateOnUnsubscribe if set to `true`, disallows unauthenticated unsubscribes of the subscription
  */
case class ConfirmSubscriptionRequest(topicArn: String, token: String, authenticateOnUnsubscribe: Option[Boolean] = None)
