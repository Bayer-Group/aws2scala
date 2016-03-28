package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.ec2.model.AwsConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import com.monsanto.arch.awsutil.testkit.Ec2ScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class InstanceStateSpec extends FreeSpec with AwsEnumerationBehaviours {
  "an InstanceState should" - {
    "be constructible from its AWS equivalent" in {
      forAll { state: InstanceState ⇒
        InstanceState.fromAws(state.toAws) shouldBe state
      }
    }
  }

  "the InstanceState.Name enumeration" - {
    behave like anAwsEnumeration(InstanceState.Name)
  }
}
