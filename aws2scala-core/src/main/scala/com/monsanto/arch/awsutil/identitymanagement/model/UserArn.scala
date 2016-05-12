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
  /** Builds a user ARN object from the given ARN string. */
  def apply(arnString: String): UserArn =
    arnString match {
      case Arn(arn: UserArn) ⇒ arn
      case _ ⇒ throw new IllegalArgumentException(s"‘$arnString’ is not a valid user ARN.")
    }

  private[awsutil] val userArnPF: PartialFunction[Arn.ArnParts, UserArn] = {
    case (_, Arn.Namespace.IAM, None, Some(account), UserResourceRegex(Path.fromString(path), name)) ⇒
      UserArn(account, name, path)
  }

  private val UserResourceRegex = "^user(/|/.*/)([^/]+)$".r
}
