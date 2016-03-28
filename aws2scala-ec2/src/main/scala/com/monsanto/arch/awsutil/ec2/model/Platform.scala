package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

sealed abstract class Platform(val toAws: aws.PlatformValues) extends AwsEnumeration[aws.PlatformValues]

object Platform extends AwsEnumerationCompanion[Platform,aws.PlatformValues] {
  case object Windows extends Platform(aws.PlatformValues.Windows)

  override val values: Seq[Platform] = Seq(Windows)
}
