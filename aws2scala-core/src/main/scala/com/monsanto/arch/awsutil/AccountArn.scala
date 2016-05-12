package com.monsanto.arch.awsutil

/** Represents the ARN of the given account. */
case class AccountArn(account: Account) extends Arn(account.partition, Arn.Namespace.IAM, None, Some(account)) {
  override val resource = "root"
}

object AccountArn {
  /** Builds an account ARN object from the given ARN string. */
  def apply(arnString: String): AccountArn =
    arnString match {
      case Arn(accountArn: AccountArn) ⇒ accountArn
      case _ ⇒ throw new IllegalArgumentException(s"‘$arnString’ is not a valid account ARN.")
    }

  private[awsutil] val accountArnPF: PartialFunction[Arn.ArnParts, AccountArn] = {
    case (_, Arn.Namespace.IAM, None, Some(account), "root") ⇒ AccountArn(account)
  }
}
