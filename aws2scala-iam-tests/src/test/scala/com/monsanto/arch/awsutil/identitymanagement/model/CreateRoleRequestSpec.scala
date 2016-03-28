package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class CreateRoleRequestSpec extends FreeSpec {
  "a CreateRoleRequest can be round-tripped" - {
    "from its AWS equivalent" in {
      forAll(
        arbitrary[Name] → "name",
        arbitrary[Policy] → "assumeRolePolicy",
        arbitrary[Option[Path]] → "path"
      ) { (name, assumeRolePolicy, path) ⇒
        val request = new aws.CreateRoleRequest()
          .withRoleName(name.value)
          .withAssumeRolePolicyDocument(assumeRolePolicy.toString)
        path.foreach(p ⇒ request.setPath(p.value))

        CreateRoleRequest.fromAws(request).toAws shouldBe request
      }
    }

    "via its AWS equivalent" in {
      forAll { request: CreateRoleRequest ⇒
        CreateRoleRequest.fromAws(request.toAws) shouldBe request
      }
    }
  }
}
