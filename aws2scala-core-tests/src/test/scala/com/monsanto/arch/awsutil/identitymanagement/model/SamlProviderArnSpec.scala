package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.Arn
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class SamlProviderArnSpec extends FreeSpec {
  "a SamlProviderArn should" - {
    "have the correct resource" in {
      forAll { arn: SamlProviderArn ⇒
        arn.resource shouldBe s"saml-provider/${arn.name}"
      }
    }

    "produce the correct ARN" in {
      forAll { arn: SamlProviderArn ⇒
        arn.arnString shouldBe s"arn:${arn.account.partition}:iam::${arn.account.id}:saml-provider/${arn.name}"
      }
    }

    "round-trip via an ARN" in {
      forAll { arn: SamlProviderArn ⇒
        Arn.unapply(arn.arnString) shouldBe Some(arn)
      }
    }
  }
}
