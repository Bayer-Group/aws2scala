package com.monsanto.arch.awsutil.securitytoken.model

import scala.concurrent.duration.FiniteDuration

/** A handy case class useful for generating AWS `AssumeRoleRequest` instances.
  *
  * @param roleArn the ARN of the role to assume
  * @param roleSessionName a unique identifier for the assumed role session
  * @param duration the duration for the role, which will be rounded down to the nearest minute
  * @param externalId a unique identifier used by a third party when assuming a role in a customer’s account
  * @param policy an IAM policy in JSON format that can further limit the permissions for the session
  * @param mfa a token value from and an identifier of a MFA devices
  *
  * @see [[com.amazonaws.services.securitytoken.model.AssumeRoleRequest AssumeRoleRequest]]
  * @see [[http://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRole.html AssumeRole API reference]]
  */
case class AssumeRoleRequest(roleArn: String,
                             roleSessionName: String,
                             duration: Option[FiniteDuration] = None,
                             externalId: Option[String] = None,
                             policy: Option[String] = None,
                             mfa: Option[AssumeRoleRequest.MFA] = None)

object AssumeRoleRequest {
  /** Contains the information necessary to authenticate with an MFA token.
    *
    * @param serialNumber the identification number of the MFA device, which may be the serial number of a hardware
    *                     device or the ARN of a virtual device
    * @param tokenCode the value provided by the MFA device
    */
  case class MFA(serialNumber: String, tokenCode: String)
}
