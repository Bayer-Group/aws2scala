package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.model.{HypervisorType ⇒ AwsHypervisorType}
import com.monsanto.arch.awsutil.util.{AwsEnumerationCompanion, AwsEnumeration}

sealed abstract class HypervisorType(val toAws: AwsHypervisorType) extends AwsEnumeration[AwsHypervisorType]

object HypervisorType extends AwsEnumerationCompanion[HypervisorType] {
  case object Ovm extends HypervisorType(AwsHypervisorType.Ovm)
  case object Xen extends HypervisorType(AwsHypervisorType.Xen)

  override val values: Seq[HypervisorType] = Seq(Ovm, Xen)
}
