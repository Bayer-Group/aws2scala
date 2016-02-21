package com.monsanto.arch.awsutil.ec2.model

import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class PlacementSpec extends FreeSpec {
  "an Placement should" - {
    "be constructible from its AWS equivalent" in {
      forAll { args: EC2Gen.PlacementArgs ⇒
        Placement.fromAws(args.toAws) shouldBe args.toPlacement
      }
    }
  }
}
