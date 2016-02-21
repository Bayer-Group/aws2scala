package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.test.AwsEnumerationBehaviours
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class InstanceStateSpec extends FreeSpec with AwsEnumerationBehaviours {
  "an InstanceState should" - {
    "be constructible from its AWS equivalent" in {
      forAll { args: EC2Gen.InstanceStateArgs ⇒
        InstanceState.fromAws(args.toAws) shouldBe args.toInstanceState
      }
    }
  }

  "the InstanceState.Name enumeration" - {
    behave like anAwsEnumeration(InstanceState.Name)
  }
}
