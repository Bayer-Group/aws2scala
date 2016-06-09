package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.converters.IamConverters._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ManagedPolicyVersionSpec extends FreeSpec {
  "a ManagedPolicyVersion should" - {
    "round-trip via its AWS equivalent" in {
      forAll { policyVersion: ManagedPolicyVersion ⇒
        policyVersion.asAws.asScala shouldBe policyVersion
      }
    }
  }
}
