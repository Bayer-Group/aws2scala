package com.monsanto.arch.awsutil.identitymanagement.model

import java.util.Date

import com.monsanto.arch.awsutil.auth.policy

/** Contains information about an IAM role.
  *
  * @param arn the ARN specifying the role
  * @param name a friendly name for the role
  * @param path the path for the role
  * @param id a stable and unique string identifying the role
  * @param assumeRolePolicyDocument the policy that grants an entity permission to assume the role
  * @param created the date and time when the role was created
  */
case class Role(arn: RoleArn,
                name: String,
                path: Path,
                id: String,
                assumeRolePolicyDocument: policy.Policy,
                created: Date)
