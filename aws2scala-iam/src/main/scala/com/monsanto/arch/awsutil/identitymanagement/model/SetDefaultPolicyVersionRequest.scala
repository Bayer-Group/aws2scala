package com.monsanto.arch.awsutil.identitymanagement.model

/** Specifies a request for setting the default (operative) version of a managed policy.
  *
  * @param arn       the ARN of the policy whose default version to set
  * @param versionId the policy version to set as the default (operative) version
  */
case class SetDefaultPolicyVersionRequest(arn: PolicyArn, versionId: String)
