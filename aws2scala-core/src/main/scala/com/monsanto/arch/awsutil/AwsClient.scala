package com.monsanto.arch.awsutil

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import com.amazonaws.auth.AWSCredentialsProvider
import com.typesafe.config.Config

/** Serves as the main entry point to aws2scala. */
trait AwsClient {
  /** Creates an asynchronous client using the given provider. */
  def async[T <: AsyncAwsClient](provider: AwsClientProvider[_ <: StreamingAwsClient, T]): T

  /** Creates a streaming client using the given provider. */
  def streaming[T <: StreamingAwsClient](provider: AwsClientProvider[T, _]): T

  /** Returns whether or not [[shutdown]] has been invoked on the client. */
  def shutdownInvoked: Boolean

  /** Requests graceful termination of all of the asynchronous clients within the given timeout.  Once the timeout
    * expires, termination is allowed to proceed non-gracefully.
    */
  def shutdown(timeout: Timeout = Timeout(1, TimeUnit.MINUTES)): Unit

  /** Given a set of credentials, return a new client that shares the same thread pools as this one, but uses the new
    * credentials.  Note that calls to `shutdown()` on the new client will not affect this client.
    *
    * @param credentialsProvider the credentials to use with the new client
    * @return a new client that will use the given credentials and share resources with this client
    */
  def withCredentialsProvider(credentialsProvider: AWSCredentialsProvider): AwsClient
}

object AwsClient {
  /** Returns a streaming AWS client using the given settings. */
  def apply(settings: AwsSettings): AwsClient = new DefaultAwsClient(settings)
  /** Returns a streaming AWS client using the given configuration. */
  def apply(config: Config): AwsClient = apply(new AwsSettings(config))

  /** A client built using the default settings. */
  lazy val Default: AwsClient = {
    val client = apply(AwsSettings.Default)

    val shutdownHook = new Thread(new Runnable {
      override def run(): Unit = client.shutdown()
    })

    Runtime.getRuntime.addShutdownHook(shutdownHook)

    client
  }
}
