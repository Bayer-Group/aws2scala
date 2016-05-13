package com.monsanto.arch.awsutil.testkit

import org.scalacheck.Gen

/** Generators for Amazon SQS. */
object SqsGen {
  /** Generates a new queue name. */
  def queueName: Gen[String] = {
    val queueNameChar = Gen.oneOf(('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') :+ '_' :+ '-')
    UtilGen.stringOf(queueNameChar, 1, 80)
  }
}
