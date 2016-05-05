package com.monsanto.arch.awsutil.identitymanagement

import java.util.concurrent.ExecutorService

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementAsyncClient
import com.monsanto.arch.awsutil.auth.policy.action.IdentityManagementAction
import com.monsanto.arch.awsutil.impl.ShutdownHook
import com.monsanto.arch.awsutil.{AwsClientProvider, AwsSettings}

object IdentityManagement extends AwsClientProvider[StreamingIdentityManagementClient,AsyncIdentityManagementClient] {
  IdentityManagementAction.registerActions()

  override private[awsutil] def streamingClient(settings: AwsSettings, credentialsProvider: AWSCredentialsProvider,
                                                executorService: ExecutorService) = {
    val aws = new AmazonIdentityManagementAsyncClient(credentialsProvider, executorService)
    aws.setRegion(settings.region)
    val client = new DefaultStreamingIdentityManagementClient(aws)
    val shutdownHook = ShutdownHook.clientHook("IAM", aws)
    (client, shutdownHook)
  }

  override private[awsutil] def asyncClient(streamingClient: StreamingIdentityManagementClient) =
    new DefaultAsyncIdentityManagementClient(streamingClient)
}
