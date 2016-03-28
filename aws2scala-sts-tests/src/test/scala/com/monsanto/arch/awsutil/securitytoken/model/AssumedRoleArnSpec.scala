package com.monsanto.arch.awsutil.securitytoken.model

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.identitymanagement.model.Name
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.StsGen
import com.monsanto.arch.awsutil.testkit.StsScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class AssumedRoleArnSpec extends FreeSpec {
  "an AssumedRoleArn should" - {
    "provide the correct resource" in {
      forAll(
        arbitrary[Account] → "account",
        arbitrary[Name] → "roleName",
        StsGen.roleSessionName → "sessionName"
      ) { (account, roleName, sessionName) ⇒
        val arn = AssumedRoleArn(account, roleName.value, sessionName)
        arn.resource shouldBe s"assumed-role/$roleName/$sessionName"
      }
    }

    "produce the correct ARN" in {
      forAll(
        arbitrary[Account] → "account",
        arbitrary[Name] → "roleName",
        StsGen.roleSessionName → "sessionName"
      ) { (account, roleName, sessionName) ⇒
        val arn = AssumedRoleArn(account, roleName.value, sessionName)
        arn.value shouldBe s"arn:${account.partition}:sts::$account:assumed-role/$roleName/$sessionName"
      }
    }

    "can round-trip via an ARN" in {
      forAll { arn: AssumedRoleArn ⇒
        AssumedRoleArn(arn.value) shouldBe arn
      }
    }
  }
}
