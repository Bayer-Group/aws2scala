package com.monsanto.arch.awsutil.securitytoken.model

import com.amazonaws.services.securitytoken.model.{AssumedRoleUser ⇒ AwsAssumedRoleUser}

/** Identifiers for temporary security credentials.
  *
  * @param arn the ARN of the temporary security credentials
  * @param assumedRoleId a unique identifier that contains the role ID and session name of the role being assumed
  */
case class AssumedRoleUser(arn: String, assumedRoleId: String) {
  /** Builds an equivalent AWS `AssumedRoleUser` from this object. */
  def toAws: AwsAssumedRoleUser =
    new AwsAssumedRoleUser()
      .withArn(arn)
      .withAssumedRoleId(assumedRoleId)
}

object AssumedRoleUser {
  /** Builds an `AssumedRoleUser` from its AWS equivalent. */
  def fromAws(value: AwsAssumedRoleUser): AssumedRoleUser =
    AssumedRoleUser(value.getArn, value.getAssumedRoleId)
}
