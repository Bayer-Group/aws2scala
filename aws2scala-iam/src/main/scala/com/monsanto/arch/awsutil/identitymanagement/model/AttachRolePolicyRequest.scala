package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.model.{AttachRolePolicyRequest ⇒ AwsAttachRolePolicyRequest}

/** Attaches the specified managed policy to the specified role.
  *
  * @param roleName the name of the role to which to attach the policy
  * @param policyArn the ARN of the managed policy
  */
case class AttachRolePolicyRequest(roleName: String, policyArn: String) {
  /** Creates the AWS `AttachRolePolicyRequest` corresponding to this object. */
  def toAws: AwsAttachRolePolicyRequest =
    new AwsAttachRolePolicyRequest()
      .withRoleName(roleName)
      .withPolicyArn(policyArn)
}

object AttachRolePolicyRequest {
  /** Creates the `AttachRolePolicyRequest` object corresponding to the AWs instance. */
  def fromAws(aws: AwsAttachRolePolicyRequest): AttachRolePolicyRequest =
    AttachRolePolicyRequest(aws.getRoleName, aws.getPolicyArn)
}
