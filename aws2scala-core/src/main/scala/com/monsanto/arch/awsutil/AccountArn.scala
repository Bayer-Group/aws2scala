package com.monsanto.arch.awsutil

import com.monsanto.arch.awsutil.partitions.Partition

private[awsutil] case class AccountArn(account: Account) extends Arn(account.partition, Arn.Namespace.IAM, None, Some(account)) {
  override val resource = "root"
}

private[awsutil] object AccountArn {
  def fromArn(arn: String): Option[AccountArn] = {
    arn match {
      case AccountArnRegex(Partition(p), id) ⇒ Some(AccountArn(Account(id, p)))
      case _ ⇒ None
    }
  }

  private val AccountArnRegex = "arn:([^:]+):iam::([^:]+):root".r
}
