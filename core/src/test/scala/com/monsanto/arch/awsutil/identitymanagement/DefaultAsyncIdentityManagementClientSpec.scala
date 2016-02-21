package com.monsanto.arch.awsutil.identitymanagement

import akka.Done
import com.monsanto.arch.awsutil.identitymanagement.model._
import com.monsanto.arch.awsutil.test.Samplers.arbitrarySample
import com.monsanto.arch.awsutil.{AwsGen, FlowMockUtils, Materialised}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DefaultAsyncIdentityManagementClientSpec extends FreeSpec with MockFactory with Materialised with FlowMockUtils {
  "the default async identity management client should" - {
    "create roles" - {
      "without paths" in {
        forAll { (roleName: AwsGen.IAM.Name, policy: AwsGen.Policy) ⇒
          val streaming = mock[StreamingIdentityManagementClient]("streaming")
          val async = new DefaultAsyncIdentityManagementClient(streaming)

          val role = arbitrarySample[AwsGen.IAM.RoleArgs].toRole

          (streaming.roleCreator _)
            .expects()
            .returningFlow(CreateRoleRequest(roleName.value, policy.toJson, None), role)

          val result = async.createRole(roleName.value, policy.toJson).futureValue
          result shouldBe role
        }
      }

      "with paths" in {
        forAll { (roleName: AwsGen.IAM.Name, policy: AwsGen.Policy, path: AwsGen.IAM.Path) ⇒
          val streaming = mock[StreamingIdentityManagementClient]("streaming")
          val async = new DefaultAsyncIdentityManagementClient(streaming)

          val role = arbitrarySample[AwsGen.IAM.RoleArgs].toRole

          (streaming.roleCreator _)
            .expects()
            .returningFlow(CreateRoleRequest(roleName.value, policy.toJson, Some(path.value)), role)

          val result = async.createRole(roleName.value, policy.toJson, path.value).futureValue
          result shouldBe role
        }
      }
    }

    "delete roles" in {
      forAll { roleName: AwsGen.IAM.Name ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.roleDeleter _)
          .expects()
          .returningFlow(roleName.value, roleName.value)

        val result = async.deleteRole(roleName.value).futureValue
        result shouldBe Done
      }
    }

    "list all roles" in {
      val roles = arbitrarySample[List[AwsGen.IAM.RoleArgs]].map(_.toRole)
      val streaming = mock[StreamingIdentityManagementClient]("streaming")
      val async = new DefaultAsyncIdentityManagementClient(streaming)

      (streaming.roleLister _)
        .expects()
        .returningConcatFlow(ListRolesRequest.allRoles, roles)

      val result = async.listRoles().futureValue
      result shouldBe roles
    }

    "list roles matching a prefix" in {
      forAll { prefix: AwsGen.IAM.Path ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        val roles = arbitrarySample[List[AwsGen.IAM.RoleArgs]].map(_.toRole)

        (streaming.roleLister _)
          .expects()
          .returningConcatFlow(ListRolesRequest.withPathPrefix(prefix.value), roles)

        val result = async.listRoles(prefix.value).futureValue
        result shouldBe roles
      }
    }

    "attach managed policies to roles" in {
      forAll { (roleName: AwsGen.IAM.Name, policyArn: AwsGen.IAM.PolicyArn) ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.rolePolicyAttacher _)
          .expects()
          .returningFlow(AttachRolePolicyRequest(roleName.value, policyArn.value), roleName.value)

        val result = async.attachRolePolicy(roleName.value, policyArn.value).futureValue
        result shouldBe Done
      }
    }

    "detach managed policies from roles" in {
      forAll { (roleName: AwsGen.IAM.Name, policyArn: AwsGen.IAM.PolicyArn) ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.rolePolicyDetacher _)
          .expects()
          .returningFlow(DetachRolePolicyRequest(roleName.value, policyArn.value), roleName.value)

        val result = async.detachRolePolicy(roleName.value, policyArn.value).futureValue
        result shouldBe Done
      }
    }

    "list all of the managed policies attached to a role" in {
      forAll { roleName: AwsGen.IAM.Name ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        val policies = arbitrarySample[List[(AwsGen.IAM.PolicyArn, AwsGen.IAM.Name)]].map { case (arn, name) ⇒
          AttachedPolicy(arn.value, name.value)
        }

        (streaming.attachedRolePolicyLister _)
          .expects()
          .returningConcatFlow(ListAttachedRolePoliciesRequest(roleName.value), policies)

        val result = async.listAttachedRolePolicies(roleName.value).futureValue
        result shouldBe policies
      }
    }

    "list the managed policies attached to a role that match a prefix" in {
      forAll { (roleName: AwsGen.IAM.Name, pathPrefix: AwsGen.IAM.Path) ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        val policies = arbitrarySample[List[(AwsGen.IAM.PolicyArn, AwsGen.IAM.Name)]].map { case (arn, name) ⇒
          AttachedPolicy(arn.value, name.value)
        }

        (streaming.attachedRolePolicyLister _)
          .expects()
          .returningConcatFlow(
            ListAttachedRolePoliciesRequest(roleName.value, pathPrefix.value),
            policies)

        val result = async.listAttachedRolePolicies(roleName.value, pathPrefix.value).futureValue
        result shouldBe policies
      }
    }

    "get the current user" in {
      val streaming = mock[StreamingIdentityManagementClient]("streaming")
      val async = new DefaultAsyncIdentityManagementClient(streaming)
      val user = arbitrarySample[AwsGen.IAM.UserArgs].toUser

      (streaming.userGetter _)
        .expects()
        .returningFlow(GetUserRequest.currentUser, user)

      val result = async.getCurrentUser().futureValue
      result shouldBe user
    }

    "get a named user" in {
      forAll { name: AwsGen.IAM.Name ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)
        val user = arbitrarySample[AwsGen.IAM.UserArgs].toUser

        (streaming.userGetter _)
          .expects()
          .returningFlow(GetUserRequest.forUserName(name.value), user)

        val result = async.getUser(name.value).futureValue
        result shouldBe user
      }
    }
  }
}
