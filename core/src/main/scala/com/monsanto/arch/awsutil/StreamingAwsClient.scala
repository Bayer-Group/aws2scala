package com.monsanto.arch.awsutil

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import com.amazonaws.auth.AWSCredentialsProvider
import com.monsanto.arch.awsutil.cloudformation.StreamingCloudFormationClient
import com.monsanto.arch.awsutil.ec2.StreamingEC2Client
import com.monsanto.arch.awsutil.identitymanagement.StreamingIdentityManagementClient
import com.monsanto.arch.awsutil.kms.StreamingKMSClient
import com.monsanto.arch.awsutil.rds.StreamingRDSClient
import com.monsanto.arch.awsutil.s3.StreamingS3Client
import com.monsanto.arch.awsutil.securitytoken.StreamingSecurityTokenServiceClient
import com.monsanto.arch.awsutil.sns.StreamingSNSClient
import com.typesafe.config.Config

trait StreamingAwsClient {
  /** Returns the streaming Cloud Formation client. */
  def cloudFormation: StreamingCloudFormationClient
  /** Returns the streaming EC2 client. */
  def ec2: StreamingEC2Client
  /** Returns the streaming IAM client. */
  def identityManagement: StreamingIdentityManagementClient
  /** Returns the streaming KMS client. */
  def kms: StreamingKMSClient
  /** Returns the streaming RDS client. */
  def rds: StreamingRDSClient
  /** Returns the streaming S3 client. */
  def s3: StreamingS3Client
  /** Returns the streaming STS client. */
  def securityTokenService: StreamingSecurityTokenServiceClient
  /** Returns the streaming SNS client. */
  def sns: StreamingSNSClient

  /** Returns an asynchronous client built on this streaming client. */
  def asyncClient: AsyncAwsClient

  /** Requests graceful termination of all of the asynchronous clients within the given timeout.  Once the timeout
    * expires, termination is allowed to proceed non-gracefully.
    */
  def shutdown(timeout: Timeout = Timeout(1, TimeUnit.MINUTES)): Unit

  /** Given a set of credentials, return a new streaming client that shares the same thread pools as this one, but
    * uses the new credentials.  Note that calls to `shutdown()` on the new client will not affect this client.
    *
    * @param credentialsProvider the credentials to use with the new client
    * @return a new streaming client that will use the given credentials and share resources with this client
    */
  def withCredentialsProvider(credentialsProvider: AWSCredentialsProvider): StreamingAwsClient
}

object StreamingAwsClient {
  /** Returns a streaming AWS client using the given settings. */
  def apply(settings: Settings): StreamingAwsClient = new DefaultStreamingAwsClient(settings)
  /** Returns a streaming AWS client using the given configuration. */
  def apply(config: Config): StreamingAwsClient = apply(new Settings(config))

  /** A client built using the default settings. */
  lazy val Default: StreamingAwsClient = {
    val client = apply(Settings.Default)

    val shutdownHook = new Thread(new Runnable {
      override def run(): Unit = client.shutdown()
    })

    Runtime.getRuntime.addShutdownHook(shutdownHook)

    client
  }
}
