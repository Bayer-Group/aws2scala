package com.monsanto.arch.awsutil.impl

import com.amazonaws.AmazonWebServiceClient
import com.amazonaws.services.s3.transfer.TransferManager
import com.monsanto.arch.awsutil.StreamingAwsClient
import com.typesafe.scalalogging.LazyLogging

import scala.ref.WeakReference

private[awsutil] trait ShutdownHook {
  def shutdown(): Unit
}

private[awsutil] object ShutdownHook extends LazyLogging {
  def clientHook(name: String, service: AmazonWebServiceClient): ShutdownHook =
    new ClientShutdownHook(name, WeakReference(service))

  def s3Hook(transferManager: TransferManager): ShutdownHook = new S3ShutdownHook(WeakReference(transferManager))

  def childClientHook(client: StreamingAwsClient): ShutdownHook = new ChildShutdownHook(WeakReference(client))

  private class ClientShutdownHook(name: String, serviceRef: WeakReference[AmazonWebServiceClient]) extends ShutdownHook {
    override def shutdown() = {
      serviceRef.get match {
        case Some(service) ⇒
          logger.debug(s"Shutting down client: $name")
          service.shutdown()
        case None ⇒
          logger.debug(s"Client $name appears to have garbage collected.")
      }
    }
  }

  private class S3ShutdownHook(transferManagerRef: WeakReference[TransferManager]) extends ShutdownHook {
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

  private class ChildShutdownHook(child: WeakReference[StreamingAwsClient]) extends ShutdownHook {
    override def shutdown() = {
      child.get match {
        case Some(client) ⇒
          logger.debug(s"Shutting down child streaming client")
          client.shutdown()
        case None ⇒
          logger.debug("Child streaming client appears to have been garbage collected")
      }
    }
  }
}
