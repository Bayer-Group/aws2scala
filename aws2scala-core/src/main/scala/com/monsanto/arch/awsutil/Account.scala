package com.monsanto.arch.awsutil

import com.monsanto.arch.awsutil.partitions.Partition

/** Represents an AWS account.
  *
  * @param id the AWS account ID, which must be a 12-digit string with no hyphens or the string `"aws"`
  * @param partition the partition to which the account belongs, defaults to the global AWS partition
  */
case class Account(id: String, partition: Partition) {
  require(id.matches("^[0-9]{12}|aws$"), "An AWS account ID must be a 12-digit string.")

  /** Returns the ARN that corresponds to this account. */
  def arn: AccountArn = AccountArn(this)
}

object Account {
  /** Constructs an AWS account in the default AWS partition. */
  def apply(id: String): Account = Account(id, Partition.Aws)

  /** Extractor to get an `Account` from a string.  Note that the account will
    * be in the default AWS region.
    */
  object fromNumber {
    def unapply(number: String): Option[Account] =
      if (number.matches("^\\d{12}$")) {
        Some(Account(number))
      } else {
        None
      }
  }
}
