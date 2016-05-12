package com.monsanto.arch.awsutil.securitytoken

import com.monsanto.arch.awsutil.identitymanagement.model.RoleArn
import com.monsanto.arch.awsutil.securitytoken.model.AssumeRoleRequest
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Samplers._
import com.monsanto.arch.awsutil.test_support.{FlowMockUtils, Materialised}
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.StsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.{CoreGen, StsGen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DefaultAsyncSecurityTokenServiceClientSpec extends FreeSpec with MockFactory with FlowMockUtils with Materialised {
  "the asynchronous SecurityTokenService client should" - {
    "assume roles" - {
      "using the simplified two-argument method" in {
        forAll(
          arbitrary[RoleArn].map(_.arnString) → "roleArn",
          CoreGen.assumedRoleSessionName → "sessionName"
        ) { (roleArn, sessionName) ⇒
          val streaming = mock[StreamingSecurityTokenServiceClient]("streaming")
          val async = new DefaultAsyncSecurityTokenServiceClient(streaming)

          val request = AssumeRoleRequest(roleArn, sessionName)
          val expectedResult = StsGen.resultFor(request).reallySample
          val credentials = expectedResult.credentials

          (streaming.roleAssumer _)
            .expects()
            .returningFlow(request, expectedResult)

          val result = async.assumeRole(roleArn, sessionName).futureValue
          result shouldBe credentials
        }
      }

      "using the simplified three-argument method" in {
        forAll(
          arbitrary[RoleArn].map(_.arnString) → "roleArn",
          CoreGen.assumedRoleSessionName → "sessionName",
          StsGen.externalId → "externalId"
        ) { (roleArn, sessionName, externalId) ⇒
          val streaming = mock[StreamingSecurityTokenServiceClient]("streaming")
          val async = new DefaultAsyncSecurityTokenServiceClient(streaming)

          val request = AssumeRoleRequest(roleArn, sessionName, externalId = Some(externalId))
          val expectedResult = StsGen.resultFor(request).reallySample
          val credentials = expectedResult.credentials

          (streaming.roleAssumer _)
            .expects()
            .returningFlow(request, expectedResult)

          val result = async.assumeRole(roleArn, sessionName, externalId).futureValue
          result shouldBe credentials
        }
      }

      "using the full request method" in {
        forAll { request: AssumeRoleRequest ⇒
          val streaming = mock[StreamingSecurityTokenServiceClient]("streaming")
          val async = new DefaultAsyncSecurityTokenServiceClient(streaming)

          val expectedResult = StsGen.resultFor(request).reallySample

          (streaming.roleAssumer _)
            .expects()
            .returningFlow(request, expectedResult)

          val result = async.assumeRole(request).futureValue
          result shouldBe expectedResult
        }
      }
    }
  }
}
