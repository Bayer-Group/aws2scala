package com.monsanto.arch.awsutil.sns

import java.util.concurrent.ExecutorService

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import com.monsanto.arch.awsutil.auth.policy.Action
import com.monsanto.arch.awsutil.auth.policy.action.SNSAction
import com.monsanto.arch.awsutil.impl.ShutdownHook
import com.monsanto.arch.awsutil.{AwsClientProvider, AwsSettings}

object SNS extends AwsClientProvider[StreamingSNSClient,AsyncSNSClient] {
  Action.registerActions(SNSAction.values)

  override private[awsutil] def streamingClient(settings: AwsSettings,
                                                credentialsProvider: AWSCredentialsProvider,
                                                executorService: ExecutorService): (StreamingSNSClient, ShutdownHook) = {
    val aws = new AmazonSNSAsyncClient(credentialsProvider, executorService)
    aws.setRegion(settings.region)
    val client = new DefaultStreamingSNSClient(aws)
    val shutdownHook = ShutdownHook.clientHook("SNS", aws)
    (client, shutdownHook)
  }

  override private[awsutil] def asyncClient(streamingClient: StreamingSNSClient): AsyncSNSClient =
    new DefaultAsyncSNSClient(streamingClient)
}
