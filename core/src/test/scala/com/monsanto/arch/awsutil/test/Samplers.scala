package com.monsanto.arch.awsutil.test

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

trait Samplers {
  implicit class EnhancedGen[T](gen: Gen[T]) {
    def reallySample(numTries: Int): T = {
      var maybeSample: Option[T] = None
      var i = 0
      while (maybeSample.isEmpty && i < numTries) {
        maybeSample = gen.sample
        i += 1
      }
      maybeSample.getOrElse(throw new RuntimeException("Ran out tries to get a sample."))
    }

    def reallySample: T = reallySample(100)
  }

  def arbitrarySample[T: Arbitrary] = arbitrary[T].reallySample
}

object Samplers extends Samplers
