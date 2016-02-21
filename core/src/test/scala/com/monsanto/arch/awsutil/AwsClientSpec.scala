package com.monsanto.arch.awsutil

import akka.Done
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.monsanto.arch.awsutil.AwsClientSpec.PromiseShutDownHook
import com.monsanto.arch.awsutil.impl.ShutdownHook
import com.typesafe.config.ConfigFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._

import scala.concurrent.Promise

class AwsClientSpec extends FreeSpec {
  "the AWS client should shut down cleanly via" - {
    "the streaming interface" in {
      val streamingClient = StreamingAwsClient(ConfigFactory.load()).asInstanceOf[DefaultStreamingAwsClient]

      // start up the clients
      streamingClient.cloudFormation
      streamingClient.ec2
      streamingClient.identityManagement
      streamingClient.kms
      streamingClient.rds
      streamingClient.s3
      streamingClient.securityTokenService
      streamingClient.sns

      streamingClient.shutdownHooks should have size 8

      streamingClient.shutdown()

      streamingClient.executorService.isShutdown shouldBe true
    }

    "the async interface" in {
      val asyncClient = AsyncAwsClient(ConfigFactory.load())
      val streamingClient = asyncClient.streamingClient.asInstanceOf[DefaultStreamingAwsClient]

      asyncClient.cloudFormation
      asyncClient.ec2
      asyncClient.identityManagement
      asyncClient.kms
      asyncClient.rds
      asyncClient.s3
      asyncClient.securityTokenService
      asyncClient.sns

      streamingClient.shutdownHooks should have size 8

      // now shut down
      asyncClient.shutdown()

      // the executor service should be shut down
      streamingClient.executorService.isShutdown shouldBe true
    }
  }

  "when creating a child streaming client" - {
    "it will use the new credentials" in {
      val parent = new DefaultStreamingAwsClient(Settings.Default)
      val newProvider = new ProfileCredentialsProvider()
      val child = parent.withCredentialsProvider(newProvider)
      child.credentialsProvider shouldBe theSameInstanceAs (newProvider)

      child.shutdown()
      parent.shutdown()
    }

    "the child will not shut down the parent" in {
      val parent = new DefaultStreamingAwsClient(Settings.Default)
      val parentHook = new PromiseShutDownHook
      parent.shutdownHooks += parentHook

      val newProvider = new ProfileCredentialsProvider()
      val child = parent.withCredentialsProvider(newProvider)
      val childHook = new PromiseShutDownHook
      child.shutdownHooks += childHook

      parentHook.invoked shouldBe false
      childHook.invoked shouldBe false

      child.shutdown()

      parentHook.invoked shouldBe false
      childHook.invoked shouldBe true

      parent.executorService.isShutdown shouldBe false

      parent.shutdown()

      parentHook.invoked shouldBe true
      parent.executorService.isShutdown shouldBe true
    }

    "the parent will shut down the child" in {
      val parent = new DefaultStreamingAwsClient(Settings.Default)
      val parentHook = new PromiseShutDownHook
      parent.shutdownHooks += parentHook

      val newProvider = new ProfileCredentialsProvider()
      val child = parent.withCredentialsProvider(newProvider)
      val childHook = new PromiseShutDownHook
      child.shutdownHooks += childHook

      parentHook.invoked shouldBe false
      childHook.invoked shouldBe false

      parent.shutdown()

      parentHook.invoked shouldBe true
      childHook.invoked shouldBe true
      parent.executorService.isShutdown shouldBe true
    }
  }

  "when creating a child asynchronous client" - {
    "it will use a streaming client with the new credentials" in {
      val streamer = new DefaultStreamingAwsClient(Settings.Default)
      val parent = new DefaultAsyncAwsClient(streamer)
      val newProvider = new ProfileCredentialsProvider()
      val child = parent.withCredentialsProvider(newProvider)

      child.streamingClient.credentialsProvider shouldBe theSameInstanceAs (newProvider)

      streamer.shutdown()
    }
  }
}

object AwsClientSpec {
  class PromiseShutDownHook extends ShutdownHook {
    val promise = Promise[Done]

    override def shutdown() = {
      if (!promise.isCompleted) {
        promise.success(Done.getInstance())
      }
    }

    def invoked: Boolean = promise.isCompleted
  }
}
