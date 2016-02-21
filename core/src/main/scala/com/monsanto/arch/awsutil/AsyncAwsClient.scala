package com.monsanto.arch.awsutil

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import com.amazonaws.auth.AWSCredentialsProvider
import com.monsanto.arch.awsutil.cloudformation.AsyncCloudFormationClient
import com.monsanto.arch.awsutil.ec2.AsyncEC2Client
import com.monsanto.arch.awsutil.identitymanagement.AsyncIdentityManagementClient
import com.monsanto.arch.awsutil.kms.AsyncKMSClient
import com.monsanto.arch.awsutil.rds.AsyncRDSClient
import com.monsanto.arch.awsutil.s3.AsyncS3Client
import com.monsanto.arch.awsutil.securitytoken.AsyncSecurityTokenServiceClient
import com.monsanto.arch.awsutil.sns.AsyncSNSClient
import com.typesafe.config.Config

trait AsyncAwsClient {
  /** Returns the asynchronous CloudFormation client. */
  def cloudFormation: AsyncCloudFormationClient
  /** Returns the asynchronous EC2 client. */
  def ec2: AsyncEC2Client
  /** Returns the asynchronous IAM client. */
  def identityManagement: AsyncIdentityManagementClient
  /** Returns the asynchronous KMS client. */
  def kms: AsyncKMSClient
  /** Returns the asynchronous RDS client. */
  def rds: AsyncRDSClient
  /** Returns the asynchronous S3 client. */
  def s3: AsyncS3Client
  /** Returns the asynchronous STS client. */
  def securityTokenService: AsyncSecurityTokenServiceClient
  /** Returns the asynchronous SNS client. */
  def sns: AsyncSNSClient

  /** Returns the underlying streaming AWS client. */
  def streamingClient: StreamingAwsClient

  /** Requests graceful termination of all of the asynchronous clients within the given timeout.  Once the timeout
    * expires, termination is allowed to proceed non-gracefully.
    */
  def shutdown(timeout: Timeout = Timeout(1, TimeUnit.MINUTES)): Unit

  /** Given a set of credentials, return a new asynchronous client that shares the same thread pools as this one, but
    * uses the new credentials.  Note that calls to `shutdown()` on the new client will not affect this client.
    *
    * @param credentialsProvider the credentials to use with the new client
    * @return a new asynchronous client that will use the given credentials and share resources with this client
    */
  def withCredentialsProvider(credentialsProvider: AWSCredentialsProvider): AsyncAwsClient
}

object AsyncAwsClient {
  /** Returns an asynchronous AWS client using the given settings. */
  def apply(settings: Settings): AsyncAwsClient = new DefaultStreamingAwsClient(settings).asyncClient
  /** Returns an asynchronous AWS client using the given configuration. */
  def apply(config: Config): AsyncAwsClient = apply(new Settings(config))

  /** A client built using the default settings. */
  lazy val Default: AsyncAwsClient = apply(Settings.Default)
}
