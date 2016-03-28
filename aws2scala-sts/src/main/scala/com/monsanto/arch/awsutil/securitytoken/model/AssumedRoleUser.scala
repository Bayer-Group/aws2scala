package com.monsanto.arch.awsutil.securitytoken.model

/** Identifiers for temporary security credentials.
  *
  * @param arn the ARN of the temporary security credentials
  * @param assumedRoleId a unique identifier that contains the role ID and session name of the role being assumed
  */
case class AssumedRoleUser(arn: String, assumedRoleId: String)
