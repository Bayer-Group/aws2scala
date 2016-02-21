package com.monsanto.arch.awsutil

import akka.util.Timeout
import com.amazonaws.auth.AWSCredentialsProvider
import com.monsanto.arch.awsutil.cloudformation.DefaultAsyncCloudFormationClient
import com.monsanto.arch.awsutil.ec2.DefaultAsyncEC2Client
import com.monsanto.arch.awsutil.identitymanagement.DefaultAsyncIdentityManagementClient
import com.monsanto.arch.awsutil.kms.DefaultAsyncKMSClient
import com.monsanto.arch.awsutil.rds.DefaultAsyncRDSClient
import com.monsanto.arch.awsutil.s3.DefaultAsyncS3Client
import com.monsanto.arch.awsutil.securitytoken.DefaultAsyncSecurityTokenServiceClient
import com.monsanto.arch.awsutil.sns.DefaultAsyncSNSClient

/** Non-blocking handling of Amazon Web Service async calls using Scala futures.
  * Instead of adapting the future, which in this case can't be done because it
  * is a Future, not a CompletableFuture, you can register a handler with the async
  * call that uses a Scala promise to fulfil a future.
  *
  * Note - some delegation is necessary to keep IDEA happy, Scala can handle the
  * chain of types, but IDEA is not as capable, therefore a single line definition
  * for each called method makes IDEA happy and cleans up the API as well.
  */
private[awsutil] class DefaultAsyncAwsClient(val streamingClient: DefaultStreamingAwsClient) extends AsyncAwsClient {
  override lazy val cloudFormation = new DefaultAsyncCloudFormationClient(streamingClient.cloudFormation)

  override lazy val ec2 = new DefaultAsyncEC2Client(streamingClient.ec2)

  override lazy val s3 = new DefaultAsyncS3Client(streamingClient.s3)

  override lazy val rds = new DefaultAsyncRDSClient(streamingClient.rds)

  override lazy val kms = new DefaultAsyncKMSClient(streamingClient.kms)

  override lazy val sns = new DefaultAsyncSNSClient(streamingClient.sns)

  override lazy val securityTokenService = new DefaultAsyncSecurityTokenServiceClient(streamingClient.securityTokenService)

  override lazy val identityManagement = new DefaultAsyncIdentityManagementClient(streamingClient.identityManagement)

  override def shutdown(timeout: Timeout): Unit = streamingClient.shutdown(timeout)

  override def withCredentialsProvider(credentialsProvider: AWSCredentialsProvider) =
    new DefaultAsyncAwsClient(streamingClient.withCredentialsProvider(credentialsProvider))
}
