package com.monsanto.arch.awsutil.identitymanagement

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.identitymanagement.{AmazonIdentityManagementAsync, model ⇒ aws}
import com.monsanto.arch.awsutil.identitymanagement.model._
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Samplers.{EnhancedGen, arbitrarySample}
import com.monsanto.arch.awsutil.test_support.{AwsMockUtils, Materialised}
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DefaultStreamingIdentityManagementClientSpec extends FreeSpec with MockFactory with Materialised with AwsMockUtils {
  "the default StreamingIdentityManagementClient provides" - {
    "a role lister" in {
      forAll(maxSize(30)) { (maybePrefix: Option[Path], scalaRoles: List[Role]) ⇒
        val awsRoles = scalaRoles.map { role ⇒
          new aws.Role()
            .withArn(role.arn)
            .withAssumeRolePolicyDocument(role.assumeRolePolicyDocument)
            .withCreateDate(role.created)
            .withPath(role.path)
            .withRoleId(role.id)
            .withRoleName(role.name)
        }
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)
        val prefix = maybePrefix.map(_.pathString).orNull
        val request = maybePrefix.map(p ⇒ ListRolesRequest.withPathPrefix(p.pathString)).getOrElse(ListRolesRequest.allRoles)

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
      forAll(maxSize(30)) { requests: List[CreateRoleRequest] ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)
        val createdRoles = Gen.listOfN(requests.size, arbitrary[Role]).reallySample

        requests.zip(createdRoles).foreach { case (request, role) ⇒
          (iam.createRoleAsync(_: aws.CreateRoleRequest, _: AsyncHandler[aws.CreateRoleRequest,aws.CreateRoleResult]))
            .expects(whereRequest { r ⇒
              r should have (
                'roleName (request.name),
                'path (request.path.orNull),
                'assumeRolePolicyDocument (request.assumeRolePolicy)
              )
              true
            })
            .withAwsSuccess(new aws.CreateRoleResult().withRole(role.toAws))
        }

        val result = Source(requests).via(streaming.roleCreator).runWith(Sink.seq).futureValue
        result shouldBe createdRoles
      }
    }

    "a role deleter" in {
      forAll { roleName: Name ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)

        (iam.deleteRoleAsync(_: aws.DeleteRoleRequest, _: AsyncHandler[aws.DeleteRoleRequest, Void]))
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
      forAll { (roleName: Name, policyArn: PolicyArn) ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)
        val request = AttachRolePolicyRequest(roleName.value, policyArn.arnString)

        (iam.attachRolePolicyAsync(_: aws.AttachRolePolicyRequest, _: AsyncHandler[aws.AttachRolePolicyRequest, Void]))
          .expects(whereRequest { r ⇒
            r should have (
              'roleName (roleName.value),
              'policyArn (policyArn.arnString)
            )
            true
          })
          .withVoidAwsSuccess()

        val result = Source.single(request).via(streaming.rolePolicyAttacher).runWith(Sink.head).futureValue
        result shouldBe roleName.value
      }
    }

    "a role policy detacher" in {
      forAll { (roleName: Name, policyArn: PolicyArn) ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)
        val request = DetachRolePolicyRequest(roleName.value, policyArn.arnString)

        (iam.detachRolePolicyAsync(_: aws.DetachRolePolicyRequest, _: AsyncHandler[aws.DetachRolePolicyRequest, Void]))
          .expects(whereRequest { r ⇒
            r should have (
              'roleName (roleName.value),
              'policyArn (policyArn.arnString)
            )
            true
          })
          .withVoidAwsSuccess()

        val result = Source.single(request).via(streaming.rolePolicyDetacher).runWith(Sink.head).futureValue
        result shouldBe roleName.value
      }
    }

    "an attached role policy lister" in {
      forAll { args: (Name, Option[Path]) ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)
        val request = args match {
          case (Name(name), Some(path)) ⇒ ListAttachedRolePoliciesRequest(name, path.pathString)
          case (Name(name), None) ⇒ ListAttachedRolePoliciesRequest(name)
        }
        val results =
          arbitrarySample[List[(PolicyArn, Name)]](20).map { args ⇒
            AttachedPolicy(args._1.arnString, args._2.value)
          }

        val pages = if (results.isEmpty) List(results) else results.grouped(5).toList

        pages.zipWithIndex.foreach { case (page, i) ⇒
          (iam.listAttachedRolePoliciesAsync(_: aws.ListAttachedRolePoliciesRequest, _: AsyncHandler[aws.ListAttachedRolePoliciesRequest, aws.ListAttachedRolePoliciesResult]))
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
      forAll { args: List[Option[Name]] ⇒
        val iam = mock[AmazonIdentityManagementAsync]("iam")
        val streaming = new DefaultStreamingIdentityManagementClient(iam)
        val requests = args.map(_.map(n ⇒ GetUserRequest.forUserName(n.value)).getOrElse(GetUserRequest.currentUser))

        val users = Gen.resize(20, Gen.listOfN(requests.size, arbitrary[User])).reallySample

        requests.zip(users).foreach { case (request, user) ⇒
          (iam.getUserAsync(_: aws.GetUserRequest, _: AsyncHandler[aws.GetUserRequest,aws.GetUserResult]))
            .expects(whereRequest { r ⇒
              r should have ('userName (request.userName.orNull))
              true
            })
            .withAwsSuccess(new aws.GetUserResult().withUser(user.toAws))
        }

        val result = Source(requests).via(streaming.userGetter).runWith(Sink.seq).futureValue
        result shouldBe users
      }
    }
  }
}
