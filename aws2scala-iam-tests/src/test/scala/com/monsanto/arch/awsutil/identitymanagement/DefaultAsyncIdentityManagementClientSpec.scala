package com.monsanto.arch.awsutil.identitymanagement

import akka.Done
import com.monsanto.arch.awsutil.identitymanagement.model._
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Samplers.{EnhancedGen, arbitrarySample}
import com.monsanto.arch.awsutil.test_support.{FlowMockUtils, Materialised}
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.{CoreGen, IamGen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DefaultAsyncIdentityManagementClientSpec extends FreeSpec with MockFactory with Materialised with FlowMockUtils {
  "the default async identity management client should" - {
    "create roles" - {
      "without paths" in {
        forAll(
          CoreGen.iamName → "roleName",
          IamGen.assumeRolePolicy → "policy"
        ) { (roleName, policy) ⇒
          val streaming = mock[StreamingIdentityManagementClient]("streaming")
          val async = new DefaultAsyncIdentityManagementClient(streaming)

          val role = IamGen.role(roleName, policy).reallySample

          (streaming.roleCreator _)
            .expects()
            .returningFlow(CreateRoleRequest(roleName, policy, None), role)

          val result = async.createRole(roleName, policy).futureValue
          result shouldBe role
        }
      }

      "with paths" in {
        forAll(
          CoreGen.iamName → "roleName",
          IamGen.assumeRolePolicy → "policy",
          arbitrary[Path] → "path"
        ) { (roleName, policy, path) ⇒
          val streaming = mock[StreamingIdentityManagementClient]("streaming")
          val async = new DefaultAsyncIdentityManagementClient(streaming)

          val role = IamGen.role(roleName, policy, path).reallySample

          (streaming.roleCreator _)
            .expects()
            .returningFlow(CreateRoleRequest(roleName, policy, Some(path)), role)

          val result = async.createRole(roleName, policy, path).futureValue
          result shouldBe role
        }
      }
    }

    "delete roles" in {
      forAll(CoreGen.iamName → "roleName") { roleName ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.roleDeleter _)
          .expects()
          .returningFlow(roleName, roleName)

        val result = async.deleteRole(roleName).futureValue
        result shouldBe Done
      }
    }

    "list all roles" in {
      forAll(maxSize(50)) { roles: List[Role] ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.roleLister _)
          .expects()
          .returningConcatFlow(ListRolesRequest.allRoles, roles)

        val result = async.listRoles().futureValue
        result shouldBe roles
      }
    }

    "list roles matching a prefix" in {
      forAll { prefix: Path ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        val roles = arbitrarySample[List[Role]](20)

        (streaming.roleLister _)
          .expects()
          .returningConcatFlow(ListRolesRequest.withPathPrefix(prefix.pathString), roles)

        val result = async.listRoles(prefix.pathString).futureValue
        result shouldBe roles
      }
    }

    "attach managed policies to roles" in {
      forAll(
        CoreGen.iamName → "roleName",
        arbitrary[PolicyArn] → "policyArn"
      ) { (roleName, policyArn) ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.rolePolicyAttacher _)
          .expects()
          .returningFlow(AttachRolePolicyRequest(roleName, policyArn), roleName)

        val result = async.attachRolePolicy(roleName, policyArn).futureValue
        result shouldBe Done
      }
    }

    "detach managed policies from roles" in {
      forAll(
        CoreGen.iamName → "roleName",
        arbitrary[PolicyArn] → "policyArn"
      ) { (roleName, policyArn) ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.rolePolicyDetacher _)
          .expects()
          .returningFlow(DetachRolePolicyRequest(roleName, policyArn.arnString), roleName)

        val result = async.detachRolePolicy(roleName, policyArn.arnString).futureValue
        result shouldBe Done
      }
    }

    "list all of the managed policies attached to a role" in {
      forAll(CoreGen.iamName → "roleName") { roleName ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        val policies = Gen.resize(20, arbitrary[List[AttachedPolicy]]).reallySample

        (streaming.attachedRolePolicyLister _)
          .expects()
          .returningConcatFlow(ListAttachedRolePoliciesRequest(roleName), policies)

        val result = async.listAttachedRolePolicies(roleName).futureValue
        result shouldBe policies
      }
    }

    "list the managed policies attached to a role that match a prefix" in {
      forAll(
        CoreGen.iamName → "roleName",
        arbitrary[Path] → "path"
      ) { (roleName, pathPrefix) ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        val policies = Gen.resize(20, arbitrary[List[AttachedPolicy]]).reallySample

        (streaming.attachedRolePolicyLister _)
          .expects()
          .returningConcatFlow(
            ListAttachedRolePoliciesRequest(roleName, pathPrefix.pathString),
            policies)

        val result = async.listAttachedRolePolicies(roleName, pathPrefix.pathString).futureValue
        result shouldBe policies
      }
    }

    "get the current user" in {
      val streaming = mock[StreamingIdentityManagementClient]("streaming")
      val async = new DefaultAsyncIdentityManagementClient(streaming)
      val user = arbitrarySample[User]

      (streaming.userGetter _)
        .expects()
        .returningFlow(GetUserRequest.currentUser, user)

      val result = async.getCurrentUser().futureValue
      result shouldBe user
    }

    "get a named user" in {
      forAll(
        CoreGen.iamName → "name",
        arbitrary[User] → "user"
      ) { (name, user) ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.userGetter _)
          .expects()
          .returningFlow(GetUserRequest.forUserName(name), user)

        val result = async.getUser(name).futureValue
        result shouldBe user
      }
    }
  }
}
