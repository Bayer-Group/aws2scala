package com.monsanto.arch.awsutil.securitytoken.model

import com.amazonaws.services.securitytoken.{model ⇒ aws}
import com.monsanto.arch.awsutil.securitytoken.model.AwsConverters._
import com.monsanto.arch.awsutil.testkit.StsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class CredentialsSpec extends FreeSpec {
  "Credentials" - {
    "can be round-tripped" - {
      "from its AWS equivalent" in {
        forAll { credentials: Credentials ⇒
          val awsCredentials = new aws.Credentials(credentials.accessKeyId, credentials.secretAccessKey,
            credentials.sessionToken, credentials.expiration)
          awsCredentials.asScala.asAws shouldBe awsCredentials
        }
      }

      "via its AWS equivalent" in {
        forAll { credentials: Credentials ⇒
          credentials.asAws.asScala shouldBe credentials
        }
      }
    }

    "functions as a credentials provider" - {
      "providing session credentials" in {
        forAll { credentials: Credentials ⇒
          val sessionCredentials = credentials.getCredentials

          sessionCredentials should have (
            'AWSAccessKeyId (credentials.accessKeyId),
            'AWSSecretKey (credentials.secretAccessKey),
            'sessionToken (credentials.sessionToken)
          )
        }
      }

      "doing nothing on refresh" in {
        forAll { credentials: Credentials ⇒
          credentials.refresh()
        }
      }
    }
  }
}
