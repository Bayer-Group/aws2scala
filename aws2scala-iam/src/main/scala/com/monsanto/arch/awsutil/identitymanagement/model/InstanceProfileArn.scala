package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.{Account, Arn}

/** Represents the ARN of of an IAM instance profile.
  *
  * @param account the account to which the policy belongs
  * @param name the name of the policy
  * @param path the path of the policy
  */
case class InstanceProfileArn(account: Account, name: String, path: Path = Path.empty) extends Arn(Arn.Namespace.IAM, None, account) {
  override val resource = s"instance-profile${path.pathString}$name"
}

object InstanceProfileArn {
  /** Builds an instance profile ARN object from the given ARN string. */
  def apply(arnString: String): InstanceProfileArn = {
    arnString match {
      case Arn(arn: InstanceProfileArn) ⇒ arn
      case _ ⇒ throw new IllegalArgumentException(s"’$arnString‘ is not a valid instance profile ARN")
    }
  }

  private[identitymanagement] val instanceProfileArnPF: PartialFunction[Arn.ArnParts, InstanceProfileArn] = {
    case (_, Arn.Namespace.IAM, None, Some(account), InstanceProfileResourceRegex(Path.fromString(path), name)) ⇒
      InstanceProfileArn(account, name, path)
  }

  private val InstanceProfileResourceRegex = "^instance-profile(/|/.*/)([^/]+)$".r
}
