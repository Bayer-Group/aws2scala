package com.monsanto.arch.awsutil.identitymanagement.model

import java.util.Date

import akka.Done
import akka.stream.Materializer
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.converters.IamConverters._
import com.monsanto.arch.awsutil.identitymanagement.AsyncIdentityManagementClient
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Materialised
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamGen
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

import scala.concurrent.Future

class ManagedPolicySpec extends FreeSpec with Materialised with MockFactory {
  "a ManagedPolicy should" - {
    "round-trip via its AWS equivalent" in {
      forAll { policy: ManagedPolicy ⇒
        policy.asAws.asScala shouldBe policy
      }
    }

    "create a new version for itself" in {
      forAll { (policy: ManagedPolicy, document: Policy, setAsDefault: Boolean) ⇒
        implicit val client = mock[AsyncIdentityManagementClient]("client")

        val version = ManagedPolicyVersion(policy.arn, None, "v2", setAsDefault, new Date)

        (client.createPolicyVersion(_: PolicyArn, _: Policy, _: Boolean)(_: Materializer))
          .expects(policy.arn, document, setAsDefault, materialiser)
          .returning(Future.successful(version))

        val result = policy.createVersion(document, setAsDefault).futureValue
        result shouldBe version
      }
    }

    "get specific versions of itself" in {
      forAll { (policy: ManagedPolicy, rawVersion: ManagedPolicyVersion) ⇒
        implicit val client = mock[AsyncIdentityManagementClient]("client")

        val version = rawVersion.copy(policyArn = policy.arn)

        (client.getPolicyVersion(_: PolicyArn, _: String)(_: Materializer))
          .expects(policy.arn, rawVersion.versionId, materialiser)
          .returning(Future.successful(version))

        val result = policy.getVersion(version.versionId).futureValue
        result shouldBe version
      }
    }

    "list its versions" in {
      implicit val arbPolicyVersions: Arbitrary[List[ManagedPolicyVersion]] =
        Arbitrary(Gen.resize(10, Gen.nonEmptyListOf(arbitrary[ManagedPolicyVersion])))
      forAll { (policy: ManagedPolicy, versions: List[ManagedPolicyVersion]) ⇒
        implicit val client = mock[AsyncIdentityManagementClient]("client")

        (client.listPolicyVersions(_: PolicyArn)(_: Materializer))
          .expects(policy.arn, materialiser)
          .returning(Future.successful(versions))

        val result = policy.versions().futureValue
        result shouldBe versions
      }
    }

    "can set its default version" - {
      "from version IDs" in {
        forAll(
          arbitrary[ManagedPolicy] → "policy",
          IamGen.policyVersionId → "versionId"
        ) { (policy, versionId) ⇒
          implicit val client = mock[AsyncIdentityManagementClient]("client")

          (client.setDefaultPolicyVersion(_: PolicyArn, _: String)(_: Materializer))
            .expects(policy.arn, versionId, materialiser)
            .returning(Future.successful(Done))

          val result = policy.setDefaultVersion(versionId).futureValue
          result shouldBe Done
        }
      }

      "from version objects" - {
        "when the policy ARNs match" in {
          forAll { (policy: ManagedPolicy, rawVersion: ManagedPolicyVersion) ⇒
            implicit val client = mock[AsyncIdentityManagementClient]("client")

            val version = rawVersion.copy(policyArn = policy.arn)

            (client.setDefaultPolicyVersion(_: PolicyArn, _: String)(_: Materializer))
              .expects(policy.arn, version.versionId, materialiser)
              .returning(Future.successful(Done))

            val result = policy.setDefaultVersion(version).futureValue
            result shouldBe Done
          }
        }

        "unless the policy ARNs differ" in {
          forAll { (policy: ManagedPolicy, version: ManagedPolicyVersion) ⇒
            implicit val client = mock[AsyncIdentityManagementClient]("client")

            whenever(version.policyArn != policy.arn) {
              an [IllegalArgumentException] shouldBe thrownBy {
                policy.setDefaultVersion(version).futureValue
              }
            }
          }
        }
      }
    }

    "can delete its versions" - {
      "using version IDs" in {
        forAll(
          arbitrary[ManagedPolicy] → "policy",
          IamGen.policyVersionId → "versionId"
        ) { (policy, versionId) ⇒
          implicit val client = mock[AsyncIdentityManagementClient]("client")

          (client.deletePolicyVersion(_: PolicyArn, _: String)(_: Materializer))
            .expects(policy.arn, versionId, materialiser)
            .returning(Future.successful(Done))

          val result = policy.deleteVersion(versionId).futureValue
          result shouldBe Done
        }
      }

      "using version objects" - {
        "when the policy ARNs match" in {
          forAll { (policy: ManagedPolicy, rawVersion: ManagedPolicyVersion) ⇒
            implicit val client = mock[AsyncIdentityManagementClient]("client")

            val version = rawVersion.copy(policyArn = policy.arn)

            (client.deletePolicyVersion(_: PolicyArn, _: String)(_: Materializer))
              .expects(policy.arn, version.versionId, materialiser)
              .returning(Future.successful(Done))

            val result = policy.deleteVersion(version).futureValue
            result shouldBe Done
          }
        }

        "unless the policy ARNs differ" in {
          forAll { (policy: ManagedPolicy, version: ManagedPolicyVersion) ⇒
            implicit val client = mock[AsyncIdentityManagementClient]("client")

            whenever(version.policyArn != policy.arn) {
              an [IllegalArgumentException] shouldBe thrownBy {
                policy.deleteVersion(version).futureValue
              }
            }
          }
        }
      }
    }

    "delete itself" in {
      forAll { policy: ManagedPolicy ⇒
        implicit val client = mock[AsyncIdentityManagementClient]("client")

        (client.deletePolicy(_: PolicyArn)(_: Materializer))
          .expects(policy.arn, materialiser)
          .returning(Future.successful(Done))

        val result = policy.delete().futureValue
        result shouldBe Done
      }
    }

    "refresh itself" in {
      forAll { (policy1: ManagedPolicy, policy2: ManagedPolicy) ⇒
        implicit val client = mock[AsyncIdentityManagementClient]("client")

        (client.getPolicy(_: PolicyArn)(_: Materializer))
          .expects(policy1.arn, materialiser)
          .returning(Future.successful(policy2))

        val result = policy1.refresh().futureValue
        result shouldBe policy2
      }
    }
  }
}
