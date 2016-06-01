package com.monsanto.arch.awsutil.kms.model

import java.util.Date

import com.monsanto.arch.awsutil.Account

/** Contains information about a customer master key (CMK).
  *
  * @param account the account that owns the key
  * @param id the globally unique identifier for the key
  * @param arn the Amazon Resource Name (ARN) of the key
  * @param creationDate the date and time when the key was created
  * @param enabled specifies whether the key is enabled
  * @param description the friendly description of the key
  * @param usage the cryptographic operations for which the key may be used
  * @param state the state of the key
  * @param deletionDate the date and time after which AWS KMS deletes the key, if any
  */
case class KeyMetadata(account: Account,
                       id: String,
                       arn: KeyArn,
                       creationDate: Date,
                       enabled: Boolean,
                       description: Option[String],
                       usage: KeyUsage,
                       state: KeyState,
                       deletionDate: Option[Date])
