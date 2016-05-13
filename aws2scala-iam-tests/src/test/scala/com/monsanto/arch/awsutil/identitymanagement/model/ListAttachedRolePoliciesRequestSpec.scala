package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.testkit.CoreGen
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ListAttachedRolePoliciesRequestSpec extends FreeSpec {
  "the list all roles request" - {
    "creates the correct AWS request" in {
      forAll(CoreGen.iamName) { roleName ⇒
        val request = ListAttachedRolePoliciesRequest(roleName)
        val awsRequest = new aws.ListAttachedRolePoliciesRequest().withRoleName(roleName)

        request.toAws shouldBe awsRequest
      }
    }

    "creates new requests" in {
      forAll(CoreGen.iamName) { roleName ⇒
        val request = ListAttachedRolePoliciesRequest(roleName)

        request.toAws should not be theSameInstanceAs (request.toAws)
      }
    }
  }

  "a list roles with prefix" - {
    "creates the correct AWS request" in {
      forAll(CoreGen.iamName, arbitrary[Path]) { (roleName, path) ⇒
        val request = ListAttachedRolePoliciesRequest(roleName, path.pathString)
        val awsRequest = new aws.ListAttachedRolePoliciesRequest()
          .withRoleName(roleName)
          .withPathPrefix(request.pathPrefix.get)
        request.toAws shouldBe awsRequest
      }
    }

    "creates new requests" in {
      forAll(CoreGen.iamName, arbitrary[Path]) { (roleName, path) ⇒
        val request = ListAttachedRolePoliciesRequest(roleName, path.pathString)
        request.toAws should not be theSameInstanceAs (request.toAws)
      }
    }
  }
}
