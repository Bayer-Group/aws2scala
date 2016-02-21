package com.monsanto.arch.awsutil.securitytoken.model

import com.monsanto.arch.awsutil.AwsGen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import com.amazonaws.services.securitytoken.model.{Credentials ⇒ AwsCredentials}

class CredentialsSpec extends FreeSpec {
  "Credentials" - {
    "can be round-tripped" - {
      "from its AWS equivalent" in {
        forAll { args: AwsGen.STS.CredentialsArgs ⇒
          val aws = new AwsCredentials(args.accessKeyId.value, args.secretAccessKey.value, args.sessionToken.value,
            args.expiration)
          Credentials.fromAws(aws).toAws shouldBe aws
        }
      }

      "via its AWS equivalent" in {
        forAll { args: AwsGen.STS.CredentialsArgs ⇒
          val credentials = args.toCredentials
          Credentials.fromAws(credentials.toAws) shouldBe credentials
        }
      }
    }

    "functions as a credentials provider" - {
      "providing session credentials" in {
        forAll { args: AwsGen.STS.CredentialsArgs ⇒
          val sessionCredentials = args.toCredentials.getCredentials

          sessionCredentials should have (
            'AWSAccessKeyId (args.accessKeyId.value),
            'AWSSecretKey (args.secretAccessKey.value),
            'sessionToken (args.sessionToken.value)
          )
        }
      }

      "doing nothing on refresh" in {
        forAll { args: AwsGen.STS.CredentialsArgs ⇒
          args.toCredentials.refresh()
        }
      }
    }
  }
}
