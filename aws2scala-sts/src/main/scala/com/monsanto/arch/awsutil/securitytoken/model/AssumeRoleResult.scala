package com.monsanto.arch.awsutil.securitytoken.model

/** Contains the results of a request to assume a role.
  *
  * @param assumedRoleUser contains the assume role ARN and ID, which can be used as principals in policies
  * @param credentials the temporary security credentials
  * @param packedPolicySize if a policy was provided
  */
case class AssumeRoleResult(assumedRoleUser: AssumedRoleUser,
                            credentials: Credentials,
                            packedPolicySize: Option[Int])
