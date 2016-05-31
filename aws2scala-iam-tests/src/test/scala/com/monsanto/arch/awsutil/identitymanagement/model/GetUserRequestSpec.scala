package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.converters.IamConverters._
import com.monsanto.arch.awsutil.testkit.CoreGen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class GetUserRequestSpec extends FreeSpec {
  "the get current user request" - {
    "creates the correct AWS request" in {
      GetUserRequest.currentUser.asAws shouldBe new aws.GetUserRequest
    }

    "creates new requests" in {
      GetUserRequest.currentUser.asAws should not be theSameInstanceAs (GetUserRequest.currentUser.asAws)
    }
  }

  "a list roles with prefix" - {
    "creates the correct AWS request" in {
      forAll(CoreGen.iamName) { name ⇒
        val request = GetUserRequest.forUserName(name)
        val awsRequest = new aws.GetUserRequest().withUserName(request.userName.get)
        request.asAws shouldBe awsRequest
      }
    }

    "creates new requests" in {
      forAll(CoreGen.iamName) { name ⇒
        val request = GetUserRequest.forUserName(name)
        request.asAws should not be theSameInstanceAs (request.asAws)
      }
    }
  }
}
