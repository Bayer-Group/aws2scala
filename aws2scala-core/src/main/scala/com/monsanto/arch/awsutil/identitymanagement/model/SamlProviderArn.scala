package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.{Account, Arn}

/** Represents the ARN of a SAML identity provider.
  *
  * @param account the account for which the provider has been set up
  * @param name the name of the SAML provider
  */
case class SamlProviderArn(account: Account, name: String) extends Arn(Arn.Namespace.IAM, None, account) {
  /** Returns the service-dependent content identifying the resource. */
  override def resource: String = s"saml-provider/$name"
}

private[awsutil] object SamlProviderArn {
  /** Utility to build/extract `SamlProviderArn` instances from strings containing ARNs. */
  object fromArnString {
    /** Builds a `SamlProviderArn` object from the given ARN string. */
    def apply(arnString: String): SamlProviderArn =
      unapply(arnString)
        .getOrElse(throw new IllegalArgumentException(s"‘$arnString’ is not a valid SAML provider ARN."))

    /** Extracts a `SamlProviderArn` object from the given ARN string. */
    def unapply(arnString: String): Option[SamlProviderArn] =
      arnString match {
        case Arn.fromArnString(arn: SamlProviderArn) ⇒ Some(arn)
        case _                                       ⇒ None
      }
  }

  private[awsutil] val samlProviderArnPF: PartialFunction[Arn.ArnParts, SamlProviderArn] = {
    case (_, Arn.Namespace.IAM, None, Some(account), SamlProviderName(name)) ⇒ SamlProviderArn(account, name)
  }

  private val SamlProviderName = "^saml-provider/(.+)$".r
}
