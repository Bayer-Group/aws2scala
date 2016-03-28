package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.{Account, Arn}

private[awsutil] case class UserArn(account: Account, name: Name, path: Path) extends Arn(Arn.Namespace.IAM, None, account) {
  override val resource = s"user$path$name"
}

private[awsutil] object UserArn {
  def apply(arn: String): UserArn = {
    arn match {
      case Arn(_, Arn.Namespace.IAM, None, Some(account), UserResourceRegex(path, name)) ⇒
        UserArn(account, Name(name), Path(path))
      case _ ⇒ throw new IllegalArgumentException(s"’$arn‘ is not a valid user ARN")
    }
  }

  private val UserResourceRegex = "^user(/|/.*/)([^/]+)$".r
}
