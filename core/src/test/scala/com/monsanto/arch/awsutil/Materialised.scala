package com.monsanto.arch.awsutil

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/** A mix-in that provides a materialiser. */
trait Materialised extends BeforeAndAfterAll { this: Suite ⇒
  private val actorSystem = ActorSystem(this.getClass.getSimpleName)
  protected implicit val materialiser = ActorMaterializer()(actorSystem)

  override protected def afterAll() =
    try {
      Await.result(actorSystem.terminate(), 3.seconds)
    } finally super.afterAll()
}
