package com.monsanto.arch.awsutil.sns.model

import com.amazonaws.services.sns.model.{SetSubscriptionAttributesRequest ⇒ AwsSetSubscriptionAttributesRequest }

/** Contains the necessary information to set an attribute of a subscription to a new value.
  *
  * @param subscriptionArn the ARN of the subscription to modify
  * @param attributeName the name of the attribute to set
  * @param attributeValue the new value for the attribute in JSON format
  */
case class SetSubscriptionAttributesRequest(subscriptionArn: String, attributeName: String, attributeValue: Option[String]) {
  def toAws: AwsSetSubscriptionAttributesRequest =
    new AwsSetSubscriptionAttributesRequest(subscriptionArn, attributeName, attributeValue.orNull)
}

object SetSubscriptionAttributesRequest {
  /** Contains the necessary information to set an attribute of a subscription to a new value.
    *
    * @param subscriptionArn the ARN of the subscription to modify
    * @param attributeName the name of the attribute to set
    * @param attributeValue the new value for the attribute in JSON format
    */
  def apply(subscriptionArn: String, attributeName: String, attributeValue: String): SetSubscriptionAttributesRequest =
    SetSubscriptionAttributesRequest(subscriptionArn, attributeName, Option(attributeValue))
}

