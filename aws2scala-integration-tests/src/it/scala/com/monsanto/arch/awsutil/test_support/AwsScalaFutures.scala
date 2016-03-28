package com.monsanto.arch.awsutil.test_support

import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration.DurationInt

trait AwsScalaFutures extends ScalaFutures {
  override implicit def patienceConfig = PatienceConfig(1.minute, 1.second)
}

object AwsScalaFutures extends AwsScalaFutures
