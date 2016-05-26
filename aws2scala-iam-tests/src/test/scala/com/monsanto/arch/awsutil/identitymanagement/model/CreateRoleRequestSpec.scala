package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.converters.IamConverters._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class CreateRoleRequestSpec extends FreeSpec {
  "a CreateRoleRequest can be round-tripped" - {
    "from its AWS equivalent" in {
      forAll { request: CreateRoleRequest ⇒
        val awsRequest = new aws.CreateRoleRequest()
          .withRoleName(request.name)
          .withAssumeRolePolicyDocument(request.assumeRolePolicy.toJson)
        request.path.foreach(p ⇒ awsRequest.setPath(p.pathString))

        awsRequest.asScala.asAws shouldBe awsRequest
      }
    }

    "via its AWS equivalent" in {
      forAll { request: CreateRoleRequest ⇒
        request.asAws.asScala shouldBe request
      }
    }
  }
}
