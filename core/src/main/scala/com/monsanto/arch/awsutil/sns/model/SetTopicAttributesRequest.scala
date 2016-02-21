package com.monsanto.arch.awsutil.sns.model

import com.amazonaws.services.sns.model.{SetTopicAttributesRequest ⇒ AwsSetTopicAttributesRequest}

/** Allows a topic owner to set an attribute of the topic to a new value.
  *
  * @param topicArn the ARN of the topic to modify
  * @param attributeName the name of the attribute to set
  * @param attributeValue the new value for the attribute, if aay
  */
case class SetTopicAttributesRequest(topicArn: String, attributeName: String, attributeValue: Option[String]) {
  /** Returns the AWS equivalent of this request. */
  def toAws: AwsSetTopicAttributesRequest =
    new AwsSetTopicAttributesRequest(topicArn, attributeName, attributeValue.orNull)
}

object SetTopicAttributesRequest {
  /** Allows a topic owner to set an attribute of the topic to a new value.
    *
    * @param topicArn the ARN of the topic to modify
    * @param attributeName the name of the attribute to set
    * @param attributeValue the new value for the attribute
    */
  def apply(topicArn: String, attributeName: String, attributeValue: String): SetTopicAttributesRequest =
    SetTopicAttributesRequest(topicArn, attributeName, Option(attributeValue))
}
