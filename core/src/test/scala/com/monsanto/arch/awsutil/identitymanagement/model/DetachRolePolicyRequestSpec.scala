package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.model.{DetachRolePolicyRequest ⇒ AwsDetachRolePolicyRequest}
import com.monsanto.arch.awsutil.AwsGen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DetachRolePolicyRequestSpec extends FreeSpec {
  "a DetachRolePolicyRequest can be round-tripped" - {
    "from its AWS equivalent" in {
      forAll { (policyArn: AwsGen.IAM.PolicyArn, roleName: AwsGen.IAM.Name) ⇒
        val aws = new AwsDetachRolePolicyRequest()
          .withPolicyArn(policyArn.value)
          .withRoleName(roleName.value)

        DetachRolePolicyRequest.fromAws(aws).toAws shouldBe aws
      }
    }

    "via its AWS equivalent" in {
      forAll { (policyArn: AwsGen.IAM.PolicyArn, roleName: AwsGen.IAM.Name) ⇒
        val request = DetachRolePolicyRequest(policyArn.value, roleName.value)

        DetachRolePolicyRequest.fromAws(request.toAws) shouldBe request
      }
    }
  }
}
