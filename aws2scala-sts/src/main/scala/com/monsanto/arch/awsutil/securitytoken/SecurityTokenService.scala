package com.monsanto.arch.awsutil.securitytoken

import java.util.concurrent.ExecutorService

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceAsyncClient
import com.monsanto.arch.awsutil.impl.ShutdownHook
import com.monsanto.arch.awsutil.{AwsClientProvider, AwsSettings}

object SecurityTokenService extends AwsClientProvider[StreamingSecurityTokenServiceClient,AsyncSecurityTokenServiceClient] {
  override private[awsutil] def streamingClient(settings: AwsSettings,
                                                credentialsProvider: AWSCredentialsProvider,
                                                executorService: ExecutorService): (StreamingSecurityTokenServiceClient, ShutdownHook) = {
    val aws = new AWSSecurityTokenServiceAsyncClient(credentialsProvider, executorService)
    aws.setRegion(settings.region)
    val client = new DefaultStreamingSecurityTokenServiceClient(aws)
    val shutdownHook = ShutdownHook.clientHook("SecurityTokenService", aws)
    (client, shutdownHook)
  }

  override private[awsutil] def asyncClient(streamingClient: StreamingSecurityTokenServiceClient): AsyncSecurityTokenServiceClient =
    new DefaultAsyncSecurityTokenServiceClient(streamingClient)
}
