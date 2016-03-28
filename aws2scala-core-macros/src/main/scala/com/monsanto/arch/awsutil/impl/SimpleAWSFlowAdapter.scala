package com.monsanto.arch.awsutil.impl

import com.amazonaws.AmazonWebServiceRequest
import com.monsanto.arch.awsutil.{AWSAsyncCall, AWSFlowAdapter}

/** An [[com.monsanto.arch.awsutil.AWSFlowAdapter AWSFlowAdapter]] for requests that do not have paged results. */
private[awsutil] class SimpleAWSFlowAdapter[Request <: AmazonWebServiceRequest, Result]
      (override val processRequest: AWSAsyncCall[Request,Result]) extends AWSFlowAdapter[Request,Result] {
  override def getToken(result: Result): Option[String] = None
  override def withToken(request: Request, token: String): Request = request
}
