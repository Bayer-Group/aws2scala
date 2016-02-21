package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.model.{DetachRolePolicyRequest ⇒ AwsDetachRolePolicyRequest}

/** Detaches the specified managed policy to the specified role.
  *
  * @param roleName the name of the role to which to attach the policy
  * @param policyArn the ARN of the managed policy
  */
case class DetachRolePolicyRequest(roleName: String, policyArn: String) {
  /** Creates the AWS `DetachRolePolicyRequest` corresponding to this object. */
  def toAws: AwsDetachRolePolicyRequest =
    new AwsDetachRolePolicyRequest()
      .withRoleName(roleName)
      .withPolicyArn(policyArn)
}

object DetachRolePolicyRequest {
  /** Creates the `DetachRolePolicyRequest` object corresponding to the AWs instance. */
  def fromAws(aws: AwsDetachRolePolicyRequest): DetachRolePolicyRequest =
    DetachRolePolicyRequest(aws.getRoleName, aws.getPolicyArn)
}
