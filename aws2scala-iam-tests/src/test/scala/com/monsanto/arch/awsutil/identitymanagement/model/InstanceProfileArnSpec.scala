package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.identitymanagement.IdentityManagement
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class InstanceProfileArnSpec extends FreeSpec {
  IdentityManagement.init()

  "a InstanceProfileArn should" - {
    "provide the correct resource" in {
      forAll { arn: InstanceProfileArn ⇒
        arn.resource shouldBe s"instance-profile${arn.path.pathString}${arn.name}"
      }
    }

    "produce the correct ARN" in {
      forAll { arn: InstanceProfileArn ⇒
        val partition = arn.account.partition.id
        val accountId = arn.account.id
        val path = arn.path.pathString
        val instanceProfileName = arn.name

        arn.arnString shouldBe s"arn:$partition:iam::$accountId:instance-profile$path$instanceProfileName"
      }
    }

    "can round-trip via an ARN" in {
      forAll { arn: InstanceProfileArn ⇒
        InstanceProfileArn(arn.arnString) shouldBe arn
      }
    }
  }
}
