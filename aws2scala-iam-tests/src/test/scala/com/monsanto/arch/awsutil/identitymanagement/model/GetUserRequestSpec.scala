package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class GetUserRequestSpec extends FreeSpec {
  "the get current user request" - {
    "creates the correct AWS request" in {
      GetUserRequest.currentUser.toAws shouldBe new aws.GetUserRequest
    }

    "creates new requests" in {
      GetUserRequest.currentUser.toAws should not be theSameInstanceAs (GetUserRequest.currentUser.toAws)
    }
  }

  "a list roles with prefix" - {
    "creates the correct AWS request" in {
      forAll { name: Name ⇒
        val request = GetUserRequest.forUserName(name.value)
        val awsRequest = new aws.GetUserRequest().withUserName(request.userName.get)
        request.toAws shouldBe awsRequest
      }
    }

    "creates new requests" in {
      forAll { name: Name ⇒
        val request = GetUserRequest.forUserName(name.value)
        request.toAws should not be theSameInstanceAs (request.toAws)
      }
    }
  }
}
