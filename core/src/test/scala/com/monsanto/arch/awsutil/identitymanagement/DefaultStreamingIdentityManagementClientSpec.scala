package com.monsanto.arch.awsutil.identitymanagement

import akka.stream.scaladsl.{Sink, Source}

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementAsync
import com.amazonaws.services.identitymanagement.model.{AttachRolePolicyRequest ⇒ AwsAttachRolePolicyRequest, DetachRolePolicyRequest ⇒ AwsDetachRolePolicyRequest, GetUserRequest ⇒ AwsGetUserRequest, ListAttachedRolePoliciesRequest ⇒ AwsListAttachedRolePoliciesRequest, ListRolesRequest ⇒ AwsListRolesRequest, _}
import com.monsanto.arch.awsutil.identitymanagement.model.GetUserRequest
import com.monsanto.arch.awsutil.identitymanagement.model.ListAttachedRolePoliciesRequest
import com.monsanto.arch.awsutil.identitymanagement.model.ListRolesRequest
import com.monsanto.arch.awsutil.identitymanagement.model.{AttachRolePolicyRequest, AttachedPolicy, DetachRolePolicyRequest}
import com.monsanto.arch.awsutil.test.Samplers._
import com.monsanto.arch.awsutil.{AwsGen, AwsMockUtils, Materialised}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DefaultStreamingIdentityManagementClientSpec extends FreeSpec with MockFactory with Materialised with AwsMockUtils {
  "the default StreamingIdentityManagementClient provides" - {
    "a role lister" in {
      forAll { (maybePrefix: Option[AwsGen.IAM.Path], roleArgs: List[AwsGen.IAM.RoleArgs]) ⇒
        val scalaRoles = roleArgs.map(_.toRole)
        val awsRoles = scalaRoles.map(_.toAws)
        val aws = mock[AmazonIdentityManagementAsync]("aws")
        val streaming = new DefaultStreamingIdentityManagementClient(aws)
        val prefix = maybePrefix.map(_.value).orNull
        val request = maybePrefix.map(p ⇒ ListRolesRequest.withPathPrefix(p.value)).getOrElse(ListRolesRequest.allRoles)

        val pages = if (awsRoles.isEmpty) List(awsRoles) else awsRoles.grouped(5).toList

        pages.zipWithIndex.foreach { case (page, i) ⇒
          (aws.listRolesAsync(_: AwsListRolesRequest, _: AsyncHandler[AwsListRolesRequest, ListRolesResult]))
            .expects(whereRequest { r ⇒
              val marker = if (i == 0) null else i.toString
              r should have (
                'pathPrefix (prefix),
                'marker (marker)
              )
              true
            })
            .withAwsSuccess {
              val result = new ListRolesResult().withRoles(page: _*)
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
      forAll { args: List[AwsGen.IAM.CreateRoleRequestArgs] ⇒
        val aws = mock[AmazonIdentityManagementAsync]("aws")
        val streaming = new DefaultStreamingIdentityManagementClient(aws)
        val requests = args.map(_.toRequest)
        val createdRoles = Gen.listOfN(requests.size, arbitrary[AwsGen.IAM.RoleArgs].map(_.toRole)).reallySample

        requests.zip(createdRoles).foreach { case (request, role) ⇒
          (aws.createRoleAsync(_: CreateRoleRequest, _: AsyncHandler[CreateRoleRequest,CreateRoleResult]))
            .expects(whereRequest { r ⇒
              r should have (
                'roleName (request.name),
                'path (request.path.orNull),
                'assumeRolePolicyDocument (request.assumeRolePolicy)
              )
              true
            })
            .withAwsSuccess(new CreateRoleResult().withRole(role.toAws))
        }

        val result = Source(requests).via(streaming.roleCreator).runWith(Sink.seq).futureValue
        result shouldBe createdRoles
      }
    }

    "a role deleter" in {
      forAll { roleName: AwsGen.IAM.Name ⇒
        val aws = mock[AmazonIdentityManagementAsync]("aws")
        val streaming = new DefaultStreamingIdentityManagementClient(aws)

        (aws.deleteRoleAsync(_: DeleteRoleRequest, _: AsyncHandler[DeleteRoleRequest, Void]))
          .expects(whereRequest { r ⇒
            r should have ('roleName (roleName.value))
            true
          })
          .withVoidAwsSuccess()

        val result = Source.single(roleName.value).via(streaming.roleDeleter).runWith(Sink.head).futureValue
        result shouldBe roleName.value
      }
    }

    "a role policy attacher" in {
      forAll { (roleName: AwsGen.IAM.Name, policyArn: AwsGen.IAM.PolicyArn) ⇒
        val aws = mock[AmazonIdentityManagementAsync]("aws")
        val streaming = new DefaultStreamingIdentityManagementClient(aws)
        val request = AttachRolePolicyRequest(roleName.value, policyArn.value)

        (aws.attachRolePolicyAsync(_: AwsAttachRolePolicyRequest, _: AsyncHandler[AwsAttachRolePolicyRequest, Void]))
          .expects(whereRequest { r ⇒
            r should have (
              'roleName (roleName.value),
              'policyArn (policyArn.value)
            )
            true
          })
          .withVoidAwsSuccess()

        val result = Source.single(request).via(streaming.rolePolicyAttacher).runWith(Sink.head).futureValue
        result shouldBe roleName.value
      }
    }

    "a role policy detacher" in {
      forAll { (roleName: AwsGen.IAM.Name, policyArn: AwsGen.IAM.PolicyArn) ⇒
        val aws = mock[AmazonIdentityManagementAsync]("aws")
        val streaming = new DefaultStreamingIdentityManagementClient(aws)
        val request = DetachRolePolicyRequest(roleName.value, policyArn.value)

        (aws.detachRolePolicyAsync(_: AwsDetachRolePolicyRequest, _: AsyncHandler[AwsDetachRolePolicyRequest, Void]))
          .expects(whereRequest { r ⇒
            r should have (
              'roleName (roleName.value),
              'policyArn (policyArn.value)
            )
            true
          })
          .withVoidAwsSuccess()

        val result = Source.single(request).via(streaming.rolePolicyDetacher).runWith(Sink.head).futureValue
        result shouldBe roleName.value
      }
    }

    "an attached role policy lister" in {
      forAll { args: (AwsGen.IAM.Name, Option[AwsGen.IAM.Path]) ⇒
        val aws = mock[AmazonIdentityManagementAsync]("aws")
        val streaming = new DefaultStreamingIdentityManagementClient(aws)
        val request = args match {
          case (AwsGen.IAM.Name(name), Some(path)) ⇒ ListAttachedRolePoliciesRequest(name, path.value)
          case (AwsGen.IAM.Name(name), None) ⇒ ListAttachedRolePoliciesRequest(name)
        }
        val results =
          arbitrarySample[List[(AwsGen.IAM.PolicyArn, AwsGen.IAM.Name)]].map { args ⇒
            AttachedPolicy(args._1.value, args._2.value)
          }

        val pages = if (results.isEmpty) List(results) else results.grouped(5).toList

        pages.zipWithIndex.foreach { case (page, i) ⇒
          (aws.listAttachedRolePoliciesAsync(_: AwsListAttachedRolePoliciesRequest, _: AsyncHandler[AwsListAttachedRolePoliciesRequest, ListAttachedRolePoliciesResult]))
            .expects(whereRequest { r ⇒
              val marker = if (i == 0) null else i.toString
              r should have(
                'roleName (request.roleName),
                'pathPrefix (request.pathPrefix.orNull),
                'marker (marker)
              )
              true
            })
            .withAwsSuccess {
              val policies = page.map(_.toAws)
              val result = new ListAttachedRolePoliciesResult().withAttachedPolicies(policies: _*)
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
      forAll { args: List[Option[AwsGen.IAM.Name]] ⇒
        val aws = mock[AmazonIdentityManagementAsync]("aws")
        val streaming = new DefaultStreamingIdentityManagementClient(aws)
        val requests = args.map(_.map(n ⇒ GetUserRequest.forUserName(n.value)).getOrElse(GetUserRequest.currentUser))

        val users = Gen.listOfN(requests.size, arbitrary[AwsGen.IAM.UserArgs].map(_.toUser)).reallySample

        requests.zip(users).foreach { case (request, user) ⇒
          (aws.getUserAsync(_: AwsGetUserRequest, _: AsyncHandler[AwsGetUserRequest,GetUserResult]))
            .expects(whereRequest { r ⇒
              r should have ('userName (request.userName.orNull))
              true
            })
            .withAwsSuccess(new GetUserResult().withUser(user.toAws))
        }

        val result = Source(requests).via(streaming.userGetter).runWith(Sink.seq).futureValue
        result shouldBe users
      }
    }
  }
}
