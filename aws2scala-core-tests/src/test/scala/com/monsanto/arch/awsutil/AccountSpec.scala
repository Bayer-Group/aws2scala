package com.monsanto.arch.awsutil

import com.monsanto.arch.awsutil.partitions.Partition
import com.monsanto.arch.awsutil.testkit.CoreGen
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

    "be constructable from an account number" in {
      forAll(CoreGen.accountId) { id ⇒
        Account(id) shouldBe Account(id, Partition.Aws)
      }
    }

    "be extractable from an account number" in {
      forAll(CoreGen.accountId) { id ⇒
        Account.fromNumber.unapply(id) shouldBe Some(Account(id, Partition.Aws))
      }
    }

    "generate the correct account ARN" in {
      forAll { account: Account ⇒
        account.arn shouldBe AccountArn(account)
      }
    }
  }
}
