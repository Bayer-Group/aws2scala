package com.monsanto.arch.awsutil

import com.monsanto.arch.awsutil.partitions.Partition

private[awsutil] case class Account(id: String, partition: Partition) {
  require(id.matches("^[0-9]{12}$"), "An AWS account ID must be a 12-digit string.")

  override def toString = id

  def arn: AccountArn = AccountArn(this)
}

private[awsutil] object Account {
  def apply(id: String): Account = Account(id, Partition.Aws)
}
