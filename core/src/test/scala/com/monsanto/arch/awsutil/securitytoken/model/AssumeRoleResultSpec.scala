package com.monsanto.arch.awsutil.securitytoken.model

import com.amazonaws.services.securitytoken.model.{AssumeRoleResult ⇒ AwsAssumeRoleResult}
import com.monsanto.arch.awsutil.AwsGen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class AssumeRoleResultSpec extends FreeSpec {
  "an AssumeRoleResult" - {
    "can be round-tripped" - {
      "from its AWS equivalent" in {
        forAll { args: AwsGen.STS.AssumeRoleResultArgs ⇒
          val aws = new AwsAssumeRoleResult
          aws.setAssumedRoleUser(args.assumedRoleUser.toAws)
          aws.setCredentials(args.credentials.toAws)
          args.packedPolicySize.foreach(pps ⇒ aws.setPackedPolicySize(pps.value))

          AssumeRoleResult.fromAws(aws).toAws shouldBe aws
        }
      }

      "via its AWS equivalent" in {
        forAll { args: AwsGen.STS.AssumeRoleResultArgs ⇒
          val result = args.toResult

          AssumeRoleResult.fromAws(result.toAws) shouldBe result
        }
      }
    }
  }
}
