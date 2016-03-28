package com.monsanto.arch

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler
import java.util.concurrent.{Future => JFuture}

package object awsutil {
  /** Handy type alias for an asynchronous AWS call. */
  type AWSAsyncCall[Request <: AmazonWebServiceRequest, Result] = (Request, AsyncHandler[Request,Result]) => JFuture[Result]
}
