package com.monsanto.arch.awsutil.sqs

import java.util.concurrent.ExecutorService

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.monsanto.arch.awsutil.auth.policy.action.SQSAction
import com.monsanto.arch.awsutil.impl.ShutdownHook
import com.monsanto.arch.awsutil.sqs.model.QueueArn
import com.monsanto.arch.awsutil.{Arn, AwsClientProvider, AwsSettings}

object SQS extends AwsClientProvider[StreamingSQSClient,AsyncSQSClient] {
  private[awsutil] def init(): Unit = {
    SQSAction.registerActions()
    Arn.registerArnPartialFunctions(QueueArn.queueArnPF)
  }

  override private[awsutil] def streamingClient(settings: AwsSettings,
                                                credentialsProvider: AWSCredentialsProvider,
                                                executorService: ExecutorService): (StreamingSQSClient, ShutdownHook) = {
    init()
    val aws = new AmazonSQSAsyncClient(credentialsProvider, executorService)
    aws.setRegion(settings.region)
    val client = new DefaultStreamingSQSClient(aws)
    val shutdownHook = ShutdownHook.clientHook("SNS", aws)
    (client, shutdownHook)
  }

  override private[awsutil] def asyncClient(streamingClient: StreamingSQSClient): AsyncSQSClient =
    new DefaultAsyncSQSClient(streamingClient)
}
