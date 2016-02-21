package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.model.{CreateRoleRequest ⇒ AwsCreateRoleRequest}
import com.monsanto.arch.awsutil.AwsGen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class CreateRoleRequestSpec extends FreeSpec {
  "a CreateRoleRequest can be round-tripped" - {
    "from its AWS equivalent" in {
      forAll { args: AwsGen.IAM.CreateRoleRequestArgs ⇒
        val aws = new AwsCreateRoleRequest()
          .withRoleName(args.name.value)
          .withAssumeRolePolicyDocument(args.assumeRolePolicy.toJson)
        args.path.foreach(p ⇒ aws.setPath(p.value))

        CreateRoleRequest.fromAws(aws).toAws shouldBe aws
      }
    }

    "via its AWS equivalent" in {
      forAll { args: AwsGen.IAM.CreateRoleRequestArgs ⇒
        val request = args.toRequest

        CreateRoleRequest.fromAws(request.toAws) shouldBe request
      }
    }
  }
}
