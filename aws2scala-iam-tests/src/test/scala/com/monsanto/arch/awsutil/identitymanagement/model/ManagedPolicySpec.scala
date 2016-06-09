package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.converters.IamConverters._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ManagedPolicySpec extends FreeSpec {
  "a ManagedPolicy should" - {
    "round-trip via its AWS equivalent" in {
      forAll { policy: ManagedPolicy ⇒
        policy.asAws.asScala shouldBe policy
      }
    }
  }
}
