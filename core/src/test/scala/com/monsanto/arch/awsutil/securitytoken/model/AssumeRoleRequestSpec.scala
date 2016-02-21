package com.monsanto.arch.awsutil.securitytoken.model

import com.monsanto.arch.awsutil.AwsGen.STS.AssumeRoleRequestArgs
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class AssumeRoleRequestSpec extends FreeSpec {
  "an AssumeRoleRequestInstance will" - {
    "correctly convert to its AWS equivalent" in {
      forAll { args: AssumeRoleRequestArgs ⇒
        val request = args.toRequest
        val aws = request.toAws
        aws should have (
          'roleArn (request.roleArn),
          'roleSessionName (request.roleSessionName),
          'durationSeconds (request.duration.map(d ⇒ Integer.valueOf(d.toSeconds.toInt)).orNull),
          'externalId (request.externalId.orNull),
          'policy (request.policy.orNull),
          'serialNumber (request.mfa.map(_.serialNumber).orNull),
          'tokenCode (request.mfa.map(_.tokenCode).orNull)
        )
      }
    }
  }
}
