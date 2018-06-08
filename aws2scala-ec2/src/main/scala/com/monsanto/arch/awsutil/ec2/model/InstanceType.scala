package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

sealed abstract class InstanceType(val toAws: aws.InstanceType) extends AwsEnumeration[aws.InstanceType]

object InstanceType extends AwsEnumerationCompanion[InstanceType, aws.InstanceType] {

  case object T1Micro extends InstanceType(aws.InstanceType.T1Micro)

  case object T2Nano extends InstanceType(aws.InstanceType.T2Nano)

  case object T2Micro extends InstanceType(aws.InstanceType.T2Micro)

  case object T2Small extends InstanceType(aws.InstanceType.T2Small)

  case object T2Medium extends InstanceType(aws.InstanceType.T2Medium)

  case object T2Large extends InstanceType(aws.InstanceType.T2Large)

  case object T2Xlarge extends InstanceType(aws.InstanceType.T2Xlarge)

  case object T22xlarge extends InstanceType(aws.InstanceType.T22xlarge)

  case object M1Small extends InstanceType(aws.InstanceType.M1Small)

  case object M1Medium extends InstanceType(aws.InstanceType.M1Medium)

  case object M1Large extends InstanceType(aws.InstanceType.M1Large)

  case object M1Xlarge extends InstanceType(aws.InstanceType.M1Xlarge)

  case object M3Medium extends InstanceType(aws.InstanceType.M3Medium)

  case object M3Large extends InstanceType(aws.InstanceType.M3Large)

  case object M3Xlarge extends InstanceType(aws.InstanceType.M3Xlarge)

  case object M32xlarge extends InstanceType(aws.InstanceType.M32xlarge)

  case object M4Large extends InstanceType(aws.InstanceType.M4Large)

  case object M4Xlarge extends InstanceType(aws.InstanceType.M4Xlarge)

  case object M42xlarge extends InstanceType(aws.InstanceType.M42xlarge)

  case object M44xlarge extends InstanceType(aws.InstanceType.M44xlarge)

  case object M410xlarge extends InstanceType(aws.InstanceType.M410xlarge)

  case object M416xlarge extends InstanceType(aws.InstanceType.M416xlarge)

  case object M2Xlarge extends InstanceType(aws.InstanceType.M2Xlarge)

  case object M22xlarge extends InstanceType(aws.InstanceType.M22xlarge)

  case object M24xlarge extends InstanceType(aws.InstanceType.M24xlarge)

  case object Cr18xlarge extends InstanceType(aws.InstanceType.Cr18xlarge)

  case object R3Large extends InstanceType(aws.InstanceType.R3Large)

  case object R3Xlarge extends InstanceType(aws.InstanceType.R3Xlarge)

  case object R32xlarge extends InstanceType(aws.InstanceType.R32xlarge)

  case object R34xlarge extends InstanceType(aws.InstanceType.R34xlarge)

  case object R38xlarge extends InstanceType(aws.InstanceType.R38xlarge)

  case object R4Large extends InstanceType(aws.InstanceType.R4Large)

  case object R4Xlarge extends InstanceType(aws.InstanceType.R4Xlarge)

  case object R42xlarge extends InstanceType(aws.InstanceType.R42xlarge)

  case object R44xlarge extends InstanceType(aws.InstanceType.R44xlarge)

  case object R48xlarge extends InstanceType(aws.InstanceType.R48xlarge)

  case object R416xlarge extends InstanceType(aws.InstanceType.R416xlarge)

  case object X116xlarge extends InstanceType(aws.InstanceType.X116xlarge)

  case object X132xlarge extends InstanceType(aws.InstanceType.X132xlarge)

  case object X1eXlarge extends InstanceType(aws.InstanceType.X1eXlarge)

  case object X1e2xlarge extends InstanceType(aws.InstanceType.X1e2xlarge)

  case object X1e4xlarge extends InstanceType(aws.InstanceType.X1e4xlarge)

  case object X1e8xlarge extends InstanceType(aws.InstanceType.X1e8xlarge)

  case object X1e16xlarge extends InstanceType(aws.InstanceType.X1e16xlarge)

  case object X1e32xlarge extends InstanceType(aws.InstanceType.X1e32xlarge)

  case object I2Xlarge extends InstanceType(aws.InstanceType.I2Xlarge)

  case object I22xlarge extends InstanceType(aws.InstanceType.I22xlarge)

  case object I24xlarge extends InstanceType(aws.InstanceType.I24xlarge)

  case object I28xlarge extends InstanceType(aws.InstanceType.I28xlarge)

  case object I3Large extends InstanceType(aws.InstanceType.I3Large)

  case object I3Xlarge extends InstanceType(aws.InstanceType.I3Xlarge)

  case object I32xlarge extends InstanceType(aws.InstanceType.I32xlarge)

  case object I34xlarge extends InstanceType(aws.InstanceType.I34xlarge)

  case object I38xlarge extends InstanceType(aws.InstanceType.I38xlarge)

  case object I316xlarge extends InstanceType(aws.InstanceType.I316xlarge)

  case object I3Metal extends InstanceType(aws.InstanceType.I3Metal)

  case object Hi14xlarge extends InstanceType(aws.InstanceType.Hi14xlarge)

  case object Hs18xlarge extends InstanceType(aws.InstanceType.Hs18xlarge)

  case object C1Medium extends InstanceType(aws.InstanceType.C1Medium)

  case object C1Xlarge extends InstanceType(aws.InstanceType.C1Xlarge)

  case object C3Large extends InstanceType(aws.InstanceType.C3Large)

  case object C3Xlarge extends InstanceType(aws.InstanceType.C3Xlarge)

  case object C32xlarge extends InstanceType(aws.InstanceType.C32xlarge)

  case object C34xlarge extends InstanceType(aws.InstanceType.C34xlarge)

  case object C38xlarge extends InstanceType(aws.InstanceType.C38xlarge)

  case object C4Large extends InstanceType(aws.InstanceType.C4Large)

  case object C4Xlarge extends InstanceType(aws.InstanceType.C4Xlarge)

  case object C42xlarge extends InstanceType(aws.InstanceType.C42xlarge)

  case object C44xlarge extends InstanceType(aws.InstanceType.C44xlarge)

  case object C48xlarge extends InstanceType(aws.InstanceType.C48xlarge)

  case object C5Large extends InstanceType(aws.InstanceType.C5Large)

  case object C5Xlarge extends InstanceType(aws.InstanceType.C5Xlarge)

  case object C52xlarge extends InstanceType(aws.InstanceType.C52xlarge)

  case object C54xlarge extends InstanceType(aws.InstanceType.C54xlarge)

  case object C59xlarge extends InstanceType(aws.InstanceType.C59xlarge)

  case object C518xlarge extends InstanceType(aws.InstanceType.C518xlarge)

  case object C5dLarge extends InstanceType(aws.InstanceType.C5dLarge)

  case object C5dXlarge extends InstanceType(aws.InstanceType.C5dXlarge)

  case object C5d2xlarge extends InstanceType(aws.InstanceType.C5d2xlarge)

  case object C5d4xlarge extends InstanceType(aws.InstanceType.C5d4xlarge)

  case object C5d9xlarge extends InstanceType(aws.InstanceType.C5d9xlarge)

  case object C5d18xlarge extends InstanceType(aws.InstanceType.C5d18xlarge)

  case object Cc14xlarge extends InstanceType(aws.InstanceType.Cc14xlarge)

  case object Cc28xlarge extends InstanceType(aws.InstanceType.Cc28xlarge)

  case object G22xlarge extends InstanceType(aws.InstanceType.G22xlarge)

  case object G28xlarge extends InstanceType(aws.InstanceType.G28xlarge)

  case object G34xlarge extends InstanceType(aws.InstanceType.G34xlarge)

  case object G38xlarge extends InstanceType(aws.InstanceType.G38xlarge)

  case object G316xlarge extends InstanceType(aws.InstanceType.G316xlarge)

  case object Cg14xlarge extends InstanceType(aws.InstanceType.Cg14xlarge)

  case object P2Xlarge extends InstanceType(aws.InstanceType.P2Xlarge)

  case object P28xlarge extends InstanceType(aws.InstanceType.P28xlarge)

  case object P216xlarge extends InstanceType(aws.InstanceType.P216xlarge)

  case object P32xlarge extends InstanceType(aws.InstanceType.P32xlarge)

  case object P38xlarge extends InstanceType(aws.InstanceType.P38xlarge)

  case object P316xlarge extends InstanceType(aws.InstanceType.P316xlarge)

  case object D2Xlarge extends InstanceType(aws.InstanceType.D2Xlarge)

  case object D22xlarge extends InstanceType(aws.InstanceType.D22xlarge)

  case object D24xlarge extends InstanceType(aws.InstanceType.D24xlarge)

  case object D28xlarge extends InstanceType(aws.InstanceType.D28xlarge)

  case object F12xlarge extends InstanceType(aws.InstanceType.F12xlarge)

  case object F116xlarge extends InstanceType(aws.InstanceType.F116xlarge)

  case object M5Large extends InstanceType(aws.InstanceType.M5Large)

  case object M5Xlarge extends InstanceType(aws.InstanceType.M5Xlarge)

  case object M52xlarge extends InstanceType(aws.InstanceType.M52xlarge)

  case object M54xlarge extends InstanceType(aws.InstanceType.M54xlarge)

  case object M512xlarge extends InstanceType(aws.InstanceType.M512xlarge)

  case object M524xlarge extends InstanceType(aws.InstanceType.M524xlarge)

  case object M5dLarge extends InstanceType(aws.InstanceType.M5dLarge)

  case object M5dXlarge extends InstanceType(aws.InstanceType.M5dXlarge)

  case object M5d2xlarge extends InstanceType(aws.InstanceType.M5d2xlarge)

  case object M5d4xlarge extends InstanceType(aws.InstanceType.M5d4xlarge)

  case object M5d12xlarge extends InstanceType(aws.InstanceType.M5d12xlarge)

  case object M5d24xlarge extends InstanceType(aws.InstanceType.M5d24xlarge)

  case object H12xlarge extends InstanceType(aws.InstanceType.H12xlarge)

  case object H14xlarge extends InstanceType(aws.InstanceType.H14xlarge)

  case object H18xlarge extends InstanceType(aws.InstanceType.H18xlarge)

  case object H116xlarge extends InstanceType(aws.InstanceType.H116xlarge)

  override def values: Seq[InstanceType] = Seq(
    T1Micro, T2Nano, T2Micro, T2Small, T2Medium, T2Large, T2Xlarge, T22xlarge, M1Small, M1Medium,
    M1Large, M1Xlarge, M3Medium, M3Large, M3Xlarge, M32xlarge, M4Large, M4Xlarge, M42xlarge, M44xlarge,
    M410xlarge, M416xlarge, M2Xlarge, M22xlarge, M24xlarge, Cr18xlarge, R3Large, R3Xlarge, R32xlarge,
    R34xlarge, R38xlarge, R4Large, R4Xlarge, R42xlarge, R44xlarge, R48xlarge, R416xlarge, X116xlarge,
    X132xlarge, X1eXlarge, X1e2xlarge, X1e4xlarge, X1e8xlarge, X1e16xlarge, X1e32xlarge, I2Xlarge,
    I22xlarge, I24xlarge, I28xlarge, I3Large, I3Xlarge, I32xlarge, I34xlarge, I38xlarge, I316xlarge,
    I3Metal, Hi14xlarge, Hs18xlarge, C1Medium, C1Xlarge, C3Large, C3Xlarge, C32xlarge, C34xlarge,
    C38xlarge, C4Large, C4Xlarge, C42xlarge, C44xlarge, C48xlarge, C5Large, C5Xlarge, C52xlarge,
    C54xlarge, C59xlarge, C518xlarge, C5dLarge, C5dXlarge, C5d2xlarge, C5d4xlarge, C5d9xlarge, C5d18xlarge,
    Cc14xlarge, Cc28xlarge, G22xlarge, G28xlarge, G34xlarge, G38xlarge, G316xlarge, Cg14xlarge, P2Xlarge,
    P28xlarge, P216xlarge, P32xlarge, P38xlarge, P316xlarge, D2Xlarge, D22xlarge, D24xlarge, D28xlarge, F12xlarge,
    F116xlarge, M5Large, M5Xlarge, M52xlarge, M54xlarge, M512xlarge, M524xlarge, M5dLarge, M5dXlarge, M5d2xlarge,
    M5d4xlarge, M5d12xlarge, M5d24xlarge, H12xlarge, H14xlarge, H18xlarge, H116xlarge
  )
}
