package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.model.{AttachedPolicy ⇒ AwsAttachedPolicy}

/** Contains information about an attached policy.
  *
  * @param arn the ARN specifying the policy
  * @param name a friendly name for the policy
  */
case class AttachedPolicy(arn: String,
                          name: String) {
  /** Builds the equivalent AWS `AttachedPolicy` object from this object. */
  def toAws: AwsAttachedPolicy =
    new AwsAttachedPolicy()
      .withPolicyArn(arn)
      .withPolicyName(name)
}

object AttachedPolicy {
  /** Builds the equivalent Scala role object from the AWS object. */
  def fromAws(aws: AwsAttachedPolicy): AttachedPolicy =
    AttachedPolicy(aws.getPolicyArn, aws.getPolicyName)
}
