package com.monsanto.arch.awsutil.identitymanagement.model

import java.util.Date

import com.amazonaws.services.identitymanagement.model.{User ⇒ AwsUser}

/** Contains information about an IAM user identity.
  *
  * @param arn the ARN that identifies the user
  * @param name the friendly name identifying the user
  * @param id the stable and unique string identifying the user
  * @param path the path to the user
  * @param created the date and time when the user was created
  * @param passwordLastUsed the date and time when the user’s password was last used to sign in to an AWS website.  May
  *                         be `None` if the user does not have a password, has never used the password, or there is no
  *                         sign-in data associated with the user
  */
case class User(path: String,
                name: String,
                id: String,
                arn: String,
                created: Date,
                passwordLastUsed: Option[Date]) {
  /** Returns the AWS account ID to which the user belongs. */
  def account: String = UserArn(arn).account.id

  /** Builds the equivalent AWS `User` object from this object. */
  def toAws: AwsUser = {
    val aws = new AwsUser(path, name, id, arn, created)
    passwordLastUsed.foreach(d ⇒ aws.setPasswordLastUsed(d))
    aws
  }
}

object User {
  /** Builds the equivalent Scala `User` object from the AWS object. */
  def fromAws(aws: AwsUser): User =
    User(aws.getPath, aws.getUserName, aws.getUserId, aws.getArn, aws.getCreateDate, Option(aws.getPasswordLastUsed))
}
