package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.{Account, Arn}

/** Represents the ARN of an Amazon SNS platform application endpoint.
  *
  * @param account the owner of the platform endpoint
  * @param region the region in which the endpoint is located
  * @param platform the type of platform to which the endpoint belongs
  * @param applicationName the name of the application to which the endpoint belongs
  * @param endpointId the unique identifier for the endpoint
  */
case class PlatformEndpointArn(account: Account,
                               region: Region,
                               platform: Platform,
                               applicationName: String,
                               endpointId: String) extends Arn(Arn.Namespace.AmazonSNS, Some(region), account) {
  override val resource = s"endpoint/${platform.name}/$applicationName/$endpointId"
}

object PlatformEndpointArn {
  /** Utility to build/extract `PlatformEndpointArn` instances from strings containing ARNs. */
  object fromArnString {
    /** Builds a `PlatformEndpointArn` object from the given ARN string. */
    def apply(arnString: String): PlatformEndpointArn =
      unapply(arnString)
        .getOrElse(throw new IllegalArgumentException(s"‘$arnString’ is not a valid platform endpoint ARN."))

    /** Extracts a `PlatformEndpointArn` object from the given ARN string. */
    def unapply(arnString: String): Option[PlatformEndpointArn] =
      arnString match {
        case Arn.fromArnString(arn: PlatformEndpointArn) ⇒ Some(arn)
        case _                                           ⇒ None
      }
  }

  private[sns] val platformEndpointArnPF: PartialFunction[Arn.ArnParts, PlatformEndpointArn] = {
    case (_, Arn.Namespace.AmazonSNS, Some(region), Some(account), PlatformEndpointResourceRegex(Platform(platform), applicationName, endpointId)) ⇒
      PlatformEndpointArn(account, region, platform, applicationName, endpointId)
  }

  private val PlatformEndpointResourceRegex = "^endpoint/([A-Z_]+)/([a-zA-Z0-9._-]+)/(.+)$".r
}

