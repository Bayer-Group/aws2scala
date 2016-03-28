package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

sealed abstract class HypervisorType(val toAws: aws.HypervisorType) extends AwsEnumeration[aws.HypervisorType]

object HypervisorType extends AwsEnumerationCompanion[HypervisorType,aws.HypervisorType] {
  case object Ovm extends HypervisorType(aws.HypervisorType.Ovm)
  case object Xen extends HypervisorType(aws.HypervisorType.Xen)

  override val values: Seq[HypervisorType] = Seq(Ovm, Xen)
}
