package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.{Account, Arn}

/** Represents the ARN of of an IAM user.
  *
  * @param account the account to which the user belongs
  * @param name the name of the user
  * @param path the path of the user
  */
case class UserArn(account: Account, name: String, path: Path = Path.empty) extends Arn(Arn.Namespace.IAM, None, account) {
  override val resource = s"user${path.pathString}$name"
}

object UserArn {
  /** Utility to build/extract `UserArn` instances from strings containing ARNs. */
  object fromArnString {
    /** Builds a `UserArn` object from the given ARN string. */
    def apply(arnString: String): UserArn =
      unapply(arnString).getOrElse(throw new IllegalArgumentException(s"‘$arnString’ is not a valid user ARN."))

    /** Extracts a `UserArn` object from the given ARN string. */
    def unapply(arnString: String): Option[UserArn] =
      arnString match {
        case Arn.fromArnString(arn: UserArn) ⇒ Some(arn)
        case _                               ⇒ None
      }
  }

  private[awsutil] val userArnPF: PartialFunction[Arn.ArnParts, UserArn] = {
    case (_, Arn.Namespace.IAM, None, Some(account), UserResourceRegex(Path.fromPathString(path), name)) ⇒
      UserArn(account, name, path)
  }

  private val UserResourceRegex = "^user(/|/.*/)([^/]+)$".r
}
