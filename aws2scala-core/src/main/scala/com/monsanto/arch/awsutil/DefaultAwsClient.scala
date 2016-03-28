package com.monsanto.arch.awsutil

import java.util.concurrent.{ExecutorService, Executors, TimeUnit}

import akka.util.Timeout
import com.amazonaws.auth.{AWSCredentialsProvider, DefaultAWSCredentialsProviderChain}
import com.monsanto.arch.awsutil.impl.{ShutdownFreeExecutorServiceWrapper, ShutdownHook}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.ref.WeakReference

class DefaultAwsClient private (settings: AwsSettings,
                                credentialsProvider: AWSCredentialsProvider,
                                executorService: ExecutorService) extends AwsClient with LazyLogging {
  def this(settings: AwsSettings) = this(settings, new DefaultAWSCredentialsProviderChain, Executors.newFixedThreadPool(50))

  /** Records whether or not shutdown has been invoked. */
  @volatile private var _shutdownInvoked = false
  /** A list of callbacks that should called to ensure that all clients have shut down. */
  private val shutdownHooks = ListBuffer.empty[ShutdownHook]

  private val streamingClientCache = new StreamingClientCache
  private val asyncClientCache = new AsyncClientCache

  override def shutdownInvoked = _shutdownInvoked

  override def async[T <: AsyncAwsClient](provider: AwsClientProvider[_ <: StreamingAwsClient, T]): T =
    shutdownHooks.synchronized {
      if (shutdownInvoked) {
        throw new IllegalStateException("Cannot create a new client after a shutdown.")
      }
      asyncClientCache(provider)
    }

  override def streaming[T <: StreamingAwsClient](provider: AwsClientProvider[T, _]): T =
    shutdownHooks.synchronized {
      if (shutdownInvoked) {
        throw new IllegalStateException("Cannot create a new client after a shutdown.")
      }
      streamingClientCache(provider)
    }

  override def shutdown(timeout: Timeout): Unit = {
    if (!shutdownInvoked) {
      shutdownHooks.synchronized {
        if (!_shutdownInvoked) {
          _shutdownInvoked = true

          logger.info("Commencing shutdown of AwsClient")
          // allow submitted tasks to terminate gracefully by requesting
          executorService.shutdown()
          try {
            // wait for things to die gracefully
            if (executorService.awaitTermination(timeout.duration.toNanos, TimeUnit.NANOSECONDS)) {
              logger.info("Executor service terminated gracefully")
            } else {
              logger.info("Executor service did not terminate gracefully.")
            }
          } catch {
            case _: InterruptedException =>
              logger.warn("Shutdown of executor service was interrupted")
          }
          // now, we get ugly
          executorService.shutdownNow()
          // ensure that all instantiated AWS clients are properly shut down
          shutdownHooks.foreach(_.shutdown())
        }
      }
    }
  }

  override def withCredentialsProvider(credentialsProvider: AWSCredentialsProvider): AwsClient = {
    shutdownHooks.synchronized {
      if (shutdownInvoked) {
        throw new IllegalStateException("Cannot create a new child after a shutdown.")
      }
      val childExecutorService = new ShutdownFreeExecutorServiceWrapper(executorService)
      val child = new DefaultAwsClient(settings, credentialsProvider, childExecutorService)
      shutdownHooks += ShutdownHook.childClientHook(child)
      child
    }
  }

  class StreamingClientCache {
    private val cache = mutable.Map.empty[AwsClientProvider[_,_], WeakReference[StreamingAwsClient]]

    def apply[T <: StreamingAwsClient](provider: AwsClientProvider[T,_]): T = {
      cache.synchronized {
        cache.get(provider) match {
          case Some(WeakReference(value)) ⇒ value.asInstanceOf[T]
          case _ ⇒
            shutdownHooks.synchronized {
              val (newClient, shutdownHook) = provider.streamingClient(settings, credentialsProvider, executorService)
              shutdownHooks += shutdownHook
              cache.update(provider, WeakReference(newClient))
              newClient
            }
        }
      }
    }
  }

  class AsyncClientCache {
    private val cache = mutable.Map.empty[AwsClientProvider[_,_], WeakReference[AsyncAwsClient]]

    def apply[T <: StreamingAwsClient,U <: AsyncAwsClient](provider: AwsClientProvider[T,U]): U = {
      cache.synchronized {
        cache.get(provider) match {
          case Some(WeakReference(value)) ⇒ value.asInstanceOf[U]
          case _ ⇒
            val streamingClient = streamingClientCache(provider)
            val asyncClient = provider.asyncClient(streamingClient)
            cache.update(provider, WeakReference(asyncClient))
            asyncClient
        }
      }
    }
  }
}
