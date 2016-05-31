package com.monsanto.arch.awsutil.identitymanagement.model

/** Detaches the specified managed policy to the specified role.
  *
  * @param roleName the name of the role to which to attach the policy
  * @param policyArn the ARN of the managed policy
  */
case class DetachRolePolicyRequest(roleName: String, policyArn: PolicyArn)
