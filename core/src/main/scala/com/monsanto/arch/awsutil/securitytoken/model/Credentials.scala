package com.monsanto.arch.awsutil.securitytoken.model

import java.util.Date

import com.amazonaws.auth.{AWSSessionCredentials, AWSSessionCredentialsProvider, BasicSessionCredentials}
import com.amazonaws.services.securitytoken.model.{Credentials ⇒ AwsCredentials}

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
  /** Builds the equivalent `Credentials` AWS object from this object. */
  def toAws: AwsCredentials = new AwsCredentials(accessKeyId,secretAccessKey,sessionToken, expiration)

  override lazy val getCredentials: AWSSessionCredentials =
    new BasicSessionCredentials(accessKeyId, secretAccessKey, sessionToken)

  override def refresh() = ()
}

object Credentials {
  /** Builds the equivalent `Credentials` object from an AWS object. */
  def fromAws(aws: AwsCredentials): Credentials =
    Credentials(aws.getAccessKeyId, aws.getSecretAccessKey, aws.getSessionToken, aws.getExpiration)
}
