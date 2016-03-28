package com.monsanto.arch.awsutil.sns.model

/** Removes a statement from a topic’s access control policy.
  *
  * @param topicArn the ARN of the topic whose access control policy to modify
  * @param label the unique label of the statment to remove
  */
case class RemovePermissionRequest(topicArn: String, label: String)
