package com.monsanto.arch.awsutil.securitytoken.model

import com.amazonaws.services.securitytoken.model.{AssumedRoleUser ⇒ AwsAssumedRoleUser}
import com.monsanto.arch.awsutil.AwsGen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class AssumedRoleUserSpec extends FreeSpec {
  "a AssumedRoleUser can be round-tripped" - {
    "from its AWS equivalent" in {
      forAll { args: AwsGen.STS.AssumedRoleUserArgs ⇒
        val aws = new AwsAssumedRoleUser()
          .withArn(args.arn)
          .withAssumedRoleId(args.id)

        AssumedRoleUser.fromAws(aws).toAws shouldBe aws
      }
    }

    "via its AWS equivalent" in {
      forAll { args: AwsGen.STS.AssumedRoleUserArgs ⇒
        val assumedRoleUser = args.toAssumedRoleUser

        AssumedRoleUser.fromAws(assumedRoleUser.toAws) shouldBe assumedRoleUser
      }
    }
  }
}
