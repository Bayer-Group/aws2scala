package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.model.{InstanceType ⇒ AwsInstanceType}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

sealed abstract class InstanceType(val toAws: AwsInstanceType) extends AwsEnumeration[AwsInstanceType]

object InstanceType extends AwsEnumerationCompanion[InstanceType] {
  case object T1Micro extends InstanceType(AwsInstanceType.T1Micro)
  case object M1Small extends InstanceType(AwsInstanceType.M1Small)
  case object M1Medium extends InstanceType(AwsInstanceType.M1Medium)
  case object M1Large extends InstanceType(AwsInstanceType.M1Large)
  case object M1Xlarge extends InstanceType(AwsInstanceType.M1Xlarge)
  case object M3Medium extends InstanceType(AwsInstanceType.M3Medium)
  case object M3Large extends InstanceType(AwsInstanceType.M3Large)
  case object M3Xlarge extends InstanceType(AwsInstanceType.M3Xlarge)
  case object M32xlarge extends InstanceType(AwsInstanceType.M32xlarge)
  case object M4Large extends InstanceType(AwsInstanceType.M4Large)
  case object M4Xlarge extends InstanceType(AwsInstanceType.M4Xlarge)
  case object M42xlarge extends InstanceType(AwsInstanceType.M42xlarge)
  case object M44xlarge extends InstanceType(AwsInstanceType.M44xlarge)
  case object M410xlarge extends InstanceType(AwsInstanceType.M410xlarge)
  case object T2Nano extends InstanceType(AwsInstanceType.T2Nano)
  case object T2Micro extends InstanceType(AwsInstanceType.T2Micro)
  case object T2Small extends InstanceType(AwsInstanceType.T2Small)
  case object T2Medium extends InstanceType(AwsInstanceType.T2Medium)
  case object T2Large extends InstanceType(AwsInstanceType.T2Large)
  case object M2Xlarge extends InstanceType(AwsInstanceType.M2Xlarge)
  case object M22xlarge extends InstanceType(AwsInstanceType.M22xlarge)
  case object M24xlarge extends InstanceType(AwsInstanceType.M24xlarge)
  case object Cr18xlarge extends InstanceType(AwsInstanceType.Cr18xlarge)
  case object I2Xlarge extends InstanceType(AwsInstanceType.I2Xlarge)
  case object I22xlarge extends InstanceType(AwsInstanceType.I22xlarge)
  case object I24xlarge extends InstanceType(AwsInstanceType.I24xlarge)
  case object I28xlarge extends InstanceType(AwsInstanceType.I28xlarge)
  case object Hi14xlarge extends InstanceType(AwsInstanceType.Hi14xlarge)
  case object Hs18xlarge extends InstanceType(AwsInstanceType.Hs18xlarge)
  case object C1Medium extends InstanceType(AwsInstanceType.C1Medium)
  case object C1Xlarge extends InstanceType(AwsInstanceType.C1Xlarge)
  case object C3Large extends InstanceType(AwsInstanceType.C3Large)
  case object C3Xlarge extends InstanceType(AwsInstanceType.C3Xlarge)
  case object C32xlarge extends InstanceType(AwsInstanceType.C32xlarge)
  case object C34xlarge extends InstanceType(AwsInstanceType.C34xlarge)
  case object C38xlarge extends InstanceType(AwsInstanceType.C38xlarge)
  case object C4Large extends InstanceType(AwsInstanceType.C4Large)
  case object C4Xlarge extends InstanceType(AwsInstanceType.C4Xlarge)
  case object C42xlarge extends InstanceType(AwsInstanceType.C42xlarge)
  case object C44xlarge extends InstanceType(AwsInstanceType.C44xlarge)
  case object C48xlarge extends InstanceType(AwsInstanceType.C48xlarge)
  case object Cc14xlarge extends InstanceType(AwsInstanceType.Cc14xlarge)
  case object Cc28xlarge extends InstanceType(AwsInstanceType.Cc28xlarge)
  case object G22xlarge extends InstanceType(AwsInstanceType.G22xlarge)
  case object G28xlarge extends InstanceType(AwsInstanceType.G28xlarge)
  case object Cg14xlarge extends InstanceType(AwsInstanceType.Cg14xlarge)
  case object R3Large extends InstanceType(AwsInstanceType.R3Large)
  case object R3Xlarge extends InstanceType(AwsInstanceType.R3Xlarge)
  case object R32xlarge extends InstanceType(AwsInstanceType.R32xlarge)
  case object R34xlarge extends InstanceType(AwsInstanceType.R34xlarge)
  case object R38xlarge extends InstanceType(AwsInstanceType.R38xlarge)
  case object D2Xlarge extends InstanceType(AwsInstanceType.D2Xlarge)
  case object D22xlarge extends InstanceType(AwsInstanceType.D22xlarge)
  case object D24xlarge extends InstanceType(AwsInstanceType.D24xlarge)
  case object D28xlarge extends InstanceType(AwsInstanceType.D28xlarge)

  override def values: Seq[InstanceType] = Seq(
    T1Micro, M1Small, M1Medium, M1Large, M1Xlarge, M3Medium, M3Large, M3Xlarge, M32xlarge, M4Large, M4Xlarge,
    M42xlarge, M44xlarge, M410xlarge, T2Nano, T2Micro, T2Small, T2Medium, T2Large, M2Xlarge, M22xlarge, M24xlarge,
    Cr18xlarge, I2Xlarge, I22xlarge, I24xlarge, I28xlarge, Hi14xlarge, Hs18xlarge, C1Medium, C1Xlarge, C3Large,
    C3Xlarge, C32xlarge, C34xlarge, C38xlarge, C4Large, C4Xlarge, C42xlarge, C44xlarge, C48xlarge, Cc14xlarge,
    Cc28xlarge, G22xlarge, G28xlarge, Cg14xlarge, R3Large, R3Xlarge, R32xlarge, R34xlarge, R38xlarge, D2Xlarge,
    D22xlarge, D24xlarge, D28xlarge
  )
}
