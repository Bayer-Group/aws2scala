package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.converters.IamConverters._
import com.monsanto.arch.awsutil.testkit.CoreGen
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DetachRolePolicyRequestSpec extends FreeSpec {
  "a DetachRolePolicyRequest can be round-tripped" - {
    "from its AWS equivalent" in {
      forAll(
        arbitrary[PolicyArn] → "policyArn",
        CoreGen.iamName → "roleName"
      ) { (policyArn, roleName) ⇒
        val request = new aws.DetachRolePolicyRequest()
          .withPolicyArn(policyArn.arnString)
          .withRoleName(roleName)

        request.asScala.asAws shouldBe request
      }
    }

    "via its AWS equivalent" in {
      forAll { request: DetachRolePolicyRequest ⇒
        request.asAws.asScala shouldBe request
      }
    }
  }
}
