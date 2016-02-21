package com.monsanto.arch.awsutil.securitytoken.model

import com.amazonaws.services.securitytoken.model.{AssumeRoleResult ⇒ AwsAssumeRoleResult}

/** Contains the results of a request to assume a role.
  *
  * @param assumedRoleUser contains the assume role ARN and ID, which can be used as principals in policies
  * @param credentials the temporary security credentials
  * @param packedPolicySize if a policy was provided
  */
case class AssumeRoleResult(assumedRoleUser: AssumedRoleUser,
                            credentials: Credentials,
                            packedPolicySize: Option[Int]) {
  /** Builds an AWS `AssumeRoleResult` object from this object. */
  def toAws: AwsAssumeRoleResult = {
    val aws = new AwsAssumeRoleResult
    aws.setAssumedRoleUser(assumedRoleUser.toAws)
    aws.setCredentials(credentials.toAws)
    packedPolicySize.foreach(pps ⇒ aws.setPackedPolicySize(pps))
    aws
  }
}

object AssumeRoleResult {
  /** Builds a Scala `AssumeRoleResult` object from an AWS result. */
  def fromAws(aws: AwsAssumeRoleResult): AssumeRoleResult =
    AssumeRoleResult(
      AssumedRoleUser.fromAws(aws.getAssumedRoleUser),
      Credentials.fromAws(aws.getCredentials),
      Option(aws.getPackedPolicySize).map(_.toInt))
}
