package com.monsanto.arch.awsutil.impl

import com.amazonaws.AmazonWebServiceClient
import com.monsanto.arch.awsutil.AwsClient
import com.typesafe.scalalogging.LazyLogging

import scala.ref.WeakReference

private[awsutil] trait ShutdownHook {
  def shutdown(): Unit
}

private[awsutil] object ShutdownHook extends LazyLogging {
  def clientHook(name: String, service: AmazonWebServiceClient): ShutdownHook =
    new ClientShutdownHook(name, WeakReference(service))

  def childClientHook(client: AwsClient): ShutdownHook = new ChildShutdownHook(WeakReference(client))

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

  private class ChildShutdownHook(child: WeakReference[AwsClient]) extends ShutdownHook {
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
