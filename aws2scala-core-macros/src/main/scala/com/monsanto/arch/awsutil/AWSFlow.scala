package com.monsanto.arch.awsutil

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.{AmazonClientException, AmazonWebServiceRequest}
import com.monsanto.arch.awsutil.impl.{AWSGraphStage, Macros, SimpleAWSFlowAdapter}

import scala.language.experimental.macros

/** Utilities for generating Akka streams that interact with AWS. */
object AWSFlow {
  /** A simple flow will return one result for each request. Amazon exceptions will cause the flow to fail. */
  def simple[Request <: AmazonWebServiceRequest, Result]
      (asyncCall: AWSAsyncCall[Request,Result]): Flow[Request, Result, NotUsed] =
    simpleEither(asyncCall).toResult

  /** A simple flow will return one result for each request. */
  def simpleEither[Request <: AmazonWebServiceRequest, Result]
      (asyncCall: AWSAsyncCall[Request,Result]): Flow[Request, Either[AmazonClientException,Result], NotUsed] =
    Flow.fromGraph(new AWSGraphStage(new SimpleAWSFlowAdapter(asyncCall)))

  /** A paged flow may return more than one result for each request, in this case pagination is managed by
    * `Result.getNextToken` and `Request.withNextToken`.  Amazon exceptions will cause the flow to fail.
    */
  def pagedByNextToken[Request <: AmazonWebServiceRequest, Result]
      (asyncCall: AWSAsyncCall[Request,Result]): Flow[Request, Result, NotUsed] =
    macro Macros.pagedByNextToken[Request,Result]

  /** A paged flow may return more than one result for each request, in this case pagination is managed by
    * `Result.getNextToken` and `Request.withNextToken`.
    */
  def pagedByNextTokenEither[Request <: AmazonWebServiceRequest, Result]
      (asyncCall: AWSAsyncCall[Request,Result]): Flow[Request, Either[AmazonClientException,Result], NotUsed] =
    macro Macros.pagedByNextTokenEither[Request,Result]

  /** A paged flow may return more than one result for each request, in this case pagination is managed by
    * `Result.getNextMarker` and `Request.withMarker`.
    */
  def pagedByNextMarker[Request <: AmazonWebServiceRequest, Result]
      (asyncCall: AWSAsyncCall[Request,Result]): Flow[Request, Result, NotUsed] =
    macro Macros.pagedByNextMarker[Request,Result]

  /** A paged flow may return more than one result for each request, in this case pagination is managed by
    * `Result.getNextMarker` and `Request.withMarker`.  Amazon exceptions will cause the flow to fail.
    */
  def pagedByNextMarkerEither[Request <: AmazonWebServiceRequest, Result]
      (asyncCall: AWSAsyncCall[Request,Result]): Flow[Request, Result, NotUsed] =
    macro Macros.pagedByNextMarkerEither[Request,Result]

  /** A paged flow may return more than one result for each request, in this case pagination is managed by
    * `Result.getMarker` and `Request.withMarker`.
    */
  def pagedByMarker[Request <: AmazonWebServiceRequest, Result]
      (asyncCall: AWSAsyncCall[Request,Result]): Flow[Request, Result, NotUsed] =
    macro Macros.pagedByMarker[Request,Result]

  /** A paged flow may return more than one result for each request, in this case pagination is managed by
    * `Result.getMarker` and `Request.withMarker`.  Amazon exceptions will cause the flow to fail.
    */
  def pagedByMarkerEither[Request <: AmazonWebServiceRequest, Result]
      (asyncCall: AWSAsyncCall[Request,Result]): Flow[Request, Result, NotUsed] =
    macro Macros.pagedByMarkerEither[Request,Result]

  /** Adds the `toResult` operation to AWS flows. */
  implicit class FlowOps[Request <: AmazonWebServiceRequest,Result]
      (val flow: Flow[Request,Either[AmazonClientException,Result],NotUsed]) extends AnyVal {
    def toResult: Flow[Request,Result,NotUsed] =
      flow.map {
        case Left(exception) ⇒ throw exception
        case Right(result) ⇒ result
      }
  }
}
