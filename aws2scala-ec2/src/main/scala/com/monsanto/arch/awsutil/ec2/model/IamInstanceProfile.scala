package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}

/** Describes an IAM instance profile.
  *
  * @param id the ID of the instance profile
  * @param arn the ARN of the instance profile
  */
case class IamInstanceProfile(id: String, arn: String)

object IamInstanceProfile {
  private[ec2] def fromAws(profile: aws.IamInstanceProfile): IamInstanceProfile =
    IamInstanceProfile(profile.getId, profile.getArn)
}
