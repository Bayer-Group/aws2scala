package com.monsanto.arch.awsutil.identitymanagement

import akka.Done
import com.monsanto.arch.awsutil.auth.policy.PolicyDSL._
import com.monsanto.arch.awsutil.auth.policy.action.SecurityTokenServiceAction
import com.monsanto.arch.awsutil.auth.policy.{Policy, Principal}
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

  private val iamReadOnlyPolicy =
    AttachedPolicy(PolicyArn.fromArnString("arn:aws:iam::aws:policy/IAMReadOnlyAccess"), "IAMReadOnlyAccess")

  private val testPathPrefix = "/aws2scala-it-iam/"
  private val testPath = Path.fromPathString(s"$testPathPrefix$testId/")
  private var testUser: User = _
  private var testRole: Role = _

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
      result.arn shouldBe s"arn:aws:iam::${testUser.account.id}:role${testPath.pathString}$roleName"

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
        val result = async.listRoles(testPath.pathString).futureValue
        (result should contain (testRole)) (decided by roleId)
      }
    }

    "attach a policy to a role" in {
      val result = async.attachRolePolicy(testRole.name, iamReadOnlyPolicy.arn).futureValue
      result shouldBe Done
    }

    "list the policies attache to a role" in {
      val result = async.listAttachedRolePolicies(testRole.name).futureValue
      result should contain (iamReadOnlyPolicy)
    }

    "detach a policy from a role" in {
      val result = async.detachRolePolicy(testRole.name, iamReadOnlyPolicy.arn.arnString).futureValue
      result shouldBe Done
    }

    "delete the role" in {
      val result = async.deleteRole(testRole.name).futureValue
      result shouldBe Done

      logger.info(s"Deleted role ${testRole.name} with ARN ${testRole.arn}")
    }

    behave like cleanupIAMRoles(testPathPrefix)
  }

  def assumeRolePolicy(user: User): Policy = {
    policy (
      statements (
        allow (
          principals(Principal.iamUser(UserArn.fromArnString(user.arn))),
          actions(SecurityTokenServiceAction.AssumeRole)
        )
      )
    )
  }
}
