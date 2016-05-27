package com.monsanto.arch.awsutil.securitytoken

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.sns.model.AuthorizationErrorException
import com.monsanto.arch.awsutil.auth.policy.PolicyDSL._
import com.monsanto.arch.awsutil.auth.policy.Principal
import com.monsanto.arch.awsutil.auth.policy.action.SecurityTokenServiceAction
import com.monsanto.arch.awsutil.identitymanagement.IdentityManagement
import com.monsanto.arch.awsutil.identitymanagement.model._
import com.monsanto.arch.awsutil.s3.S3
import com.monsanto.arch.awsutil.securitytoken.model.{AssumeRoleRequest, Credentials}
import com.monsanto.arch.awsutil.sns.SNS
import com.monsanto.arch.awsutil.test_support.AwsScalaFutures._
import com.monsanto.arch.awsutil.test_support.{AwsIntegrationSpec, IntegrationCleanup, IntegrationTest}
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.Eventually.eventually

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Promise}

@IntegrationTest
class SecurityTokenServiceIntegrationSpec extends FreeSpec with AwsIntegrationSpec with StrictLogging with IntegrationCleanup {
  private val iam = awsClient.streaming(IdentityManagement)
  private val async = awsClient.async(SecurityTokenService)

  private val testPathPrefix = "/aws2scala-it-sts/"
  private val testPath = Path.fromPathString(s"$testPathPrefix$testId/")
  private val testRoleName = s"STSTestRole-$testId"
  private val testRolePolicyArn = PolicyArn.fromArnString("arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess")
  private var testRole: Role = _
  private var credentials: Credentials = _

  "the Security Token Service client" - {
    "can assume a role" in {
      val credentialsPromise = Promise[Credentials]

      implicit val patienceConfig = Eventually.PatienceConfig(15.seconds, 500.milliseconds)

      eventually {
        logger.debug("Attempting to assume role…")
        val result = async.assumeRole(AssumeRoleRequest(testRole.arn.arnString, "aws2scala-it-sts")).futureValue
        logger.info(s"Assumed role with ID ${result.assumedRoleUser.assumedRoleId}")
        credentialsPromise.success(result.credentials)
      }

      credentials = credentialsPromise.future.futureValue
    }

    "can use the credentials" - {
      "to not list SNS topics" in {
        val sns = awsClient.withCredentialsProvider(credentials).async(SNS)
        an [AuthorizationErrorException] shouldBe thrownBy {
          Await.result(sns.listTopics(), 3.seconds)
        }
      }

      "to list buckets" in {
        val s3 = awsClient.withCredentialsProvider(credentials).async(S3)
        s3.listBuckets().futureValue
      }
    }

    behave like cleanupIAMRoles(testPathPrefix)
  }

  override protected def beforeAll() = {
    super.beforeAll()
    testRole =
      Source.single(GetUserRequest.currentUser)
        .via(iam.userGetter)
        .map(makeCreateRoleRequest)
        .via(iam.roleCreator)
        .flatMapConcat { role ⇒
          Source.single(AttachRolePolicyRequest(role.name, testRolePolicyArn))
            .via(iam.rolePolicyAttacher)
            .map(_ ⇒ role)
        }
        .runWith(Sink.head)
        .futureValue
    logger.info(s"Created STS test role ${testRole.name} at ${testRole.arn}")
  }

  override protected def afterAll() = {
    try {
      val deletedRole =
        Source.single(DetachRolePolicyRequest(testRole.name, testRolePolicyArn.arnString))
          .via(iam.rolePolicyDetacher)
          .via(iam.roleDeleter)
          .runWith(Sink.head)
          .futureValue
      logger.info(s"Deleted STS test role $deletedRole")
    } finally super.afterAll()
  }

  private def makeCreateRoleRequest(user: User): CreateRoleRequest = {
    val assumeRolePolicy =
      policy (
        statements (
          allow (
            principals(Principal.iamUser(UserArn.fromArnString(user.arn))),
            actions(SecurityTokenServiceAction.AssumeRole)
          )
        )
      )
    CreateRoleRequest(testRoleName, assumeRolePolicy, Some(testPath))
  }
}
