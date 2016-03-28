package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.{Account, Arn}

private[awsutil] case class PolicyArn(account: Account, name: Name, path: Path) extends Arn(Arn.Namespace.IAM, None, account) {
  override val resource = s"policy$path$name"
}

private[awsutil] object PolicyArn {
  def apply(arn: String): PolicyArn = {
    arn match {
      case Arn(_, Arn.Namespace.IAM, None, Some(account), PolicyResourceRegex(path, name)) ⇒
        PolicyArn(account, Name(name), Path(path))
      case _ ⇒ throw new IllegalArgumentException(s"’$arn‘ is not a valid role ARN")
    }
  }

  private val PolicyResourceRegex = "^policy(/|/.*/)([^/]+)$".r
}
