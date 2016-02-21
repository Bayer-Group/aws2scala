package com.monsanto.arch.awsutil.test

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.monsanto.arch.awsutil.{DefaultStreamingAwsClient, Settings}
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait AwsIntegrationSpec extends BeforeAndAfterAll { this: Suite ⇒
  private val actorSystem = ActorSystem(this.getClass.getSimpleName)
  protected implicit val materialiser = ActorMaterializer()(actorSystem)
  protected val streamingAwsClient = new DefaultStreamingAwsClient(Settings.Default)
  protected val asyncAwsClient = streamingAwsClient.asyncClient

  /** A randomly-generated identifier for the test. */
  protected val testId = UUID.randomUUID().getMostSignificantBits.toHexString

  override protected def afterAll() =
    try {
      streamingAwsClient.shutdown()
      Await.result(actorSystem.terminate(), 3.seconds)
    } finally super.afterAll()
}
