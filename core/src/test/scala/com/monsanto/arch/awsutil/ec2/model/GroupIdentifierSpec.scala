package com.monsanto.arch.awsutil.ec2.model

import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class GroupIdentifierSpec extends FreeSpec {
  "a GroupIdentifier should" - {
    "be constructible from its AWS equivalent" in {
      forAll { args: EC2Gen.GroupIdentifierArgs ⇒
        GroupIdentifier.fromAws(args.toAws) shouldBe args.toGroupIdentifier
      }
    }
  }
}
