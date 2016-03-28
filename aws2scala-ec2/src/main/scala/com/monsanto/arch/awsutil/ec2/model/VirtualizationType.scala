package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

sealed abstract class VirtualizationType(val toAws: aws.VirtualizationType) extends AwsEnumeration[aws.VirtualizationType]

object VirtualizationType extends AwsEnumerationCompanion[VirtualizationType,aws.VirtualizationType] {
  case object Hvm extends VirtualizationType(aws.VirtualizationType.Hvm)
  case object Paravirtual extends VirtualizationType(aws.VirtualizationType.Paravirtual)

  override val values: Seq[VirtualizationType] = Seq(Hvm, Paravirtual)
}
