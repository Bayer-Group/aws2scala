package com.monsanto.arch.awsutil.identitymanagement.model

import java.util.Date

import com.monsanto.arch.awsutil.auth.policy.Policy

/** Contains information about a version of a managed policy.
  *
  * @param document the policy document
  * @param versionId the identifier for the policy version
  * @param isDefaultVersion specifies whether the policy version is set as the policy’s default version
  * @param created the date and time when the policy version was created
  */
case class ManagedPolicyVersion(document: Policy,
                                versionId: String,
                                isDefaultVersion: Boolean,
                                created: Date)
