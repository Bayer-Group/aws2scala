package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.model.{Role ⇒ AwsRole}

import java.util.Date

/** Contains information about an IAM role.
  *
  * @param arn the ARN specifying the role
  * @param name a friendly name for the role
  * @param path the path for the role
  * @param id a stable and unique string identifying the role
  * @param assumeRolePolicyDocument the policy that grants an entity permission to assume the role
  * @param created the date and time when the role was created
  */
case class Role(arn: String,
                name: String,
                path: String,
                id: String,
                assumeRolePolicyDocument: String,
                created: Date) {
  /** Builds the equivalent AWS role object from this object. */
  def toAws: AwsRole =
    new AwsRole()
      .withArn(arn)
      .withRoleName(name)
      .withPath(path)
      .withRoleId(id)
      .withAssumeRolePolicyDocument(assumeRolePolicyDocument)
      .withCreateDate(created)
}

object Role {
  /** Builds the equivalent Scala role object from the AWS object. */
  def fromAws(aws: AwsRole): Role =
    Role(aws.getArn, aws.getRoleName, aws.getPath, aws.getRoleId, aws.getAssumeRolePolicyDocument, aws.getCreateDate)
}
