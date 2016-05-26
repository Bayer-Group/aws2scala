package com.monsanto.arch.awsutil

/** Represents the ARN of the given account. */
case class AccountArn(account: Account) extends Arn(account.partition, Arn.Namespace.IAM, None, Some(account)) {
  override val resource = "root"
}

object AccountArn {
  /** Utility to build/extract `AccountArn` instances from strings. */
  object fromArnString {
    /** Builds an `AccountArn` object from the given ARN string. */
    def apply(arnString: String): AccountArn =
      unapply(arnString).getOrElse(throw new IllegalArgumentException(s"‘$arnString’ is not a valid account ARN."))

    /** Extracts an `AccountArn` object from the given ARN string. */
    def unapply(arnString: String): Option[AccountArn] =
      arnString match {
        case Arn.fromArnString(accountArn: AccountArn) ⇒ Some(accountArn)
        case _                                         ⇒ None
      }
  }

  private[awsutil] val accountArnPF: PartialFunction[Arn.ArnParts, AccountArn] = {
    case (_, Arn.Namespace.IAM, None, Some(account), "root") ⇒ AccountArn(account)
  }
}
