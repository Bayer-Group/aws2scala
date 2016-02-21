package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

sealed abstract class NetworkInterfaceStatus(val toAws: aws.NetworkInterfaceStatus) extends AwsEnumeration[aws.NetworkInterfaceStatus]

object NetworkInterfaceStatus extends AwsEnumerationCompanion[NetworkInterfaceStatus] {
  case object Attaching extends NetworkInterfaceStatus(aws.NetworkInterfaceStatus.Attaching)
  case object Available extends NetworkInterfaceStatus(aws.NetworkInterfaceStatus.Available)
  case object Detaching extends NetworkInterfaceStatus(aws.NetworkInterfaceStatus.Detaching)
  case object InUse extends NetworkInterfaceStatus(aws.NetworkInterfaceStatus.InUse)

  override val values: Seq[NetworkInterfaceStatus] = Seq(Attaching, Available, Detaching, InUse)
}
