package com.monsanto.arch.awsutil

import java.util.concurrent.{Future ⇒ JFuture, TimeUnit}

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler

/** Type class used for paging through asynchronous AWS requests. */
trait AWSFlowAdapter[Request <: AmazonWebServiceRequest, Result] {
  /** Handy alias for the request processor type. */
  type RequestProcessor = AWSAsyncCall[Request,Result]

  /** Provides a function handle to an asynchronous AWS method. */
  def processRequest: RequestProcessor

  /** Gets the next token from a result, if any. */
  def getToken(result: Result): Option[String]

  /** Returns a request with the given token.  This will only be called if a token is considered necessary. */
  def withToken(request: Request, token: String): Request
}

/** Utilities for creating [[com.monsanto.arch.awsutil.AWSFlowAdapter AWSFlowAdapter]] instances. */
object AWSFlowAdapter {
  /** Generates a flow adapter from an async method handle.  The implementation depends on the fact that
    * paging is handled by calling `Request.withNextToken` and `Response.getNextToken`.
    */
  def nextTokenFlowAdapter[Request <: AmazonWebServiceRequest: TakesNextToken, Result: HasNextToken]
        (asyncCall: AWSAsyncCall[Request,Result]): AWSFlowAdapter[Request,Result] =
    new AWSFlowAdapter[Request,Result] {
      override def processRequest: RequestProcessor = asyncCall
      override def getToken(result: Result): Option[String] = HasNextToken[Result].getToken(result)
      override def withToken(request: Request, token: String): Request = TakesNextToken[Request].withToken(request,token)
    }

  /** Generates a flow adapter from an async method handle.  The implementation depends on the fact that
    * paging is handled by calling `Request.withMarker` and `Response.getNextMarker`.
    */
  def nextMarkerFlowAdapter[Request <: AmazonWebServiceRequest: TakesMarker, Result: HasNextMarker]
        (asyncCall: AWSAsyncCall[Request,Result]): AWSFlowAdapter[Request,Result] =
    new AWSFlowAdapter[Request,Result] {
      override def processRequest: RequestProcessor = asyncCall
      override def getToken(result: Result): Option[String] = HasNextMarker[Result].getNextMarker(result)
      override def withToken(request: Request, token: String): Request = TakesMarker[Request].withMarker(request,token)
    }

  /** Generates a flow adapter from an async method handle.  The implementation depends on the fact that
    * paging is handled by calling `Request.withMarker` and `Response.getMarker`.
    */
  def markerFlowAdapter[Request <: AmazonWebServiceRequest: TakesMarker, Result: HasMarker]
        (asyncCall: AWSAsyncCall[Request,Result]): AWSFlowAdapter[Request,Result] =
    new AWSFlowAdapter[Request,Result] {
      override def processRequest: RequestProcessor = asyncCall
      override def getToken(result: Result): Option[String] = HasMarker[Result].getMarker(result)
      override def withToken(request: Request, token: String): Request = TakesMarker[Request].withMarker(request,token)
    }

  /** A utility for wrapping an asynchronous AWS call that returns a useless POJO.  This utility will make the result
    * of the call be the request that was passed in.
    */
  def returnInput[T <: AmazonWebServiceRequest, U](asyncCall: AWSAsyncCall[T,U]): AWSAsyncCall[T,T] = { (request: T, handler: AsyncHandler[T,T]) ⇒
    val ignoreResultHandler = new AsyncHandler[T, U] {
      override def onSuccess(request: T, result: U): Unit = handler.onSuccess(request, request)
      override def onError(exception: Exception): Unit = handler.onError(exception)
    }

    val javaFuture = asyncCall(request, ignoreResultHandler)

    new JFuture[T] {
      override def get(): T = {
        javaFuture.get()
        request
      }

      override def get(timeout: Long, unit: TimeUnit): T = {
        javaFuture.get(timeout, unit)
        request
      }

      override def cancel(mayInterruptIfRunning: Boolean): Boolean = javaFuture.cancel(mayInterruptIfRunning)

      override def isCancelled: Boolean = javaFuture.isCancelled

      override def isDone: Boolean = javaFuture.isDone
    }
  }
}
