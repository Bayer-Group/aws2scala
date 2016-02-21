package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.model.{User ⇒ AwsUser}
import com.monsanto.arch.awsutil.AwsGen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class UserSpec extends FreeSpec {
  "a User" - {
    "can be round-tripped" - {
      "from its AWS equivalent" in {
        forAll { args: AwsGen.IAM.UserArgs ⇒
          val aws = new AwsUser(args.path.value, args.name.value, args.id.value, args.arn.value, args.created)
          args.passwordLastUsed.foreach(d ⇒ aws.setPasswordLastUsed(d))

          User.fromAws(aws).toAws shouldBe aws
        }
      }

      "via its AWS equivalent" in {
        forAll { args: AwsGen.IAM.UserArgs ⇒
          val user = args.toUser
          User.fromAws(user.toAws) shouldBe user
        }
      }
    }

    "can provide its account ID" in {
      forAll { args: AwsGen.IAM.UserArgs ⇒
        val user = args.toUser
        user.account shouldBe args.account.value
      }
    }
  }
}
