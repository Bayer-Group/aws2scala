package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.{Account, Arn}

private[awsutil] case class InstanceProfileArn(account: Account, name: Name, path: Path) extends Arn(Arn.Namespace.IAM, None, account) {
  override val resource = s"instance-profile$path$name"
}

private[awsutil] object InstanceProfileArn {
  def fromArn(arn: String): Option[InstanceProfileArn] = {
    arn match {
      case Arn(_, Arn.Namespace.IAM, None, Some(account), InstanceProfileResourceRegex(path, name)) ⇒
        Some(InstanceProfileArn(account, Name(name), Path(path)))
      case _ ⇒ None
    }
  }

  private val InstanceProfileResourceRegex = "^instance-profile(/|/.*/)([^/]+)$".r
}
