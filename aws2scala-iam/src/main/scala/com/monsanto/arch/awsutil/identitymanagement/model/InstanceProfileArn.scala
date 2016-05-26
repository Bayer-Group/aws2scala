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
  /** Utility to build/extract `InstanceProfileArn` instances from strings. */
  object fromArnString {
    /** Builds an `InstanceProfileArn` object from the given ARN string. */
    def apply(arnString: String): InstanceProfileArn =
      unapply(arnString)
        .getOrElse(throw new IllegalArgumentException(s"‘$arnString’ is not a valid instance profile ARN."))

    /** Extracts an `InstanceProfileArn` object from the given ARN string. */
    def unapply(arnString: String): Option[InstanceProfileArn] =
      arnString match {
        case Arn.fromArnString(accountArn: InstanceProfileArn) ⇒ Some(accountArn)
        case _                                                 ⇒ None
      }
  }

  private[identitymanagement] val instanceProfileArnPF: PartialFunction[Arn.ArnParts, InstanceProfileArn] = {
    case (_, Arn.Namespace.IAM, None, Some(account), InstanceProfileResourceRegex(Path.fromPathString(path), name)) ⇒
      InstanceProfileArn(account, name, path)
  }

  private val InstanceProfileResourceRegex = "^instance-profile(/|/.*/)([^/]+)$".r
}
