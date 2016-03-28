package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.ec2.model.AwsConverters._
import com.monsanto.arch.awsutil.testkit.Ec2ScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class PlacementSpec extends FreeSpec {
  "an Placement should" - {
    "be constructible from its AWS equivalent" in {
      forAll { placement: Placement ⇒
        Placement.fromAws(placement.toAws) shouldBe placement
      }
    }
  }
}
