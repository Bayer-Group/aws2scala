package com.monsanto.arch.awsutil.sns.model

import com.amazonaws.services.sns.model.{SubscribeRequest ⇒ AwsSubscribeRequest}

/** Contains the information for a subscribe request.
  *
  * @param topicArn the ARN of the topic to which to subscribe
  * @param protocol the protocol to use
  * @param endpoint the endpoint that will receive notifications
  */
case class SubscribeRequest(topicArn: String, protocol: String, endpoint: String) {
  /** Creates an AWS equivalent to this request. */
  def toAws: AwsSubscribeRequest = new AwsSubscribeRequest(topicArn, protocol, endpoint)
}
