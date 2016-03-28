package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

sealed abstract class DeviceType(val toAws: aws.DeviceType) extends AwsEnumeration[aws.DeviceType]

object DeviceType extends AwsEnumerationCompanion[DeviceType,aws.DeviceType] {
  case object Ebs extends DeviceType(aws.DeviceType.Ebs)
  case object InstanceStore extends DeviceType(aws.DeviceType.InstanceStore)

  override val values: Seq[DeviceType] = Seq(Ebs, InstanceStore)
}
