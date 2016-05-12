package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class RoleArnSpec extends FreeSpec {
  "a RoleArn should" - {
    "provide the correct resource" in {
      forAll { arn: RoleArn ⇒
        val path = arn.path.pathString
        val roleName = arn.name
        arn.resource shouldBe s"role$path$roleName"
      }
    }

    "produce the correct ARN" in {
      forAll { arn: RoleArn ⇒
        val partition = arn.account.partition
        val account = arn.account.id
        val path = arn.path.pathString
        val roleName = arn.name

        arn.arnString shouldBe s"arn:$partition:iam::$account:role$path$roleName"
      }
    }

    "can round-trip via an ARN" in {
      forAll { arn: RoleArn ⇒
        RoleArn(arn.arnString) shouldBe arn
      }
    }
  }
}
