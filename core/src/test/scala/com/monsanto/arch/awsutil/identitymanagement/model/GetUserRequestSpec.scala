package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.model.{GetUserRequest ⇒ AwsGetUserRequest}
import com.monsanto.arch.awsutil.AwsGen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class GetUserRequestSpec extends FreeSpec {
  "the get current user request" - {
    "creates the correct AWS request" in {
      GetUserRequest.currentUser.toAws shouldBe new AwsGetUserRequest
    }

    "creates new requests" in {
      GetUserRequest.currentUser.toAws should not be theSameInstanceAs (GetUserRequest.currentUser.toAws)
    }
  }

  "a list roles with prefix" - {
    "creates the correct AWS request" in {
      forAll { name: AwsGen.IAM.Name ⇒
        val request = GetUserRequest.forUserName(name.value)
        val aws = new AwsGetUserRequest().withUserName(request.userName.get)
        request.toAws shouldBe aws
      }
    }

    "creates new requests" in {
      forAll { name: AwsGen.IAM.Name ⇒
        val request = GetUserRequest.forUserName(name.value)
        request.toAws should not be theSameInstanceAs (request.toAws)
      }
    }
  }
}
