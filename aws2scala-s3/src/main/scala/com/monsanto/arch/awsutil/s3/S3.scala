package com.monsanto.arch.awsutil.s3

import java.util.concurrent.ExecutorService

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.transfer.TransferManager
import com.monsanto.arch.awsutil.impl.ShutdownHook
import com.monsanto.arch.awsutil.{AwsClientProvider, AwsSettings}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext
import scala.ref.WeakReference

object S3 extends AwsClientProvider[StreamingS3Client, AsyncS3Client] {
  override private[awsutil] def streamingClient(settings: AwsSettings, credentialsProvider: AWSCredentialsProvider,
                                                executorService: ExecutorService) = {
    val s3 = new AmazonS3Client(credentialsProvider)
    s3.setRegion(settings.region)
    val transferManager = new TransferManager(s3, executorService)
    val executionContext = ExecutionContext.fromExecutorService(executorService)
    val client = new DefaultStreamingS3Client(s3, transferManager, settings)(executionContext)
    val shutdownHook: ShutdownHook = new S3ShutdownHook(WeakReference(transferManager))
    (client, shutdownHook)
  }

  override private[awsutil] def asyncClient(streamingClient: StreamingS3Client) = new DefaultAsyncS3Client(streamingClient)

  private class S3ShutdownHook(transferManagerRef: WeakReference[TransferManager]) extends ShutdownHook with StrictLogging {
    override def shutdown() = {
      transferManagerRef.get match {
        case Some(transferManager) ⇒
          logger.debug(s"Shutting down S3 transfer manager and client")
          transferManager.shutdownNow(true)
        case None ⇒
          logger.debug("S3 transfer manager appears to have been garbage collected.")
      }
    }
  }
}