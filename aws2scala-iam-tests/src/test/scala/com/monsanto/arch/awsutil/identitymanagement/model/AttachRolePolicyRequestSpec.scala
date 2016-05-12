package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.model.{AttachRolePolicyRequest ⇒ AwsAttachRolePolicyRequest}
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class AttachRolePolicyRequestSpec extends FreeSpec {
  "a AttachRolePolicyRequest can be round-tripped" - {
    "from its AWS equivalent" in {
      forAll { (policyArn: PolicyArn, roleName: Name) ⇒
        val aws = new AwsAttachRolePolicyRequest()
          .withPolicyArn(policyArn.arnString)
          .withRoleName(roleName.value)

        AttachRolePolicyRequest.fromAws(aws).toAws shouldBe aws
      }
    }

    "via its AWS equivalent" in {
      forAll { request: AttachRolePolicyRequest ⇒
        AttachRolePolicyRequest.fromAws(request.toAws) shouldBe request
      }
    }
  }
}
