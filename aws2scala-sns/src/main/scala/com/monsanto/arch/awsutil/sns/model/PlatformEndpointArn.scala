package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.{Account, Arn}

private[awsutil] case class PlatformEndpointArn(account: Account,
                                                region: Region,
                                                platform: Platform,
                                                applicationName: String,
                                                endpointId: String) extends Arn(Arn.Namespace.AmazonSNS, Some(region), account) {
  override val resource = s"endpoint/${platform.name}/$applicationName/$endpointId"
}

private[awsutil] object PlatformEndpointArn {
  def apply(arn: String): PlatformEndpointArn = {
    arn match {
      case Arn(_, Arn.Namespace.AmazonSNS, Some(region), Some(account), PlatformEndpointResourceRegex(Platform(platform), applicationName, endpointId)) ⇒
        PlatformEndpointArn(account, region, platform, applicationName, endpointId)
      case _ ⇒ throw new IllegalArgumentException(s"’$arn‘ is not a valid platform endpoint ARN")
    }
  }

  private val PlatformEndpointResourceRegex = "^endpoint/([A-Z_]+)/([a-zA-Z0-9._-]+)/(.+)$".r
}

