package com.monsanto.arch.awsutil.securitytoken

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceAsync
import com.amazonaws.services.securitytoken.model.{AssumeRoleRequest, AssumeRoleResult}
import com.monsanto.arch.awsutil.test.Samplers.EnhancedGen
import com.monsanto.arch.awsutil.{AwsGen, AwsMockUtils, Materialised}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DefaultStreamingSecurityTokenServiceClientSpec extends FreeSpec with MockFactory with Materialised with AwsMockUtils {
  "the streaming security token service client should" - {
    "assume roles" in {
      forAll { requestArgs: AwsGen.STS.AssumeRoleRequestArgs ⇒
        val aws = mock[AWSSecurityTokenServiceAsync]("aws")
        val streaming = new DefaultStreamingSecurityTokenServiceClient(aws)
        val request = requestArgs.toRequest
        val expectedResult = AwsGen.STS.AssumeRoleResultArgs.forRequest(requestArgs).reallySample.toResult

        (aws.assumeRoleAsync(_: AssumeRoleRequest, _: AsyncHandler[AssumeRoleRequest,AssumeRoleResult]))
          .expects(whereRequest { r ⇒
            r shouldBe request.toAws
            true
          })
          .withAwsSuccess(expectedResult.toAws)

        val result = Source.single(request).via(streaming.roleAssumer).runWith(Sink.head).futureValue
        result shouldBe expectedResult
      }
    }
  }
}
