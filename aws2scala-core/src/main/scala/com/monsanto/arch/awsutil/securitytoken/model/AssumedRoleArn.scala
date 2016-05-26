package com.monsanto.arch.awsutil.securitytoken.model

import com.monsanto.arch.awsutil.{Account, Arn}

/** Represents the ARN of an STS assumed role session.
  *
  * @param account the account to which the assumed role session belongs
  * @param roleName the name of the role that was assumed
  * @param sessionName the name of the session
  */
case class AssumedRoleArn(account: Account,
                          roleName: String,
                          sessionName: String) extends Arn(Arn.Namespace.AwsSTS, None, account) {
  override val resource = s"assumed-role/$roleName/$sessionName"
}

object AssumedRoleArn {
  /** Utility to build/extract `AssumedRoleArn` instances from strings containing ARNs. */
  object fromArnString {
    /** Builds a `AssumedRoleArn` object from the given ARN string. */
    def apply(arnString: String): AssumedRoleArn =
      unapply(arnString).getOrElse(throw new IllegalArgumentException(s"‘$arnString’ is not a valid assumed role ARN."))

    /** Extracts a `AssumedRoleArn` object from the given ARN string. */
    def unapply(arnString: String): Option[AssumedRoleArn] =
      arnString match {
        case Arn.fromArnString(arn: AssumedRoleArn) ⇒ Some(arn)
        case _                                      ⇒ None
      }
  }

  private[awsutil] val assumeRoleArnPF: PartialFunction[Arn.ArnParts, AssumedRoleArn] = {
    case (_, Arn.Namespace.AwsSTS, None, Some(account), AssumedRoleResourceRegex(roleName, sessionName)) ⇒
      AssumedRoleArn(account, roleName, sessionName)
  }

  private val AssumedRoleResourceRegex = "^assumed-role/([^/]+)/(.+$)".r
}
