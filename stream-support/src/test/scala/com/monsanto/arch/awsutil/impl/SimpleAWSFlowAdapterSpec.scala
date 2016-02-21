package com.monsanto.arch.awsutil.impl

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler
import com.monsanto.arch.awsutil.AWSAsyncCall
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpec, Matchers}

class SimpleAWSFlowAdapterSpec extends FreeSpec with MockFactory {
  import Matchers._
  import SimpleAWSFlowAdapterSpec._

  "a SimpleAWSFlowAdapterTest" - {
    "invokes the supplied async call handle" in {
      val asyncCall = mock[AWSAsyncCall[TestRequest.type, TestResult.type]]
      val handler = mock[AsyncHandler[TestRequest.type, TestResult.type]]
      val adapter = new SimpleAWSFlowAdapter(asyncCall)

      (asyncCall.apply _).expects(TestRequest, handler)

      adapter.processRequest(TestRequest, handler)
    }

    "always returns a None token" in {
      val asyncCall = mock[AWSAsyncCall[TestRequest.type, TestResult.type]]
      val adapter = new SimpleAWSFlowAdapter(asyncCall)

      adapter.getToken(TestResult) shouldBe None
    }

    "should pass through a request in withToken" in {
      val asyncCall = mock[AWSAsyncCall[TestRequest.type, TestResult.type]]
      val adapter = new SimpleAWSFlowAdapter(asyncCall)

      adapter.withToken(TestRequest, "foo") shouldBe TestRequest
    }
  }
}

object SimpleAWSFlowAdapterSpec {
  case object TestRequest extends AmazonWebServiceRequest
  case object TestResult
}
