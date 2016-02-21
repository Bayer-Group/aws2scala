package com.monsanto.arch.awsutil.sns.model

import com.amazonaws.services.sns.model.{AddPermissionRequest ⇒ AwsAddPermissionRequest}
import com.monsanto.arch.awsutil.auth.policy.action.SNSAction

import scala.collection.JavaConverters._

/** Adds a statement to a topic’s access control policy, granting access for the specified AWS accounts to the
  * specified actions.
  *
  * @param topicArn the ARN of the topic whose access control policy to modify
  * @param label a unique identifier for the new policy statement
  * @param accounts the AWS account IDs of the users who will be given access to the specified actions
  * @param actions the actions to allow for the specified users
  */
case class AddPermissionRequest(topicArn: String, label: String, accounts: Seq[String], actions: Seq[SNSAction]) {
  /** Returns an AWS equivalent for this request. */
  def toAws: AwsAddPermissionRequest =
    new AwsAddPermissionRequest(topicArn, label, accounts.asJava, actions.map(_.toString).asJava)
}
