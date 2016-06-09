package com.monsanto.arch.awsutil.identitymanagement

import java.util.Date

import akka.Done
import com.monsanto.arch.awsutil.auth.policy.PolicyDSL._
import com.monsanto.arch.awsutil.auth.policy.action.{IdentityManagementAction, SecurityTokenServiceAction}
import com.monsanto.arch.awsutil.auth.policy.{Policy, Principal, Resource}
import com.monsanto.arch.awsutil.identitymanagement.model._
import com.monsanto.arch.awsutil.test_support.AwsScalaFutures._
import com.monsanto.arch.awsutil.test_support.{AwsIntegrationSpec, IntegrationCleanup, IntegrationTest}
import com.typesafe.scalalogging.StrictLogging
import org.scalactic.Equality
import org.scalatest.FreeSpec
import org.scalatest.Matchers._

@IntegrationTest
class IdentityManagementIntegrationSpec extends FreeSpec with AwsIntegrationSpec with StrictLogging with IntegrationCleanup {
  private val async = awsClient.async(IdentityManagement)

  private val testPathPrefix = Path.empty / "aws2scala-it-iam"
  private val testPath = testPathPrefix / testId
  private val testPolicyName = s"TestPolicy-$testId"
  private var testUser: User = _
  private var testRole: Role = _
  private var testPolicyArn: PolicyArn = _
  private var testPolicy: ManagedPolicy = _
  private var version1Created: Date = _
  private var version2Created: Date = _

  lazy val documentV2 = policy (
    statements (
      allow (
        actions(IdentityManagementAction.GetUser, IdentityManagementAction.GetUserPolicy),
        resources(Resource(testUser.arn.arnString))
      )
    )
  )

  "the Identity Management client can" - {
    "get the current user" in {
      val result = async.getCurrentUser().futureValue
      result.name should not be empty

      logger.info(s"Current AWS account is ${result.account}")

      testUser = result
    }

    "create a role" in {
      val roleName = s"TestRole-$testId"
      val result = async.createRole(roleName, assumeRolePolicy(testUser), testPath).futureValue
      result.arn shouldBe RoleArn(testUser.account, roleName, testPath)

      logger.info(s"Created role ${result.name} with ARN ${result.arn}")

      testRole = result
    }

    "list roles" - {
      val roleId = new Equality[Role] {
        override def areEqual(a: Role, b: Any) = {
          b match {
            case r: Role ⇒ a.id == r.id
            case _ ⇒ false
          }
        }
      }

      "all of them" in {
        val result = async.listRoles().futureValue
        (result should contain (testRole)) (decided by roleId)
      }

      "with a prefix" in {
        val result = async.listRoles(testPath).futureValue
        (result should contain (testRole)) (decided by roleId)
      }
    }

    "create a managed policy" in {
      val document = policy (
        statements (
          allow (
            actions(IdentityManagementAction.GetUser),
            resources(Resource(testUser.arn.arnString))
          )
        )
      )
      val result = async.createPolicy(testPolicyName, document, "A test policy", testPathPrefix).futureValue

      testPolicyArn = result.arn

      logger.info(s"Created policy $testPolicyName at ${testPolicyArn.arnString}")
    }

    "get the managed policy" in {
      val result = async.getPolicy(testPolicyArn).futureValue
      val theArn = testPolicyArn
      result should matchPattern {
        case ManagedPolicy(`testPolicyName`, _, `theArn`, `testPathPrefix`, "v1", 0, true, Some("A test policy"), _, _) ⇒
      }

      version1Created = result.created
      testPolicy = result
    }

    "list the policy" - {
      lazy val policyId = new Equality[ManagedPolicy] {
        override def areEqual(a: ManagedPolicy, b: Any) = {
          b match {
            case p: ManagedPolicy ⇒ a.id == p.id
            case _ ⇒ false
          }
        }
      }

      "in a list of all policies" in {
        val result = async.listPolicies().futureValue
        (result should contain (testPolicy)) (decided by policyId)
      }

      "in a list of local policies" in {
        val result = async.listLocalPolicies().futureValue
        (result should contain (testPolicy)) (decided by policyId)
      }

      "in a list of policies filtered by prefix" in {
        val result = async.listLocalPolicies().futureValue
        (result should contain (testPolicy)) (decided by policyId)
      }
    }

    "create a new version of the managed policy" in {
      val result = async.createPolicyVersion(testPolicyArn, documentV2, setAsDefault = false).futureValue

      result should have (
        'document (None),
        'versionId ("v2"),
        'isDefaultVersion (false)
      )

      // round to nearest second
      version2Created = new Date((result.created.getTime / 1000) * 1000)

      logger.info(s"Created policy version ${result.versionId} for ${testPolicyArn.arnString}")
    }

    "list policy versions" in {
      val result = async.listPolicyVersions(testPolicyArn).futureValue

      result should contain theSameElementsAs
        Seq(
          ManagedPolicyVersion(None, "v1", isDefaultVersion = true, version1Created),
          ManagedPolicyVersion(None, "v2", isDefaultVersion = false, version2Created))
    }

    "set the default policy version" in {
      val result = async.setDefaultPolicyVersion(testPolicyArn, "v2").futureValue
      result shouldBe Done
    }

    "get a policy version" in {
      val result = async.getPolicyVersion(testPolicyArn, "v2").futureValue

      result should have (
        'document (Some(documentV2)),
        'versionId ("v2"),
        'isDefaultVersion (true),
        'created (version2Created)
      )
    }

    "attach a policy to a role" in {
      val result = async.attachRolePolicy(testRole.name, testPolicyArn).futureValue
      result shouldBe Done
    }

    "list the policies attached to a role" in {
      val result = async.listAttachedRolePolicies(testRole.name).futureValue
      result should contain (AttachedPolicy(testPolicyArn, testPolicyName))
    }

    "detach a policy from a role" in {
      val result = async.detachRolePolicy(testRole.name, testPolicyArn).futureValue
      result shouldBe Done
    }

    "delete a policy version" in {
      val result = async.deletePolicyVersion(testPolicyArn, "v1").futureValue
      result shouldBe Done

      logger.info(s"Deleted policy version v1 from ${testPolicyArn.arnString}")
    }

    "delete the policy" in {
      val result = async.deletePolicy(testPolicyArn).futureValue
      result shouldBe Done

      logger.info(s"Deleted policy ${testPolicyArn.name} with ARN ${testPolicyArn.arnString}")
    }

    "delete the role" in {
      val result = async.deleteRole(testRole.name).futureValue
      result shouldBe Done

      logger.info(s"Deleted role ${testRole.name} with ARN ${testRole.arn}")
    }

    behave like cleanupIAMRoles(testPathPrefix)
    behave like cleanupIAMPolicies(testPathPrefix)
  }

  def assumeRolePolicy(user: User): Policy = {
    policy (
      statements (
        allow (
          principals(Principal.iamUser(user.arn)),
          actions(SecurityTokenServiceAction.AssumeRole)
        )
      )
    )
  }
}
