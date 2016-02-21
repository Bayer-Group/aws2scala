package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.model.{GroupIdentifier ⇒ AwsGroupIdentifier}

/** Describes a security group.
  *
  * @param id the ID of the security group
  * @param name the name of the security group
  */
case class GroupIdentifier private[ec2] (id: String, name: String)

object GroupIdentifier {
  /** Converts an AWS `GroupIdentifier` to its Scala equivalent. */
  private[ec2] def fromAws(aws: AwsGroupIdentifier): GroupIdentifier = GroupIdentifier(aws.getGroupId, aws.getGroupName)
}
