package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.{Account, Arn}

/** Represents the ARN of of an IAM role.
  *
  * @param account the account to which the role belongs
  * @param name the name of the role
  * @param path the path of the role
  */
case class RoleArn(account: Account, name: String, path: Path = Path.empty) extends Arn(Arn.Namespace.IAM, None, account) {
  override val resource = s"role${path.pathString}$name"
}

object RoleArn {
  /** Utility to build/extract `RoleArn` instances from strings containing ARNs. */
  object fromArnString {
    /** Builds a `RoleArn` object from the given ARN string. */
    def apply(arnString: String): RoleArn =
      unapply(arnString).getOrElse(throw new IllegalArgumentException(s"‘$arnString’ is not a valid role ARN."))

    /** Extracts a `RoleArn` object from the given ARN string. */
    def unapply(arnString: String): Option[RoleArn] =
      arnString match {
        case Arn.fromArnString(arn: RoleArn) ⇒ Some(arn)
        case _                               ⇒ None
      }
  }

  private[awsutil] val roleArnPF: PartialFunction[Arn.ArnParts, RoleArn] = {
    case (_, Arn.Namespace.IAM, None, Some(account), RoleResourceRegex(Path.fromPathString(path), name)) ⇒
      RoleArn(account, name, path)
  }

  private val RoleResourceRegex = "^role(/|/.*/)([^/]+)$".r
}
