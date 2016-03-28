package com.monsanto.arch.awsutil.rds

import java.util.concurrent.ExecutorService

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.rds.AmazonRDSAsyncClient
import com.monsanto.arch.awsutil.impl.ShutdownHook
import com.monsanto.arch.awsutil.{AwsClientProvider, AwsSettings}

object RDS extends AwsClientProvider[StreamingRDSClient,AsyncRDSClient] {
  override private[awsutil] def streamingClient(settings: AwsSettings, credentialsProvider: AWSCredentialsProvider,
                                                executorService: ExecutorService): (StreamingRDSClient, ShutdownHook) = {
    val aws = new AmazonRDSAsyncClient(credentialsProvider, executorService)
    aws.setRegion(settings.region)
    val client = new DefaultStreamingRDSClient(aws)
    val shutdownHook = ShutdownHook.clientHook("RDS", aws)
    (client, shutdownHook)
  }

  override private[awsutil] def asyncClient(streamingClient: StreamingRDSClient): AsyncRDSClient =
    new DefaultAsyncRDSClient(streamingClient)
}
