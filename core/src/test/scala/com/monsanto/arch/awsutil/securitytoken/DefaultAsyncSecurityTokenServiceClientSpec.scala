package com.monsanto.arch.awsutil.securitytoken

import com.monsanto.arch.awsutil.securitytoken.model.AssumeRoleRequest
import com.monsanto.arch.awsutil.test.Samplers._
import com.monsanto.arch.awsutil.{AwsGen, FlowMockUtils, Materialised}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DefaultAsyncSecurityTokenServiceClientSpec extends FreeSpec with MockFactory with FlowMockUtils with Materialised {
  "the asynchronous SecurityTokenService client should" - {
    "assume roles" - {
      "using the simplified two-argument method" in {
        forAll("roleArn", "sessionName") { (roleArn: AwsGen.IAM.RoleArn, sessionName: AwsGen.STS.RoleSessionName) ⇒
          val streaming = mock[StreamingSecurityTokenServiceClient]("streaming")
          val async = new DefaultAsyncSecurityTokenServiceClient(streaming)

          val request = AssumeRoleRequest(roleArn.value, sessionName.value)
          val expectedResult = arbitrarySample[AwsGen.STS.AssumeRoleResultArgs].toResult
          val credentials = expectedResult.credentials

          (streaming.roleAssumer _)
            .expects()
            .returningFlow(request, expectedResult)

          val result = async.assumeRole(roleArn.value, sessionName.value).futureValue
          result shouldBe credentials
        }
      }

      "using the simplified three-argument method" in {
        forAll("roleArn", "sessionName", "externalId") { (roleArn: AwsGen.IAM.RoleArn,
                                                          sessionName: AwsGen.STS.RoleSessionName,
                                                          externalId: AwsGen.STS.ExternalId) ⇒
          val streaming = mock[StreamingSecurityTokenServiceClient]("streaming")
          val async = new DefaultAsyncSecurityTokenServiceClient(streaming)

          val request = AssumeRoleRequest(roleArn.value, sessionName.value, externalId = Some(externalId.value))
          val expectedResult = arbitrarySample[AwsGen.STS.AssumeRoleResultArgs].toResult
          val credentials = expectedResult.credentials

          (streaming.roleAssumer _)
            .expects()
            .returningFlow(request, expectedResult)

          val result = async.assumeRole(roleArn.value, sessionName.value, externalId.value).futureValue
          result shouldBe credentials
        }
      }

      "using the full request method" in {
        forAll { requestArgs: AwsGen.STS.AssumeRoleRequestArgs ⇒
          val streaming = mock[StreamingSecurityTokenServiceClient]("streaming")
          val async = new DefaultAsyncSecurityTokenServiceClient(streaming)

          val request = requestArgs.toRequest
          val expectedResult = AwsGen.STS.AssumeRoleResultArgs.forRequest(requestArgs).reallySample.toResult

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
