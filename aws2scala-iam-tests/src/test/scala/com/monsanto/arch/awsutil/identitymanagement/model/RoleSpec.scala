package com.monsanto.arch.awsutil.identitymanagement.model

import java.util.Date

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class RoleSpec extends FreeSpec {
  "a Role can be round-tripped" - {
    "from its AWS equivalent" in {
      forAll (
        arbitrary[Account] → "account",
        arbitrary[Name] → "name",
        arbitrary[Path] → "path",
        arbitrary[RoleId] → "roleId",
        arbitrary[Policy] → "assumeRolePolicy",
        arbitrary[Date] → "created"
      ) { (account, name, path, roleId, assumeRolePolicy, created) ⇒
        val arn = RoleArn(account, name, path)
        val role = new aws.Role()
          .withArn(arn.value)
          .withRoleName(name.value)
          .withRoleId(roleId.value)
          .withPath(path.value)
          .withAssumeRolePolicyDocument(assumeRolePolicy.toString)
          .withCreateDate(created)
        Role.fromAws(role).toAws shouldBe role
      }
    }

    "via its AWS equivalent" in {
      forAll { role: Role ⇒
        Role.fromAws(role.toAws) shouldBe role
      }
    }
  }
}
