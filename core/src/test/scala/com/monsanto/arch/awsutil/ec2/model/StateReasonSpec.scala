package com.monsanto.arch.awsutil.ec2.model

import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class StateReasonSpec extends FreeSpec {
  "an StateReason should" - {
    "be constructible from its AWS equivalent" in {
      forAll { args: EC2Gen.StateReasonArgs ⇒
        StateReason.fromAws(args.toAws) shouldBe args.toStateReason
      }
    }
  }
}
