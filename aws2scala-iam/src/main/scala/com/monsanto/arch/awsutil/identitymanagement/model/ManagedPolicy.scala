package com.monsanto.arch.awsutil.identitymanagement.model

import java.util.Date

/** Contains information about a managed policy.
  *
  * @param name the friendly name identifying the policy
  * @param id the stable and unique string identifying the policy
  * @param arn the Amazon Resource Name (ARN) that uniquely identifies this policy
  * @param path the path to the policy
  * @param defaultVersionId the identifier for the version of the policy that is set as the default version
  * @param attachmentCount the number of entities (user, groups, and roles) that the policy is attached to
  * @param attachable specifies whether the policy can be attached to an IAM user, group, or role
  * @param description a friendly description of the policy
  * @param created the date and time when the policy was created
  * @param updated the date and time when the policy was last updated.  When a policy has only one version, this field
  *                contains the date and time when the policy was created.  When a policy has more than one version,
  *                this field contains the date and time when the most recent policy version was created.
  */
case class ManagedPolicy(name: String,
                         id: String,
                         arn: PolicyArn,
                         path: Path,
                         defaultVersionId: String,
                         attachmentCount: Int,
                         attachable: Boolean,
                         description: Option[String],
                         created: Date,
                         updated: Date)
