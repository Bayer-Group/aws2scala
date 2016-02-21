package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.model.{Role ⇒ AwsRole}
import com.monsanto.arch.awsutil.AwsGen.IAM.RoleArgs
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class RoleSpec extends FreeSpec {
  "a Role can be round-tripped" - {
    "from its AWS equivalent" in {
      forAll { args: RoleArgs ⇒
        val aws = new AwsRole()
          .withArn(args.arn.value)
          .withRoleName(args.name.value)
          .withRoleId(args.id.value)
          .withPath(args.path.value)
          .withAssumeRolePolicyDocument(args.assumeRolePolicyDocument.toJson)
          .withCreateDate(args.created)

        Role.fromAws(aws).toAws shouldBe aws
      }
    }

    "via its AWS equivalent" in {
      forAll { args: RoleArgs ⇒
        val role = args.toRole

        Role.fromAws(role.toAws) shouldBe role
      }
    }
  }
}
