package com.monsanto.arch.awsutil.ec2

import java.util.concurrent.ExecutorService

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.ec2.AmazonEC2AsyncClient
import com.monsanto.arch.awsutil.impl.ShutdownHook
import com.monsanto.arch.awsutil.{AwsClientProvider, AwsSettings}

object EC2 extends AwsClientProvider[StreamingEC2Client,AsyncEC2Client] {
  override private[awsutil] def streamingClient(settings: AwsSettings,
                                                credentialsProvider: AWSCredentialsProvider,
                                                executorService: ExecutorService): (StreamingEC2Client, ShutdownHook) = {
    val aws = new AmazonEC2AsyncClient(credentialsProvider, executorService)
    aws.setRegion(settings.region)
    val client = new DefaultStreamingEC2Client(aws)
    val shutdownHook = ShutdownHook.clientHook("EC2", aws)
    (client, shutdownHook)
  }

  override private[awsutil] def asyncClient(streamingClient: StreamingEC2Client): AsyncEC2Client =
    new DefaultAsyncEC2Client(streamingClient)
}
