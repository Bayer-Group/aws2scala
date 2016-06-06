package com.monsanto.arch.awsutil.identitymanagement

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.identitymanagement.{AmazonIdentityManagementAsync, model ⇒ aws}
import com.monsanto.arch.awsutil.converters.IamConverters._
import com.monsanto.arch.awsutil.identitymanagement.model._
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Samplers.EnhancedGen
import com.monsanto.arch.awsutil.test_support.{AwsMockUtils, Materialised}
import com.monsanto.arch.awsutil.testkit.CoreGen
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DefaultStreamingIdentityManagementClientSpec extends FreeSpec with MockFactory with Materialised with AwsMockUtils with AwsMatcherSupport {
  "the default StreamingIdentityManagementClient provides" - {
    "a role lister" in {
      forAll(maxSize(30)) { (maybePrefix: Option[Path], scalaRoles: List[Role]) ⇒
        val awsRoles = scalaRoles.map(_.asAws)
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)
        val prefix = maybePrefix.map(_.pathString).orNull
        val request = maybePrefix.map(p ⇒ ListRolesRequest.withPrefix(p)).getOrElse(ListRolesRequest.allRoles)

        val pages = if (awsRoles.isEmpty) List(awsRoles) else awsRoles.grouped(5).toList

        pages.zipWithIndex.foreach { case (page, i) ⇒
          (iam.listRolesAsync(_: aws.ListRolesRequest, _: AsyncHandler[aws.ListRolesRequest, aws.ListRolesResult]))
            .expects(whereRequest { r ⇒
              val marker = if (i == 0) null else i.toString
              r should have (
                'pathPrefix (prefix),
                'marker (marker)
              )
              true
            })
            .withAwsSuccess {
              val result = new aws.ListRolesResult().withRoles(page: _*)
              val next = i + 1
              if (next != pages.size) {
                result.setIsTruncated(true)
                result.setMarker(next.toString)
              }
              result
            }
        }

        val result = Source.single(request).via(streaming.roleLister).runWith(Sink.seq).futureValue
        result shouldBe scalaRoles
      }
    }

    "a role creator" in {
      forAll(maxSize(20)) { requestsWithRoles: List[(CreateRoleRequest,Role)] ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)
        val (requests, createdRoles) = requestsWithRoles.unzip

        requests.zip(createdRoles).foreach { case (request, role) ⇒
          (iam.createRoleAsync(_: aws.CreateRoleRequest, _: AsyncHandler[aws.CreateRoleRequest,aws.CreateRoleResult]))
            .expects(whereRequest { r ⇒
              r should have (
                'roleName (request.name),
                'path (request.path.map(_.pathString).orNull),
                'assumeRolePolicyDocument (request.assumeRolePolicy.toJson)
              )
              true
            })
            .withAwsSuccess(new aws.CreateRoleResult().withRole(role.asAws))
        }

        val result = Source(requests).via(streaming.roleCreator).runWith(Sink.seq).futureValue
        result shouldBe createdRoles
      }
    }

    "a role deleter" in {
      forAll(CoreGen.iamName) { roleName ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)

        (iam.deleteRoleAsync(_: aws.DeleteRoleRequest, _: AsyncHandler[aws.DeleteRoleRequest, aws.DeleteRoleResult]))
          .expects(whereRequest { r ⇒
            r should have ('roleName (roleName))
            true
          })
          .withVoidAwsSuccess()

        val result = Source.single(roleName).via(streaming.roleDeleter).runWith(Sink.head).futureValue
        result shouldBe roleName
      }
    }

    "a role policy attacher" in {
      forAll(
        CoreGen.iamName → "roleName",
        arbitrary[PolicyArn] → "policyArn"
      ) { (roleName, policyArn) ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)
        val request = AttachRolePolicyRequest(roleName, policyArn)

        (iam.attachRolePolicyAsync(_: aws.AttachRolePolicyRequest, _: AsyncHandler[aws.AttachRolePolicyRequest, aws.AttachRolePolicyResult]))
          .expects(whereRequest { r ⇒
            r should have (
              'roleName (roleName),
              'policyArn (policyArn.arnString)
            )
            true
          })
          .withVoidAwsSuccess()

        val result = Source.single(request).via(streaming.rolePolicyAttacher).runWith(Sink.head).futureValue
        result shouldBe roleName
      }
    }

    "a role policy detacher" in {
      forAll(
        CoreGen.iamName → "roleName",
        arbitrary[PolicyArn] → "policyArn"
      ) { (roleName, policyArn) ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)
        val request = DetachRolePolicyRequest(roleName, policyArn)

        (iam.detachRolePolicyAsync(_: aws.DetachRolePolicyRequest, _: AsyncHandler[aws.DetachRolePolicyRequest, aws.DetachRolePolicyResult]))
          .expects(whereRequest { r ⇒
            r should have (
              'roleName (roleName),
              'policyArn (policyArn.arnString)
            )
            true
          })
          .withVoidAwsSuccess()

        val result = Source.single(request).via(streaming.rolePolicyDetacher).runWith(Sink.head).futureValue
        result shouldBe roleName
      }
    }

    "an attached role policy lister" in {
      forAll { request: ListAttachedRolePoliciesRequest ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)
        val results = Gen.resize(20, arbitrary[List[AttachedPolicy]]).reallySample

        val pages = if (results.isEmpty) List(results) else results.grouped(5).toList

        pages.zipWithIndex.foreach { case (page, i) ⇒
          (iam.listAttachedRolePoliciesAsync(_: aws.ListAttachedRolePoliciesRequest, _: AsyncHandler[aws.ListAttachedRolePoliciesRequest, aws.ListAttachedRolePoliciesResult]))
            .expects(whereRequest { r ⇒
              val marker = if (i == 0) null else i.toString
              r should have(
                'roleName (request.roleName),
                'pathPrefix (request.prefix.map(_.pathString).orNull),
                'marker (marker)
              )
              true
            })
            .withAwsSuccess {
              val policies = page.map(_.asAws)
              val result = new aws.ListAttachedRolePoliciesResult().withAttachedPolicies(policies: _*)
              val next = i + 1
              if (next != pages.size) {
                result.setIsTruncated(true)
                result.setMarker(next.toString)
              }
              result
            }
        }

        val result = Source.single(request).via(streaming.attachedRolePolicyLister).runWith(Sink.seq).futureValue
        result shouldBe results
      }
    }

    "a user getter" in {
      forAll { requests: List[GetUserRequest] ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)

        val users = Gen.resize(20, Gen.listOfN(requests.size, arbitrary[User])).reallySample

        requests.zip(users).foreach { case (request, user) ⇒
          (iam.getUserAsync(_: aws.GetUserRequest, _: AsyncHandler[aws.GetUserRequest,aws.GetUserResult]))
            .expects(whereRequest { r ⇒
              r should have ('userName (request.userName.orNull))
              true
            })
            .withAwsSuccess(new aws.GetUserResult().withUser(user.asAws))
        }

        val result = Source(requests).via(streaming.userGetter).runWith(Sink.seq).futureValue
        result shouldBe users
      }
    }

    "a policy creator" in {
      forAll { (request: CreatePolicyRequest, policy: ManagedPolicy) ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)

        (iam.createPolicyAsync(_: aws.CreatePolicyRequest, _: AsyncHandler[aws.CreatePolicyRequest, aws.CreatePolicyResult]))
          .expects(whereRequest { r ⇒
            r.asScala shouldBe request
            true
          })
          .withAwsSuccess(new aws.CreatePolicyResult().withPolicy(policy.asAws))

        val result = Source.single(request).via(streaming.policyCreator).runWith(Sink.head).futureValue
        result shouldBe policy
      }
    }

    "a policy deleter" in {
      forAll { policyArn: PolicyArn ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)

        (iam.deletePolicyAsync(_: aws.DeletePolicyRequest, _: AsyncHandler[aws.DeletePolicyRequest, aws.DeletePolicyResult]))
          .expects(whereRequest { r ⇒
            r should have (
              'PolicyArn (policyArn.arnString)
            )
            true
          })
          .withAwsSuccess(new aws.DeletePolicyResult)

        val result = Source.single(policyArn).via(streaming.policyDeleter).runWith(Sink.head).futureValue
        result shouldBe policyArn
      }
    }

    "a policy getter" in {
      forAll { (policyArn: PolicyArn, policy: ManagedPolicy) ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)

        (iam.getPolicyAsync(_: aws.GetPolicyRequest, _: AsyncHandler[aws.GetPolicyRequest, aws.GetPolicyResult]))
          .expects(whereRequest { r ⇒
            r should have (
              'PolicyArn (policyArn.arnString)
            )
            true
          })
          .withAwsSuccess(new aws.GetPolicyResult().withPolicy(policy.asAws))

        val result = Source.single(policyArn).via(streaming.policyGetter).runWith(Sink.head).futureValue
        result shouldBe policy
      }
    }

    "a managed policy lister" in {
      forAll { request: ListPoliciesRequest ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)

        val policies = Gen.resize(20, Gen.listOf(arbitrary[ManagedPolicy])).reallySample
        val pages = if (policies.isEmpty) List(policies) else policies.grouped(5).toList

        pages.zipWithIndex.foreach { case (page, i) ⇒
          (iam.listPoliciesAsync(_: aws.ListPoliciesRequest, _: AsyncHandler[aws.ListPoliciesRequest, aws.ListPoliciesResult]))
            .expects(whereRequest { r ⇒
              val marker = if (i == 0) null else i.toString

              r should have(
                'Marker (marker),
                onlyAttached (request.onlyAttached),
                'PathPrefix (request.prefix.pathString),
                'Scope (request.scope.name)
              )
              true
            })
            .withAwsSuccess {
              val policies = page.map(_.asAws)
              val result = new aws.ListPoliciesResult().withPolicies(policies: _*)
              val next = i + 1
              if (next != pages.size) {
                result.setIsTruncated(true)
                result.setMarker(next.toString)
              }
              result
            }
        }

        val result = Source.single(request).via(streaming.policyLister).runWith(Sink.seq).futureValue
        result shouldBe policies
      }
    }
  }
}
