package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

sealed abstract class Tenancy(val toAws: aws.Tenancy) extends AwsEnumeration[aws.Tenancy]

object Tenancy extends AwsEnumerationCompanion[Tenancy,aws.Tenancy] {
  case object Default extends Tenancy(aws.Tenancy.Default)
  case object Dedicated extends Tenancy(aws.Tenancy.Dedicated)
  case object Host extends Tenancy(aws.Tenancy.Host)

  override val values: Seq[Tenancy] = Seq(Default, Dedicated, Host)
}
