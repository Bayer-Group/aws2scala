package com.monsanto.arch.awsutil.securitytoken.model

import java.util.Date

import com.amazonaws.auth.{AWSSessionCredentials, AWSSessionCredentialsProvider, BasicSessionCredentials}

/** Temporary AWS credentials for API authentication.  This class can also serve as a session credentials provider.
  *
  * @param accessKeyId the access key identifier for the temporary security credentials
  * @param secretAccessKey the secret access key that can be used to sign requests
  * @param sessionToken the token that must be passed to the service API to use the temporary credentials
  * @param expiration the date on which the credentials expire
  */
case class Credentials(accessKeyId: String,
                       secretAccessKey: String,
                       sessionToken: String,
                       expiration: Date) extends AWSSessionCredentialsProvider {
  override lazy val getCredentials: AWSSessionCredentials =
    new BasicSessionCredentials(accessKeyId, secretAccessKey, sessionToken)

  override def refresh() = ()
}
