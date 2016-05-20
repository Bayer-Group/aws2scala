package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.converters.CoreConverters._
import com.monsanto.arch.awsutil.identitymanagement.model.{RoleArn, SamlProviderArn, UserArn}
import com.monsanto.arch.awsutil.partitions.Partition
import com.monsanto.arch.awsutil.securitytoken.model.AssumedRoleArn
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalatest.prop.TableDrivenPropertyChecks.{forAll ⇒ forAllIn, _}

class PrincipalSpec extends FreeSpec with AwsEnumerationBehaviours {
  val services = Table("service", Principal.Service.values: _*)
  val webIdentityProviders = Table("web identity provider", Principal.WebIdentityProvider.values: _*)

  "a Principal" - {
    "can be round-tripped via" - {
      "its AWS equivalent" in {
        forAll { principal: Principal ⇒
          principal.asAws.asScala shouldBe principal
        }
      }

      "its provider and identifier" in {
        forAll { principal: Principal ⇒
          Principal(principal.provider, principal.id) shouldBe principal
        }
      }
    }

    "converts" - {
      "AWS account principals that only contain an account number" in {
        forAll { account: Account ⇒
          new aws.Principal(account.id).asScala shouldBe Principal.account(account.copy(partition = Partition.Aws))
        }
      }

      "the all principals value to the AWS constant" in {
        Principal.AllPrincipals.asAws should be theSameInstanceAs aws.Principal.All
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
            'id (principal.samlProviderArn.arnString)
          )
        }
      }

      "IAM user principals" in {
        forAll { principal: Principal.IamUserPrincipal ⇒
          principal.asAws should have (
            'provider ("AWS"),
            'id (principal.userArn.arnString)
          )
        }
      }

      "IAM role principals" in {
        forAll { principal: Principal.IamRolePrincipal ⇒
          principal.asAws should have (
            'provider ("AWS"),
            'id (principal.roleArn.arnString)
          )
        }
      }

      "IAM assumed role principals" in {
        forAll { principal: Principal.StsAssumedRolePrincipal ⇒
          principal.asAws should have (
            'provider ("AWS"),
            'id (principal.assumedRoleArn.arnString)
          )
        }
      }
    }

    "has a valid" - {
      "account principal factory method" in {
        forAll { account: Account ⇒
          Principal.account(account) should have (
            'provider ("AWS"),
            'id (account.arn.arnString)
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
            'id (provider.provider)
          )
        }
      }

      "SAML provider principal factory method" in {
        forAll { samlProviderArn: SamlProviderArn ⇒
          Principal.samlProvider(samlProviderArn) shouldBe Principal.SamlProviderPrincipal(samlProviderArn)
        }
      }

      "IAM user principal factory method" in {
        forAll { userArn: UserArn ⇒
          Principal.iamUser(userArn) shouldBe Principal.IamUserPrincipal(userArn)
        }
      }

      "IAM role principal factory method" in {
        forAll { roleArn: RoleArn ⇒
          Principal.iamRole(roleArn) shouldBe Principal.IamRolePrincipal(roleArn)
        }
      }

      "IAM assumed role principal factory method" in {
        forAll { asssumedRoleArn: AssumedRoleArn ⇒
          Principal.stsAssumedRole(asssumedRoleArn) shouldBe Principal.StsAssumedRolePrincipal(asssumedRoleArn)
        }
      }
    }

    "has a Service enumeration" - {
      behave like anAwsEnumeration(
        aws.Principal.Services.values,
        Principal.Service.values,
        (_ : Principal.Service).asAws,
        (_: aws.Principal.Services).asScala)

      "with id values" in {
        forAllIn(services) { service ⇒
          service.id shouldBe service.asAws.getServiceId
        }
      }

      "with a fromId extractor" in {
        forAllIn(services) { service ⇒
          Principal.Service.fromId.unapply(service.id) shouldBe Some(service)
        }
      }
    }

    "has a WebIdentityProvider enumeration" - {
      behave like anAwsEnumeration(
        aws.Principal.WebIdentityProviders.values,
        Principal.WebIdentityProvider.values,
        (_: Principal.WebIdentityProvider).asAws,
        (_: aws.Principal.WebIdentityProviders).asScala)

      "with id values" in {
        forAllIn(webIdentityProviders) { provider ⇒
          provider.provider shouldBe provider.asAws.getWebIdentityProvider
        }
      }

      "with a fromId" in {
        forAllIn(webIdentityProviders) { provider ⇒
          Principal.WebIdentityProvider.fromProvider.unapply(provider.provider) shouldBe Some(provider)
        }
      }
    }
  }
}
