package com.monsanto.arch.awsutil

import java.util.concurrent.ExecutorService

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.{AmazonWebServiceClient, ClientConfiguration}
import com.monsanto.arch.awsutil.DefaultAwsClientSpec.{Client, TestClientProvider}
import com.monsanto.arch.awsutil.impl.ShutdownHook
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers._
import org.scalatest.{Outcome, fixture}

class DefaultAwsClientSpec extends fixture.FreeSpec with MockFactory {
  def clientCreationBehaviours[T <: Client: Manifest](createClient: (AwsClient) ⇒ T): Unit = {
    "creating client instances" in { f ⇒
      val result = createClient(f.awsClient)
      result shouldBe a[T]
    }

    "caching created client instances" in { f ⇒
      val result1 = createClient(f.awsClient)
      val result2 = createClient(f.awsClient)

      result1 shouldBe theSameInstanceAs (result2)
    }

    "shutting down the client" in { f ⇒
      val client = createClient(f.awsClient)

      client.isShutdown shouldBe false

      f.awsClient.shutdown()

      client.isShutdown shouldBe true
    }

    "not being able to create client instances after a shutdown" in { f ⇒
      f.awsClient.shutdown()
      an[IllegalStateException] shouldBe thrownBy {
        createClient(f.awsClient)
      }
    }

    "having its children create clients with the right credentials provider" in { f ⇒
      val credentialsProvider = mock[AWSCredentialsProvider]("credentialsProvider")
      val child = f.awsClient.withCredentialsProvider(credentialsProvider)

      val client = createClient(child)

      client.credentialsProvider shouldBe theSameInstanceAs (credentialsProvider)
    }
  }

  "the DefaultAwsClient should" - {
    "allow shutdown to be called multiple times" in { f ⇒
      f.awsClient.shutdownInvoked shouldBe false
      f.awsClient.shutdown()
      f.awsClient.shutdownInvoked shouldBe true
      f.awsClient.shutdown()
    }

    "work with asynchronous clients by" - {
      behave like clientCreationBehaviours((_: AwsClient).async(TestClientProvider))
    }

    "work with streaming clients by" - {
      behave like clientCreationBehaviours((_: AwsClient).streaming(TestClientProvider))
    }

    "allow creation of child clients" in { f ⇒
      val credentialsProvider = mock[AWSCredentialsProvider]("credentialsProvider")
      val child = f.awsClient.withCredentialsProvider(credentialsProvider)

      child.shutdownInvoked shouldBe false
    }

    "not be affected by a child shutting down" in { f ⇒
      val credentialsProvider = mock[AWSCredentialsProvider]("credentialsProvider")
      val child = f.awsClient.withCredentialsProvider(credentialsProvider)

      child.shutdownInvoked shouldBe false
      f.awsClient.shutdownInvoked shouldBe false

      child.shutdown()

      child.shutdownInvoked shouldBe true
      f.awsClient.shutdownInvoked shouldBe false
    }

    "shut down its children" in { f ⇒
      val credentialsProvider = mock[AWSCredentialsProvider]("credentialsProvider")
      val child = f.awsClient.withCredentialsProvider(credentialsProvider)

      child.shutdownInvoked shouldBe false
      f.awsClient.shutdownInvoked shouldBe false

      f.awsClient.shutdown()

      child.shutdownInvoked shouldBe true
      f.awsClient.shutdownInvoked shouldBe true
    }

    "not allow the creation of new children once a shut down has been invoked" in { f ⇒
      f.awsClient.shutdown()
      an[IllegalStateException] shouldBe thrownBy {
        val credentialsProvider = mock[AWSCredentialsProvider]("credentialsProvider")
        f.awsClient.withCredentialsProvider(credentialsProvider)
      }
    }
  }

  case class FixtureParam(awsClient: AwsClient)

  override protected def withFixture(test: OneArgTest): Outcome = {
    val awsClient = new DefaultAwsClient(AwsSettings.Default)
    try {
      withFixture(test.toNoArgTest(FixtureParam(awsClient)))
    } finally awsClient.shutdown()
  }
}

object DefaultAwsClientSpec {
  trait Client {
    def isShutdown: Boolean
    def credentialsProvider: AWSCredentialsProvider
  }
  class AwsTestClient extends AmazonWebServiceClient(new ClientConfiguration()) {
    var isShutdown: Boolean = false

    override def shutdown(): Unit = {
      isShutdown = true
      super.shutdown()
    }
  }

  case class StreamingTestClient(credentialsProvider: AWSCredentialsProvider, aws: AwsTestClient) extends Client with StreamingAwsClient {
    override def isShutdown = aws.isShutdown
  }
  case class AsyncTestClient(streamingTestClient: StreamingTestClient) extends Client with AsyncAwsClient {
    override def isShutdown = streamingTestClient.isShutdown
    override def credentialsProvider = streamingTestClient.credentialsProvider
  }

  object TestClientProvider extends AwsClientProvider[StreamingTestClient,AsyncTestClient] {
    override private[awsutil] def streamingClient(settings: AwsSettings,
                                                  credentialsProvider: AWSCredentialsProvider,
                                                  executorService: ExecutorService) = {
      settings shouldBe theSameInstanceAs (AwsSettings.Default)
      val aws = new AwsTestClient
      val hook = ShutdownHook.clientHook("test", aws)
      (StreamingTestClient(credentialsProvider, aws), hook)
    }

    override private[awsutil] def asyncClient(streamingTestClient: StreamingTestClient) =
      AsyncTestClient(streamingTestClient)
  }
}
