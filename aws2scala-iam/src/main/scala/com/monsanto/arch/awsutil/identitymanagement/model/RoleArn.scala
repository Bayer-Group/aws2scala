package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.{Account, Arn}

private[awsutil] case class RoleArn(account: Account, name: Name, path: Path) extends Arn(Arn.Namespace.IAM, None, account) {
  override val resource = s"role$path$name"
}

private[awsutil] object RoleArn {
  def apply(arn: String): RoleArn = {
    arn match {
      case Arn(_, Arn.Namespace.IAM, None, Some(account), RoleResourceRegex(path, name)) ⇒
        RoleArn(account, Name(name), Path(path))
      case _ ⇒ throw new IllegalArgumentException(s"’$arn‘ is not a valid role ARN")
    }
  }

  private val RoleResourceRegex = "^role(/|/.*/)([^/]+)$".r
}
