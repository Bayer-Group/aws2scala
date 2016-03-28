package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.{Account, Arn}

private[awsutil] case class PlatformApplicationArn(account: Account,
                                                   region: Region,
                                                   platform: Platform,
                                                   name: String) extends Arn(Arn.Namespace.AmazonSNS, Some(region), account) {
  override val resource = s"app/${platform.name}/$name"
}

private[awsutil] object PlatformApplicationArn {
  def apply(arn: String): PlatformApplicationArn = {
    arn match {
      case Arn(_, Arn.Namespace.AmazonSNS, Some(region), Some(account), PlatformApplicationResourceRegex(Platform(platform), name)) ⇒
        PlatformApplicationArn(account, region, platform, name)
      case _ ⇒ throw new IllegalArgumentException(s"’$arn‘ is not a valid platform application ARN")
    }
  }

  private val PlatformApplicationResourceRegex = "^app/([A-Z_]+)/([a-zA-Z0-9._-]+)$".r
}
