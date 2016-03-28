package com.monsanto.arch.awsutil.test_support

import java.util.concurrent.ExecutorService

import com.amazonaws.auth.AWSCredentialsProvider
import com.monsanto.arch.awsutil.impl.ShutdownHook
import com.monsanto.arch.awsutil.{AsyncAwsClient, AwsClientProvider, AwsSettings, StreamingAwsClient}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._

trait AwsClientProviderBehaviours { this: FreeSpec with MockFactory ⇒
  def anAwsClientProvider[StreamingClient <: StreamingAwsClient: Manifest, AsyncClient <: AsyncAwsClient: Manifest](provider: AwsClientProvider[StreamingClient,AsyncClient]): Unit = {
    val executorService = mock[ExecutorService]("executorService")
    var streamingClient: Option[StreamingClient] = None
    var shutdownHook: Option[ShutdownHook] = None

    "create" - {
      "a streaming client" in {
        val settings = AwsSettings.Default
        val credentialsProvider = mock[AWSCredentialsProvider]("credentialsProvider")

        val result = provider.streamingClient(settings, credentialsProvider, executorService)

        result._1 shouldBe a [StreamingClient]
        result._2 shouldBe a [ShutdownHook]

        streamingClient = Some(result._1)
        shutdownHook = Some(result._2)
      }

      "an asynchronous client" in {
        val asyncClient = provider.asyncClient(streamingClient.get)

        asyncClient shouldBe an [AsyncClient]
      }
    }

    "shut down" in {
      (executorService.shutdownNow _).expects()

      shutdownHook.get.shutdown()
    }
  }
}
