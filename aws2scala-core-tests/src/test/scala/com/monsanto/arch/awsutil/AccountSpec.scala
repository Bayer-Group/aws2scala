package com.monsanto.arch.awsutil

import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class AccountSpec extends FreeSpec {
  "an Account must" - {
    "have a 12-digit ID" in {
      forAll { id: String ⇒
        whenever(!id.matches("^[0-9]{12}$")) {
          an [IllegalArgumentException] shouldBe thrownBy {
            Account(id)
          }
        }
      }
    }

    "generate the correct account ARN" in {
      forAll { account: Account ⇒
        account.arn shouldBe AccountArn(account)
      }
    }
  }
}
