package com.monsanto.arch.awsutil.cloudformation

import java.util.concurrent.ExecutorService

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient
import com.monsanto.arch.awsutil.impl.ShutdownHook
import com.monsanto.arch.awsutil.{AwsClientProvider, AwsSettings}

object CloudFormation extends AwsClientProvider[StreamingCloudFormationClient,AsyncCloudFormationClient] {
  override private[awsutil] def streamingClient(settings: AwsSettings, credentialsProvider: AWSCredentialsProvider,
                                                executorService: ExecutorService) = {
    val aws = new AmazonCloudFormationAsyncClient(credentialsProvider, executorService)
    aws.setRegion(settings.region)
    val client = new DefaultStreamingCloudFormationClient(aws)
    val shutdownHook = ShutdownHook.clientHook("CloudFormation", aws)
    (client, shutdownHook)
  }

  override private[awsutil] def asyncClient(streamingClient: StreamingCloudFormationClient) =
    new DefaultAsyncCloudFormationClient(streamingClient)
}
