package com.monsanto.arch.awsutil.ec2.model

import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class IamInstanceProfileSpec extends FreeSpec {
  "a IamInstanceProfile should" - {
    "be constructible from its AWS equivalent" in {
      forAll { args: EC2Gen.IamInstanceProfileArgs ⇒
        IamInstanceProfile.fromAws(args.toAws) shouldBe args.toIamInstanceProfile
      }
    }
  }
}
