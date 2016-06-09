package com.monsanto.arch.awsutil.identitymanagement.model

/** Specifies a request for retrieving a version of a managed policy.
  *
  * @param arn       the ARN of the policy from which to retrieve a version
  * @param versionId the policy version to retrieve
  */
case class GetPolicyVersionRequest(arn: PolicyArn, versionId: String)
