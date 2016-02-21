package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.model.{IamInstanceProfile ⇒ AwsIamInstanceProfile }

/** Describes an IAM instance profile.
  *
  * @param id the ID of the instance profile
  * @param arn the ARN of the instance profile
  */
case class IamInstanceProfile private[ec2] (id: String, arn: String)

object IamInstanceProfile {
  private[ec2] def fromAws(aws: AwsIamInstanceProfile): IamInstanceProfile =
    IamInstanceProfile(aws.getId, aws.getArn)
}
