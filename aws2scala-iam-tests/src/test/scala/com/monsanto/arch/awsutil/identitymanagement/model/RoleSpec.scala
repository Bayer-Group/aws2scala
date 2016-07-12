package com.monsanto.arch.awsutil.identitymanagement.model

import akka.Done
import akka.stream.Materializer
import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.converters.IamConverters._
import com.monsanto.arch.awsutil.identitymanagement.AsyncIdentityManagementClient
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Materialised
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

import scala.concurrent.Future

class RoleSpec extends FreeSpec with MockFactory with Materialised {
  "a Role can" - {
    "be round-trip" - {
      "from its AWS equivalent" in {
        forAll { role: Role ⇒
          val awsRole = new aws.Role()
            .withArn(role.arn.arnString)
            .withRoleName(role.name)
            .withRoleId(role.id)
            .withPath(role.path.pathString)
            .withAssumeRolePolicyDocument(role.assumeRolePolicyDocument.toJson)
            .withCreateDate(role.created)
          awsRole.asScala.asAws shouldBe awsRole
        }
      }

      "via its AWS equivalent" in {
        forAll { role: Role ⇒
          role.asAws.asScala shouldBe role
        }
      }
    }

    "delete itself" in {
      forAll { role: Role ⇒
        implicit val client = mock[AsyncIdentityManagementClient]("client")

        (client.deleteRole(_: String)(_: Materializer))
          .expects(role.name, materialiser)
          .returning(Future.successful(Done))

        val result = role.delete().futureValue
        result shouldBe Done
      }
    }

    "attach a managed policy" in {
      forAll { (role: Role, policyArn: PolicyArn) ⇒
        implicit val client = mock[AsyncIdentityManagementClient]("client")

        (client.attachRolePolicy(_: String, _: PolicyArn)(_: Materializer))
          .expects(role.name, policyArn, materialiser)
          .returning(Future.successful(Done))

        val result = role.attachPolicy(policyArn).futureValue
        result shouldBe Done
      }
    }

    "detach a managed policy" in {
      forAll { (role: Role, policyArn: PolicyArn) ⇒
        implicit val client = mock[AsyncIdentityManagementClient]("client")

        (client.detachRolePolicy(_: String, _: PolicyArn)(_: Materializer))
          .expects(role.name, policyArn, materialiser)
          .returning(Future.successful(Done))

        val result = role.detachPolicy(policyArn).futureValue
        result shouldBe Done
      }
    }

    "list attached managed policies" - {
      "all of them" in {
        forAll { (role: Role, expected: Seq[AttachedRolePolicy]) ⇒
          implicit val client = mock[AsyncIdentityManagementClient]("client")

          (client.listAttachedRolePolicies(_: String)(_: Materializer))
            .expects(role.name, materialiser)
            .returning(Future.successful(expected))

          val result = role.listAttachedPolicies().futureValue
          result shouldBe expected
        }
      }

      "with a prefix" in {
        forAll { (role: Role, prefix: Path, expected: Seq[AttachedRolePolicy]) ⇒
          implicit val client = mock[AsyncIdentityManagementClient]("client")

          (client.listAttachedRolePolicies(_: String, _: Path)(_: Materializer))
            .expects(role.name, prefix, materialiser)
            .returning(Future.successful(expected))

          val result = role.listAttachedPolicies(prefix).futureValue
          result shouldBe expected
        }
      }
    }
  }
}
