package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ListAttachedRolePoliciesRequestSpec extends FreeSpec {
  "the list all roles request" - {
    "creates the correct AWS request" in {
      forAll { roleName: Name ⇒
        val request = ListAttachedRolePoliciesRequest(roleName.value)
        val awsRequest = new aws.ListAttachedRolePoliciesRequest().withRoleName(roleName.value)

        request.toAws shouldBe awsRequest
      }
    }

    "creates new requests" in {
      forAll { roleName: Name ⇒
        val request = ListAttachedRolePoliciesRequest(roleName.value)

        request.toAws should not be theSameInstanceAs (request.toAws)
      }
    }
  }

  "a list roles with prefix" - {
    "creates the correct AWS request" in {
      forAll { (roleName: Name, path: Path) ⇒
        val request = ListAttachedRolePoliciesRequest(roleName.value, path.pathString)
        val awsRequest = new aws.ListAttachedRolePoliciesRequest()
          .withRoleName(roleName.value)
          .withPathPrefix(request.pathPrefix.get)
        request.toAws shouldBe awsRequest
      }
    }

    "creates new requests" in {
      forAll { (roleName: Name, path: Path) ⇒
        val request = ListAttachedRolePoliciesRequest(roleName.value, path.pathString)
        request.toAws should not be theSameInstanceAs (request.toAws)
      }
    }
  }
}
