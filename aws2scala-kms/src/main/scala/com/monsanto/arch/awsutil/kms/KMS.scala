package com.monsanto.arch.awsutil.kms

import java.util.concurrent.ExecutorService

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.kms.AWSKMSAsyncClient
import com.monsanto.arch.awsutil.impl.ShutdownHook
import com.monsanto.arch.awsutil.{AwsClientProvider, AwsSettings}

object KMS extends AwsClientProvider[StreamingKMSClient,AsyncKMSClient] {
  override private[awsutil] def streamingClient(settings: AwsSettings, credentialsProvider: AWSCredentialsProvider,
                                                executorService: ExecutorService): (StreamingKMSClient, ShutdownHook) = {
    val aws = new AWSKMSAsyncClient(credentialsProvider, executorService)
    aws.setRegion(settings.region)
    val client = new DefaultStreamingKMSClient(aws)
    val shutdownHook = ShutdownHook.clientHook("KMS", aws)
    (client, shutdownHook)
  }

  override private[awsutil] def asyncClient(streamingClient: StreamingKMSClient): AsyncKMSClient =
    new DefaultAsyncKMSClient(streamingClient)
}
