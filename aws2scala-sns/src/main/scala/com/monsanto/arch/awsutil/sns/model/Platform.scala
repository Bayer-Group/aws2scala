package com.monsanto.arch.awsutil.sns.model

/** Identifies a push notification platform.
  *
  * @param name the name of the platform as identified by AWS
  */
sealed abstract class Platform(val name: String)

object Platform {
  /** The Amazon Device Messaging (ADM) platform. */
  case object ADM extends Platform("ADM") {
    /** Creates credentials for a ADM platform application.
      *
      * @param clientId     the client ID from your application’s security profile
      * @param clientSecret the client secret from your application’s security profile
      * @return credentials to use for creating a ADM platform application
      */
    def apply(clientId: String, clientSecret: String): PlatformApplicationCredentials =
      PlatformApplicationCredentials(this, clientId, clientSecret)
  }

  /** The Apple Push Notification Service (APNs) platform (development). */
  case object APNSDevelopment extends Platform("APNS_SANDBOX") {
    /** Creates credentials for a APNs development platform application.
      *
      * @param certificate the PEM-encoded certificate for your application provided by Apple
      * @param key         the PEM-encoded private key for your certificate
      * @return credentials to use for creating a development APNs platform application
      */
    def apply(certificate: String, key: String): PlatformApplicationCredentials =
      PlatformApplicationCredentials(this, certificate, key)
  }

  /** The Apple Push Notification Service (APNs) platform (production). */
  case object APNSProduction extends Platform("APNS") {
    /** Creates credentials for a APNs production platform application.
      *
      * @param certificate the PEM-encoded certificate for your application provided by Apple
      * @param key         the PEM-encoded private key for your certificate
      * @return credentials to use for creating a production APNs platform application
      */
    def apply(certificate: String, key: String): PlatformApplicationCredentials =
      PlatformApplicationCredentials(this, certificate, key)
  }

  /** The Baidu Cloud Push platform. */
  case object Baidu extends Platform("BAIDU") {
    /** Creates credentials for a Baidu platform application.
      *
      * @param apiKey    the ''API Key'' from your Baidu Cloud Push Project
      * @param secretKey the ''Secret Key'' from your Baidu Cloud Push Project
      * @return credentials to use for creating a Baidu platform application
      */
    def apply(apiKey: String, secretKey: String): PlatformApplicationCredentials =
      PlatformApplicationCredentials(this, apiKey, secretKey)
  }

  /** The Google Cloud Messaging for Android (GCM) platform. */
  case object GCM extends Platform("GCM") {
    /** Creates credentials for a GCM platform application.
      *
      * @param serverApiKey the ''Server API Key'' obtained from Google Play Services
      * @return credentials to use for creating a GCM platform application
      */
    def apply(serverApiKey: String): PlatformApplicationCredentials =
      PlatformApplicationCredentials(this, "", serverApiKey)
  }

  /** The Microsoft Push Notification Service for Windows Phone (MPNS) platform. */
  case object MPNS extends Platform("MPNS") {
    /** Creates credentials for an authenticated MPNS platform application.
      *
      * @param tlsCertificateChain a PEM-encoded TLS certificate chain obtained from Microsoft
      * @param privateKey          a PEM-encoded private key for your TLS certificate
      * @return credentials to use for creating an authenticated MPNS platform application
      */
    def apply(tlsCertificateChain: String, privateKey: String): PlatformApplicationCredentials =
      PlatformApplicationCredentials(this, tlsCertificateChain, privateKey)

    /** Creates credentials for an unauthenticated MPNS platform application.
      *
      * @return credentials to use for creating an unauthenticated MPNS platform application
      */
    def apply(): PlatformApplicationCredentials = PlatformApplicationCredentials(this, "", "")
  }

  /** The Windows Push Notification Service (WNS) platform. */
  case object WNS extends Platform("WNS") {
    /** Creates credentials for an authenticated WNS platform application.
      *
      * @param packageSecurityIdentifier the Package Security Identifier (SID) for your application
      * @param secretKey                 a secret key corresponding to the Package Security Identifier
      * @return credentials to use for creating an authenticated MPNS platform application
      */
    def apply(packageSecurityIdentifier: String, secretKey: String): PlatformApplicationCredentials =
      PlatformApplicationCredentials(this, packageSecurityIdentifier, secretKey)
  }

  /** Enumeration of all the valid platform values. */
  val values: Seq[Platform] = Seq(ADM, APNSDevelopment, APNSProduction, Baidu, GCM, MPNS, WNS)

  /** Returns the platform instance corresponding to the given name. */
  def apply(name: String): Platform =
    unapply(name).getOrElse(throw new IllegalArgumentException(s"’$name‘ is not a valid platform name."))

  /** Allows pattern matching on a name. */
  def unapply(name: String): Option[Platform] = values.find(_.name == name)
}

