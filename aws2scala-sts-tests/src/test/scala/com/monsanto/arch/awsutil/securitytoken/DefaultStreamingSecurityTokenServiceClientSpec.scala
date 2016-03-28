package com.monsanto.arch.awsutil.securitytoken

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.securitytoken.{AWSSecurityTokenServiceAsync, model ⇒ aws}
import com.monsanto.arch.awsutil.securitytoken.model.AssumeRoleRequest
import com.monsanto.arch.awsutil.securitytoken.model.AwsConverters._
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Samplers.EnhancedGen
import com.monsanto.arch.awsutil.test_support.{AwsMockUtils, Materialised}
import com.monsanto.arch.awsutil.testkit.StsGen
import com.monsanto.arch.awsutil.testkit.StsScalaCheckImplicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DefaultStreamingSecurityTokenServiceClientSpec extends FreeSpec with MockFactory with Materialised with AwsMockUtils {
  "the streaming security token service client should" - {
    "assume roles" in {
      forAll { request: AssumeRoleRequest ⇒
        val sts = mock[AWSSecurityTokenServiceAsync]("sts")
        val streaming = new DefaultStreamingSecurityTokenServiceClient(sts)
        val expectedResult = StsGen.resultFor(request).reallySample

        (sts.assumeRoleAsync(_: aws.AssumeRoleRequest, _: AsyncHandler[aws.AssumeRoleRequest,aws.AssumeRoleResult]))
          .expects(whereRequest { r ⇒
            r shouldBe request.asAws
            true
          })
          .withAwsSuccess(expectedResult.asAws)

        val result = Source.single(request).via(streaming.roleAssumer).runWith(Sink.head).futureValue
        result shouldBe expectedResult
      }
    }
  }
}
