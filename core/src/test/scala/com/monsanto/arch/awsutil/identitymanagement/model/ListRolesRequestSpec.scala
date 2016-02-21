package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.model.{ListRolesRequest ⇒ AwsListRolesRequest}
import com.monsanto.arch.awsutil.AwsGen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ListRolesRequestSpec extends FreeSpec {
  "the list all roles request" - {
    "creates the correct AWS request" in {
      ListRolesRequest.allRoles.toAws shouldBe new AwsListRolesRequest
    }

    "creates new requests" in {
      ListRolesRequest.allRoles.toAws should not be theSameInstanceAs (ListRolesRequest.allRoles.toAws)
    }
  }

  "a list roles with prefix" - {
    "creates the correct AWS request" in {
      forAll { path: AwsGen.IAM.Path ⇒
        val request = ListRolesRequest.withPathPrefix(path.value)
        val aws = new AwsListRolesRequest().withPathPrefix(request.pathPrefix.get)
        request.toAws shouldBe aws
      }
    }

    "creates new requests" in {
      forAll { path: AwsGen.IAM.Path ⇒
        val request = ListRolesRequest.withPathPrefix(path.value)
        request.toAws should not be theSameInstanceAs (request.toAws)
      }
    }
  }
}
