package com.monsanto.arch.awsutil

import java.util.concurrent.ExecutorService

import com.amazonaws.auth.AWSCredentialsProvider
import com.monsanto.arch.awsutil.impl.ShutdownHook

/** Serves as the interface provided by clients to [[com.monsanto.arch.awsutil.AwsClient AwsClient]] so that it can
  * build clients.
  */
trait AwsClientProvider[T <: StreamingAwsClient, U <: AsyncAwsClient] {
  /** Creates a streaming client with an appropriate shutdown hook.
    *
    * @param settings the settings to use
    * @param credentialsProvider the provider of AWS credentials
    * @param executorService the executor service that the AWS clients should use for their asynchronous work
    * @return a tuple of the new client and a shutdown hook to shut the client down
    */
  private[awsutil] def streamingClient(settings: AwsSettings,
                                       credentialsProvider: AWSCredentialsProvider,
                                       executorService: ExecutorService): (T, ShutdownHook)

  /** Creates an asynchronous client from a streaming client. */
  private[awsutil] def asyncClient(streamingClient: T): U
}
