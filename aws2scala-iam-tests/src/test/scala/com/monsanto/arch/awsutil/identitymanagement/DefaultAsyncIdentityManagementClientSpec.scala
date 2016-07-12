package com.monsanto.arch.awsutil.identitymanagement

import akka.Done
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.identitymanagement.model._
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Samplers.{EnhancedGen, arbitrarySample}
import com.monsanto.arch.awsutil.test_support.{FlowMockUtils, Materialised}
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.{CoreGen, IamGen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
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
          .returningConcatFlow(ListRolesRequest.withPrefix(prefix), roles)

        val result = async.listRoles(prefix).futureValue
        result shouldBe roles
      }
    }

    "get a specified role" in {
      forAll { role: Role ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.roleGetter _)
          .expects()
          .returningFlow(role.name, role)

        val result = async.getRole(role.name).futureValue
        result shouldBe role
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
          .returningFlow(DetachRolePolicyRequest(roleName, policyArn), roleName)

        val result = async.detachRolePolicy(roleName, policyArn).futureValue
        result shouldBe Done
      }
    }

    "list all of the managed policies attached to a role" in {
      forAll(CoreGen.iamName → "roleName") { roleName ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        val policies = Gen.resize(20, arbitrary[List[AttachedRolePolicy]]).reallySample

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

        val policies = Gen.resize(20, arbitrary[List[AttachedRolePolicy]]).reallySample

        (streaming.attachedRolePolicyLister _)
          .expects()
          .returningConcatFlow(
            ListAttachedRolePoliciesRequest(roleName, pathPrefix),
            policies)

        val result = async.listAttachedRolePolicies(roleName, pathPrefix).futureValue
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

    "create new managed policies" - {
      "using a name and policy" in {
        forAll(
          CoreGen.iamName → "name",
          arbitrary[Policy] → "document",
          arbitrary[ManagedPolicy] → "managedPolicy"
        ) { (name, document, managedPolicy) ⇒
          val streaming = mock[StreamingIdentityManagementClient]("streaming")
          val async = new DefaultAsyncIdentityManagementClient(streaming)

          (streaming.policyCreator _)
            .expects()
            .returningFlow(CreatePolicyRequest(name, document, None, Path.empty), managedPolicy)

          val result = async.createPolicy(name, document).futureValue
          result shouldBe managedPolicy
        }
      }

      "using a name, policy, and description" in {
        forAll(
          CoreGen.iamName → "name",
          arbitrary[Policy] → "document",
          arbitrary[String] → "description",
          arbitrary[ManagedPolicy] → "managedPolicy"
        ) { (name, document, description, managedPolicy) ⇒
          val streaming = mock[StreamingIdentityManagementClient]("streaming")
          val async = new DefaultAsyncIdentityManagementClient(streaming)

          (streaming.policyCreator _)
            .expects()
            .returningFlow(CreatePolicyRequest(name, document, Some(description), Path.empty), managedPolicy)

          val result = async.createPolicy(name, document, description).futureValue
          result shouldBe managedPolicy
        }
      }

      "using name, policy, description, and path" in {
        forAll(
          CoreGen.iamName → "name",
          arbitrary[Policy] → "document",
          arbitrary[String] → "description",
          arbitrary[Path] → "path",
          arbitrary[ManagedPolicy] → "managedPolicy"
        ) { (name, document, description, path, managedPolicy) ⇒
          val streaming = mock[StreamingIdentityManagementClient]("streaming")
          val async = new DefaultAsyncIdentityManagementClient(streaming)

          (streaming.policyCreator _)
            .expects()
            .returningFlow(CreatePolicyRequest(name, document, Some(description), path), managedPolicy)

          val result = async.createPolicy(name, document, description, path).futureValue
          result shouldBe managedPolicy
        }
      }
    }

    "delete managed policies" in {
      forAll { policyArn: PolicyArn ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.policyDeleter _)
          .expects()
          .returningFlow(policyArn, policyArn)

        val result = async.deletePolicy(policyArn).futureValue
        result shouldBe Done
      }
    }

    "get managed policies" in {
      forAll { (policyArn: PolicyArn, policy: ManagedPolicy) ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.policyGetter _)
          .expects()
          .returningFlow(policyArn, policy)

        val result = async.getPolicy(policyArn).futureValue
        result shouldBe policy
      }
    }

    "list policies" - {
      "all of them" in {
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)
        val policies = List.empty[ManagedPolicy]

        (streaming.policyLister _)
          .expects()
          .returningConcatFlow(ListPoliciesRequest.allPolicies, policies)

        val result = async.listPolicies().futureValue
        result shouldBe policies
      }

      "with a prefix" in {
        forAll { prefix: Path ⇒
          val streaming = mock[StreamingIdentityManagementClient]("streaming")
          val async = new DefaultAsyncIdentityManagementClient(streaming)
          val policies = List.empty[ManagedPolicy]

          (streaming.policyLister _)
            .expects()
            .returningConcatFlow(ListPoliciesRequest.withPrefix(prefix), policies)

          val result = async.listPolicies(prefix).futureValue
          result shouldBe policies
        }
      }

      "with an arbitrary filter" in {
        forAll { request: ListPoliciesRequest ⇒
          val streaming = mock[StreamingIdentityManagementClient]("streaming")
          val async = new DefaultAsyncIdentityManagementClient(streaming)
          val policies = List.empty[ManagedPolicy]

          (streaming.policyLister _)
            .expects()
            .returningConcatFlow(request, policies)

          val result = async.listPolicies(request).futureValue
          result shouldBe policies
        }
      }

      "only local policies" in {
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)
        val policies = List.empty[ManagedPolicy]

        (streaming.policyLister _)
          .expects()
          .returningConcatFlow(ListPoliciesRequest.localPolicies, policies)

        val result = async.listLocalPolicies().futureValue
        result shouldBe policies
      }
    }

    "create new policy versions" in {
      forAll { (arn: PolicyArn, document: Policy, setAsDefault: Boolean, policyVersion: ManagedPolicyVersion) ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.policyVersionCreator _)
          .expects()
          .returningFlow(CreatePolicyVersionRequest(arn, document, setAsDefault), policyVersion)

        val result = async.createPolicyVersion(arn, document, setAsDefault).futureValue
        result shouldBe policyVersion
      }
    }

    "delete policy versions" in {
      forAll { request: DeletePolicyVersionRequest ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.policyVersionDeleter _)
          .expects()
          .returningFlow(request, request.arn)

        val result = async.deletePolicyVersion(request.arn, request.versionId).futureValue
        result shouldBe Done
      }
    }

    "get policy versions" in {
      forAll { (request: GetPolicyVersionRequest, policyVersion: ManagedPolicyVersion) ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.policyVersionGetter _)
          .expects()
          .returningFlow(request, policyVersion)

        val result = async.getPolicyVersion(request.arn, request.versionId).futureValue
        result shouldBe policyVersion
      }
    }

    "list policy versions" in {
      implicit val arbPolicyVersions: Arbitrary[List[ManagedPolicyVersion]] =
        Arbitrary(Gen.resize(10, Gen.nonEmptyListOf(arbitrary[ManagedPolicyVersion])))
      forAll { (arn: PolicyArn, versions: List[ManagedPolicyVersion]) ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.policyVersionLister _)
          .expects()
          .returningConcatFlow(arn, versions)

        val result = async.listPolicyVersions(arn).futureValue
        result shouldBe versions
      }
    }

    "set default policy versions" in {
      forAll { request: SetDefaultPolicyVersionRequest ⇒
        val streaming = mock[StreamingIdentityManagementClient]("streaming")
        val async = new DefaultAsyncIdentityManagementClient(streaming)

        (streaming.defaultPolicyVersionSetter _)
          .expects()
          .returningFlow(request, request.arn)

        val result = async.setDefaultPolicyVersion(request.arn, request.versionId).futureValue
        result shouldBe Done
      }
    }
  }
}
