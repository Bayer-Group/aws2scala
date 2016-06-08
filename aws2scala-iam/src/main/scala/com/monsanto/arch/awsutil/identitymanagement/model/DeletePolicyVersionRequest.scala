package com.monsanto.arch.awsutil.identitymanagement.model

/** Specifies a request for deleting a version of a managed policy.
  *
  * @param arn       the ARN of the policy from which to delete a version
  * @param versionId the policy version to delete
  */
case class DeletePolicyVersionRequest(arn: PolicyArn, versionId: String)
