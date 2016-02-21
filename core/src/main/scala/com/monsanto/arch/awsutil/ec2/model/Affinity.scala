package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

sealed abstract class Affinity(val toAws: aws.Affinity) extends AwsEnumeration[aws.Affinity]

object Affinity extends AwsEnumerationCompanion[Affinity] {
  case object Default extends Affinity(aws.Affinity.Default)
  case object Host extends Affinity(aws.Affinity.Host)

  override val values: Seq[Affinity] = Seq(Default, Host)
}
