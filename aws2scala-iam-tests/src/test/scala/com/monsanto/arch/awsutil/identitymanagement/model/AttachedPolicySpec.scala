package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.model.{AttachedPolicy ⇒ AwsAttachedPolicy}
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class AttachedPolicySpec extends FreeSpec {
  "a AttachedPolicy can be round-tripped" - {
    "from its AWS equivalent" in {
      forAll { (arn: PolicyArn, name: Name) ⇒
        val aws = new AwsAttachedPolicy()
          .withPolicyArn(arn.arnString)
          .withPolicyName(name.value)

        AttachedPolicy.fromAws(aws).toAws shouldBe aws
      }
    }

    "via its AWS equivalent" in {
      forAll { (arn: PolicyArn, name: Name) ⇒
        val attachedPolicy = AttachedPolicy(arn.arnString, name.value)

        AttachedPolicy.fromAws(attachedPolicy.toAws) shouldBe attachedPolicy
      }
    }
  }
}
