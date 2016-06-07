package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.auth.policy.Policy

/** Requests creation of a new version of the specified managed policy.
  *
  * @param arn the ARN of the managed policy to update
  * @param document the policy content that should be used for the new version of the managed policy
  * @param setAsDefault specifies whether to set this version as the policy’s default version
  */
case class CreatePolicyVersionRequest(arn: PolicyArn,
                                      document: Policy,
                                      setAsDefault: Boolean)
