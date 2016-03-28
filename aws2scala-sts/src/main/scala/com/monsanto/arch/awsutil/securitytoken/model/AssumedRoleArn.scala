package com.monsanto.arch.awsutil.securitytoken.model

import com.monsanto.arch.awsutil.{Account, Arn}

private[awsutil] case class AssumedRoleArn(account: Account, roleName: String, sessionName: String) extends Arn(Arn.Namespace.AwsSTS, None, account) {
  override val resource = s"assumed-role/$roleName/$sessionName"
}

private[awsutil] object AssumedRoleArn {
  def apply(arn: String): AssumedRoleArn = {
    arn match {
      case Arn(_, Arn.Namespace.AwsSTS, None, Some(account), AssumedRoleResourceRegex(roleName, sessionName)) ⇒
        AssumedRoleArn(account, roleName, sessionName)
      case _ ⇒ throw new IllegalArgumentException(s"’$arn‘ is not a valid assumed role ARN.")
    }
  }

  private val AssumedRoleResourceRegex = "^assumed-role/([^/]+)/(.+$)".r
}
