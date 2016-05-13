package com.monsanto.arch.awsutil

import com.monsanto.arch.awsutil.partitions.Partition

/** Represents an AWS account.
  *
  * @param id the AWS account ID, which must be a 12-digit string with no hyphens or the string `"aws"`
  * @param partition the partition to which the account belongs, defaults to the global AWS partition
  */
case class Account(id: String, partition: Partition = Partition.Aws) {
  require(id.matches("^[0-9]{12}|aws$"), "An AWS account ID must be a 12-digit string.")

  /** Returns the ARN that corresponds to this account. */
  def arn: AccountArn = AccountArn(this)
}
