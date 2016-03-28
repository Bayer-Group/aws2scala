package com.monsanto.arch.awsutil.test_support

import com.typesafe.scalalogging.Logger
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationInt

trait AdaptableScalaFutures extends ScalaFutures {
  private lazy val lazyConfig =
    if (Option(System.getenv("TRAVIS")).contains("true")) {
      AdaptableScalaFutures.logger.info("Using Travis patience configuration")
      PatienceConfig(10.seconds, 50.milliseconds)
    } else {
      AdaptableScalaFutures.logger.info("Using default patience configuration")
      PatienceConfig(250.milliseconds, 10.milliseconds)
    }

  override implicit def patienceConfig = lazyConfig
}

object AdaptableScalaFutures extends AdaptableScalaFutures {
  private val logger: Logger = Logger(LoggerFactory.getLogger("AdaptableScalaFutures"))
}
