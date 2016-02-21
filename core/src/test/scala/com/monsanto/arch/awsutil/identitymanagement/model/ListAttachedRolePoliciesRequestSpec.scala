package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.model.{ListAttachedRolePoliciesRequest ⇒ AwsListAttachedRolePoliciesRequest}
import com.monsanto.arch.awsutil.AwsGen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ListAttachedRolePoliciesRequestSpec extends FreeSpec {
  "the list all roles request" - {
    "creates the correct AWS request" in {
      forAll { roleName: AwsGen.IAM.Name ⇒
        val request = ListAttachedRolePoliciesRequest(roleName.value)
        val aws = new AwsListAttachedRolePoliciesRequest().withRoleName(roleName.value)

        request.toAws shouldBe aws
      }
    }

    "creates new requests" in {
      forAll { roleName: AwsGen.IAM.Name ⇒
        val request = ListAttachedRolePoliciesRequest(roleName.value)
        val aws = new AwsListAttachedRolePoliciesRequest().withRoleName(roleName.value)

        request.toAws should not be theSameInstanceAs (request.toAws)
      }
    }
  }

  "a list roles with prefix" - {
    "creates the correct AWS request" in {
      forAll { (roleName: AwsGen.IAM.Name, path: AwsGen.IAM.Path) ⇒
        val request = ListAttachedRolePoliciesRequest(roleName.value, path.value)
        val aws = new AwsListAttachedRolePoliciesRequest()
          .withRoleName(roleName.value)
          .withPathPrefix(request.pathPrefix.get)
        request.toAws shouldBe aws
      }
    }

    "creates new requests" in {
      forAll { (roleName: AwsGen.IAM.Name, path: AwsGen.IAM.Path) ⇒
        val request = ListAttachedRolePoliciesRequest(roleName.value, path.value)
        request.toAws should not be theSameInstanceAs (request.toAws)
      }
    }
  }
}
