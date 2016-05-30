package com.monsanto.arch.awsutil.identitymanagement.model

import java.util.Date

import com.monsanto.arch.awsutil.Account

/** Contains information about an IAM user identity.
  *
  * @param path the path to the user
  * @param name the friendly name identifying the user
  * @param id the stable and unique string identifying the user
  * @param arn the ARN that identifies the user
  * @param created the date and time when the user was created
  * @param passwordLastUsed the date and time when the user’s password was last used to sign in to an AWS website.  May
  *                         be `None` if the user does not have a password, has never used the password, or there is no
  *                         sign-in data associated with the user
  */
case class User(path: Path,
                name: String,
                id: String,
                arn: UserArn,
                created: Date,
                passwordLastUsed: Option[Date]) {
  /** Returns the AWS account ID to which the user belongs. */
  def account: Account = arn.account
}
