package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.converters.IamConverters._
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ListRolesRequestSpec extends FreeSpec {
  "the list all roles request" - {
    "creates the correct AWS request" in {
      ListRolesRequest.allRoles.asAws shouldBe new aws.ListRolesRequest
    }

    "creates new requests" in {
      ListRolesRequest.allRoles.asAws should not be theSameInstanceAs (ListRolesRequest.allRoles.asAws)
    }
  }

  "a list roles with prefix" - {
    "creates the correct AWS request" in {
      forAll { path: Path ⇒
        val request = ListRolesRequest.withPrefix(path)
        val awsRequest = new aws.ListRolesRequest().withPathPrefix(path.pathString)
        request.asAws shouldBe awsRequest
      }
    }

    "creates new requests" in {
      forAll { path: Path ⇒
        val request = ListRolesRequest.withPrefix(path)
        request.asAws should not be theSameInstanceAs (request.asAws)
      }
    }
  }
}
