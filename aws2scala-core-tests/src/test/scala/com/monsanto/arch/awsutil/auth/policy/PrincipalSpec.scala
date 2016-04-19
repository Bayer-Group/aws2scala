package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.auth.policy.AwsConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import com.monsanto.arch.awsutil.testkit.AwsGen
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalatest.prop.TableDrivenPropertyChecks.{forAll ⇒ forAllIn, _}

class PrincipalSpec extends FreeSpec with AwsEnumerationBehaviours {
  val services = Table("service", Principal.Service.values: _*)
  val webIdentityProviders = Table("web identity provider", Principal.WebIdentityProvider.values: _*)

  "a Principal" - {
    "can be round-tripped" in {
      forAll { principal: Principal ⇒
        principal.asAws.asScala shouldBe principal
      }
    }

    "converts" - {
      "AWS account principals that only contain an account number" in {
        forAll { account: Account ⇒
          new aws.Principal(account.id).asScala shouldBe Principal.account(account.id)
        }
      }

      "the all principals value to the AWS constant" in {
        Principal.all.asAws should be theSameInstanceAs aws.Principal.All
      }

      "all services value to the AWS constant" in {
        Principal.allServices.asAws should be theSameInstanceAs aws.Principal.AllServices
      }

      "all web identity services to the AWS constant" in {
        Principal.allWebProviders.asAws should be theSameInstanceAs aws.Principal.AllWebProviders
      }

      "SAML provider principals" in {
        forAll { principal: Principal.SamlProviderPrincipal ⇒
          principal.asAws should have (
            'provider ("Federated"),
            'id (s"arn:${principal.account.partition}:iam::${principal.account.id}:saml-provider/${principal.name}")
          )
        }
      }

      "IAM user principals" in {
        forAll { principal: Principal.IamUserPrincipal ⇒
          principal.asAws should have (
            'provider ("AWS"),
            'id (s"arn:${principal.account.partition}:iam::${principal.account.id}:user${principal.path.getOrElse("/")}${principal.name}")
          )
        }
      }

      "IAM role principals" in {
        forAll { principal: Principal.IamRolePrincipal ⇒
          principal.asAws should have (
            'provider ("AWS"),
            'id (s"arn:${principal.account.partition}:iam::${principal.account.id}:role${principal.path.getOrElse("/")}${principal.name}")
          )
        }
      }

      "IAM assumed role principals" in {
        forAll { principal: Principal.IamAssumedRolePrincipal ⇒
          principal.asAws should have (
            'provider ("AWS"),
            'id (s"arn:${principal.account.partition}:iam::${principal.account.id}:assumed-role/${principal.roleName}/${principal.sessionName}")
          )
        }
      }
    }

    "has a valid" - {
      "account principal factory method" in {
        forAll { account: Account ⇒
          Principal.account(account.id) should have (
            'provider ("AWS"),
            'id (s"arn:aws:iam::$account:root")
          )
        }
      }

      "service principal factory method" in {
        forAllIn(services) { service ⇒
          Principal.service(service) should have (
            'provider ("Service"),
            'id (service.id)
          )
        }
      }

      "web identity provider principal factory method" in {
        forAllIn(webIdentityProviders) { provider ⇒
          Principal.webProvider(provider) should have (
            'provider ("Federated"),
            'id (provider.id)
          )
        }
      }

      "SAML provider principal factory method" in {
        forAll(
          AwsGen.accountId → "account",
          AwsGen.iamName → "SAML provider name"
        ) { (account, samlProviderName) ⇒
          Principal.samlProvider(account, samlProviderName) shouldBe Principal.SamlProviderPrincipal(Account(account), samlProviderName)
        }
      }

      "IAM user principal factory method" in {
        forAll(
          AwsGen.accountId → "account",
          AwsGen.iamName → "IAM user name"
        ) { (account, name) ⇒
          Principal.iamUser(account, name) shouldBe Principal.IamUserPrincipal(Account(account), name, None)
        }
      }

      "IAM role principal factory method" in {
        forAll(
          AwsGen.accountId → "account",
          AwsGen.iamName → "IAM role name"
        ) { (account, name) ⇒
          Principal.iamRole(account, name) shouldBe Principal.IamRolePrincipal(Account(account), name, None)
        }
      }

      "IAM assumed role principal factory method" in {
        forAll(
          AwsGen.accountId → "account",
          AwsGen.iamName → "IAM role name",
          AwsGen.iamName → "IAM assumed role session name"
        ) { (account, roleName, sessionName) ⇒
          Principal.iamAssumedRole(account, roleName, sessionName) shouldBe Principal.IamAssumedRolePrincipal(Account(account), roleName, sessionName)
        }
      }
    }

    "has a Service enumeration" - {
      behave like anAwsEnumeration(Principal.Service)

      "with id values" in {
        forAllIn(services) { service ⇒
          service.id shouldBe service.toAws.getServiceId
        }
      }

      "with a ById extractor" in {
        forAllIn(services) { service ⇒
          Principal.Service.ById.unapply(service.id) shouldBe Some(service)
        }
      }
    }

    "has a WebIdentityProvider enumeration" - {
      behave like anAwsEnumeration(Principal.WebIdentityProvider)

      "with id values" in {
        forAllIn(webIdentityProviders) { provider ⇒
          provider.id shouldBe provider.toAws.getWebIdentityProvider
        }
      }

      "with a fromId" in {
        forAllIn(webIdentityProviders) { provider ⇒
          Principal.WebIdentityProvider.ById.unapply(provider.id) shouldBe Some(provider)
        }
      }
    }
  }
}
