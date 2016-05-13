package com.monsanto.arch.awsutil.identitymanagement.model

/** Contains information about an attached policy.
  *
  * @param arn the ARN specifying the policy
  * @param name a friendly name for the policy
  */
case class AttachedPolicy(arn: PolicyArn, name: String)
