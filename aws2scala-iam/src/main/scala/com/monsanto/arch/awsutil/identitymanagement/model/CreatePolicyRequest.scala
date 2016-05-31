package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.auth.policy.Policy

/** Requests creation of a new managed policy.
  *
  * @param name the friendly name of the policy
  * @param document the policy to use as the content for the new policy
  * @param description a friendly description of the policy
  * @param path the path for the policy
  */
case class CreatePolicyRequest(name: String,
                               document: Policy,
                               description: Option[String],
                               path: Path)
