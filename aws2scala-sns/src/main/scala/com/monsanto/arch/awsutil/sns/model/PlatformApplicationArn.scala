package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.{Account, Arn}

/** Represents the ARN of an Amazon SNS platform application.
  *
  * @param account the owner of the platform application
  * @param region the region in which the application is located
  * @param platform the type of platform on which the application runs
  * @param name the name of the application
  */
case class PlatformApplicationArn(account: Account,
                                  region: Region,
                                  platform: Platform,
                                  name: String) extends Arn(Arn.Namespace.AmazonSNS, Some(region), account) {
  override val resource = s"app/${platform.name}/$name"
}

object PlatformApplicationArn {
  /** Utility to build/extract `PlatformApplicationArn` instances from strings containing ARNs. */
  object fromArnString {
    /** Builds a `PlatformApplicationArn` object from the given ARN string. */
    def apply(arnString: String): PlatformApplicationArn =
      unapply(arnString)
        .getOrElse(throw new IllegalArgumentException(s"‘$arnString’ is not a valid platform application ARN."))

    /** Extracts a `PlatformApplicationArn` object from the given ARN string. */
    def unapply(arnString: String): Option[PlatformApplicationArn] =
      arnString match {
        case Arn.fromArnString(arn: PlatformApplicationArn) ⇒ Some(arn)
        case _                                              ⇒ None
      }
  }

  private[sns] val platformApplicationArnPF: PartialFunction[Arn.ArnParts, PlatformApplicationArn] = {
    case (_, Arn.Namespace.AmazonSNS, Some(region), Some(account), PlatformApplicationResourceRegex(Platform(platform), name)) ⇒
      PlatformApplicationArn(account, region, platform, name)
  }

  private val PlatformApplicationResourceRegex = "^app/([A-Z_]+)/([a-zA-Z0-9._-]+)$".r
}
