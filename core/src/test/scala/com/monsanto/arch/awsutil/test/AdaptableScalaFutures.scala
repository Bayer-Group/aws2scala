package com.monsanto.arch.awsutil.test

import com.typesafe.scalalogging.Logger
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt

trait AdaptableScalaFutures extends ScalaFutures {
  override implicit lazy val patienceConfig =
    if (Option(System.getenv("TRAVIS")).contains("true")) {
      AdaptableScalaFutures.logger.info("Using Travis patience configuration")
      PatienceConfig(5.seconds, 50.milliseconds)
    } else {
      AdaptableScalaFutures.logger.info("Using default patience configuration")
      PatienceConfig(200.milliseconds, 10.milliseconds)
    }
}

object AdaptableScalaFutures extends ScalaFutures {
  private val logger: Logger = Logger(LoggerFactory.getLogger("AdaptableScalaFutures"))
}
