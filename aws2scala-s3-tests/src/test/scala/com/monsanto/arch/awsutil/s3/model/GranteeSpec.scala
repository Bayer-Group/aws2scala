package com.monsanto.arch.awsutil.s3.model

import com.monsanto.arch.awsutil.s3.model.AwsConverters._
import com.monsanto.arch.awsutil.testkit.S3ScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.{S3Gen, UtilGen}
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class GranteeSpec extends FreeSpec {
  "the Grantee object provides" - {
    "a method for creating" - {
      "canonical grantees" in {
        forAll(S3Gen.canonicalIdentifier) { id ⇒
          val grantee = Grantee.canonical(id)
          grantee should have (
            'identifier (id),
            'typeIdentifier ("id")
          )
        }
      }

      "e-mail address grantees" in {
        forAll(UtilGen.emailAddress) { emailAddress ⇒
          val grantee = Grantee.emailAddress(emailAddress)
          grantee should have (
            'identifier (emailAddress),
            'typeIdentifier ("emailAddress")
          )
        }
      }
    }

    "a value for" - {
      "all users" in {
        Grantee.allUsers should have (
          'identifier ("http://acs.amazonaws.com/groups/global/AllUsers"),
          'typeIdentifier ("uri")
        )
      }

      "any authenticated user" in {
        Grantee.authenticatedUsers should have (
          'identifier ("http://acs.amazonaws.com/groups/global/AuthenticatedUsers"),
          'typeIdentifier ("uri")
        )
      }

      "log delivery" in {
        Grantee.logDelivery should have (
          'identifier ("http://acs.amazonaws.com/groups/s3/LogDelivery"),
          'typeIdentifier ("uri")
        )
      }
    }
  }

  "a Grantee can be round-tripped via its AWS equivalent" in {
    forAll { grantee: Grantee ⇒
      grantee.asAws.asScala shouldBe grantee
    }
  }
}
