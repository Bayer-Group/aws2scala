package com.monsanto.arch.awsutil.identitymanagement.model

import java.util.Date

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import com.monsanto.arch.awsutil.{Account, Arn}
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
          arbitrary[Name] → "name",
          arbitrary[Path] → "path",
          arbitrary[UserId] → "userId",
          arbitrary[Date] → "created",
          arbitrary[Option[Date]] → "passwordLastUsed"
        ) { (account, name, path, id, created, passwordLastUsed) ⇒
          val awsUser = new aws.User(path.value, name.value, id.value, UserArn(account, name, path).value, created)
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
        val Arn(_, _, _, Some(account), _) = user.arn
        user.account shouldBe account.id
      }
    }
  }
}
