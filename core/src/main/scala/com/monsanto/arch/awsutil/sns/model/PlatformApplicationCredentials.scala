package com.monsanto.arch.awsutil.sns.model

/** Contains the credentials necessary for SNS to use to connect to a push notification platform.
  *
  * @param platform the push notification platform for which to use these credentials
  * @param principal the principal from the notification service.  For ADM this is ''client id'', for APNs this is
  *                  a PEM-encoded ''SSL certificate'', for Baidu this is an ''API key'', for CCM this is an empty
  *                  string, for MPNS this is a PEM-encoded ''TLS certificate'', and for WNS this is a ''Package
  *                  Security Identifier''.
  * @param credential the credential from the notification service.  For ADM this is a ''client secret'', for APNs
  *                   this is a PEM-encoded ''private key'', for Baidu this is a ''secret key'', for GCM this is an
  *                   ''API key'', for MPNS this is a PEM-encoded ''private key'', and for WNS this is a ''secret
  *                   key''.
  */
case class PlatformApplicationCredentials(platform: Platform, principal: String, credential: String)
