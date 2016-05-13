package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.converters.IamConverters._
import com.monsanto.arch.awsutil.testkit.CoreGen
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class AttachRolePolicyRequestSpec extends FreeSpec {
  "a AttachRolePolicyRequest can be round-tripped" - {
    "from its AWS equivalent" in {
      forAll(arbitrary[PolicyArn], CoreGen.iamName) { (policyArn, roleName) ⇒
        val request = new aws.AttachRolePolicyRequest()
          .withPolicyArn(policyArn.arnString)
          .withRoleName(roleName)

        request.asScala.asAws shouldBe request
      }
    }

    "via its AWS equivalent" in {
      forAll { request: AttachRolePolicyRequest ⇒
        request.asAws.asScala shouldBe request
      }
    }
  }
}
