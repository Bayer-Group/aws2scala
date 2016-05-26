package com.monsanto.arch.awsutil.identitymanagement.model

import java.util.Date

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.testkit.CoreGen
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class UserSpec extends FreeSpec {
  "a User" - {
    "can be round-tripped" - {
      "from its AWS equivalent" in {
        forAll (
          arbitrary[Account] → "account",
          CoreGen.iamName → "name",
          arbitrary[Path] → "path",
          arbitrary[UserId] → "userId",
          arbitrary[Date] → "created",
          arbitrary[Option[Date]] → "passwordLastUsed"
        ) { (account, name, path, id, created, passwordLastUsed) ⇒
          val awsUser = new aws.User(path.pathString, name, id.value, UserArn(account, name, path).arnString, created)
          passwordLastUsed.foreach(d ⇒ awsUser.setPasswordLastUsed(d))

          User.fromAws(awsUser).toAws shouldBe awsUser
        }
      }

      "via its AWS equivalent" in {
        forAll { user: User ⇒
          User.fromAws(user.toAws) shouldBe user
        }
      }
    }

    "can provide its account ID" in {
      forAll { user: User ⇒
        val UserArn(account, _, _) = UserArn.fromArnString(user.arn)
        user.account shouldBe account
      }
    }
  }
}
