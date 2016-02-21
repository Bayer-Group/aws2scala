package com.monsanto.arch.awsutil

import java.util.concurrent.{Future ⇒ JFuture, TimeUnit}

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler
import com.monsanto.arch.awsutil.impl.Macros

import scala.language.experimental.macros

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
  def nextTokenFlowAdapter[Request <: AmazonWebServiceRequest, Result]
        (asyncCall: AWSAsyncCall[Request,Result]): AWSFlowAdapter[Request,Result] =
    macro Macros.nextTokenFlowAdapter[Request, Result]

  /** Generates a flow adapter from an async method handle.  The implementation depends on the fact that
    * paging is handled by calling `Request.withMarker` and `Response.getNextMarker`.
    */
  def nextMarkerFlowAdapter[Request <: AmazonWebServiceRequest, Result]
        (asyncCall: AWSAsyncCall[Request,Result]): AWSFlowAdapter[Request,Result] =
    macro Macros.nextMarkerFlowAdapter[Request, Result]

  /** Generates a flow adapter from an async method handle.  The implementation depends on the fact that
    * paging is handled by calling `Request.withMarker` and `Response.getMarker`.
    */
  def markerFlowAdapter[Request <: AmazonWebServiceRequest, Result]
        (asyncCall: AWSAsyncCall[Request,Result]): AWSFlowAdapter[Request,Result] =
    macro Macros.markerFlowAdapter[Request, Result]

  /** A utility for wrapping an asynchronous AWS call that returns void.  This utility will make the result of the call
    * be the request that was passed in.  This is necessary because Akka streams do not allow null values to passed
    * through.
    */
  def devoid[T <: AmazonWebServiceRequest](voidCall: AWSAsyncCall[T,Void]): AWSAsyncCall[T,T] = { (request: T, handler: AsyncHandler[T,T]) =>
    val voidHandler = new AsyncHandler[T, Void] {
      override def onSuccess(request: T, result: Void): Unit = handler.onSuccess(request, request)
      override def onError(exception: Exception): Unit = handler.onError(exception)
    }

    val voidFuture = voidCall(request, voidHandler)

    new JFuture[T] {
      override def get(): T = {
        voidFuture.get()
        request
      }

      override def get(timeout: Long, unit: TimeUnit): T = {
        voidFuture.get(timeout, unit)
        request
      }

      override def cancel(mayInterruptIfRunning: Boolean): Boolean = voidFuture.cancel(mayInterruptIfRunning)

      override def isCancelled: Boolean = voidFuture.isCancelled

      override def isDone: Boolean = voidFuture.isDone
    }
  }
}
