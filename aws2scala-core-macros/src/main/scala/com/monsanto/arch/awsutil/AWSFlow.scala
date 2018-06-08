package com.monsanto.arch.awsutil

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.{AmazonClientException, AmazonWebServiceRequest}
import com.monsanto.arch.awsutil.impl.{AWSGraphStage, SimpleAWSFlowAdapter}
import AWSFlowAdapter.{nextTokenFlowAdapter,markerFlowAdapter,nextMarkerFlowAdapter}

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
  def pagedByNextToken[Request <: AmazonWebServiceRequest: TakesNextToken, Result: HasNextToken]
      (asyncCall: AWSAsyncCall[Request,Result]): Flow[Request, Result, NotUsed] =
    pagedByNextTokenEither(asyncCall).map {
      case Left(exception) => throw exception
      case Right(result) => result
    }

  /** A paged flow may return more than one result for each request, in this case pagination is managed by
    * `Result.getNextToken` and `Request.withNextToken`.
    */
  def pagedByNextTokenEither[Request <: AmazonWebServiceRequest: TakesNextToken, Result: HasNextToken]
      (asyncCall: AWSAsyncCall[Request,Result]): Flow[Request, Either[AmazonClientException,Result], NotUsed] =
    akka.stream.scaladsl.Flow.fromGraph(new AWSGraphStage(nextTokenFlowAdapter(asyncCall)))

  /** A paged flow may return more than one result for each request, in this case pagination is managed by
    * `Result.getNextMarker` and `Request.withMarker`.
    */
  def pagedByNextMarker[Request <: AmazonWebServiceRequest: TakesMarker, Result: HasNextMarker]
      (asyncCall: AWSAsyncCall[Request,Result]): Flow[Request, Result, NotUsed] =
    pagedByNextMarkerEither(asyncCall).map {
      case Left(exception) => throw exception
      case Right(result) => result
    }

  /** A paged flow may return more than one result for each request, in this case pagination is managed by
    * `Result.getNextMarker` and `Request.withMarker`.  Amazon exceptions will cause the flow to fail.
    */
  def pagedByNextMarkerEither[Request <: AmazonWebServiceRequest: TakesMarker, Result: HasNextMarker]
      (asyncCall: AWSAsyncCall[Request,Result]): Flow[Request, Either[AmazonClientException,Result], NotUsed] =
    akka.stream.scaladsl.Flow.fromGraph(new AWSGraphStage(nextMarkerFlowAdapter(asyncCall)))

  /** A paged flow may return more than one result for each request, in this case pagination is managed by
    * `Result.getMarker` and `Request.withMarker`.
    */
  def pagedByMarker[Request <: AmazonWebServiceRequest: TakesMarker, Result: HasMarker]
      (asyncCall: AWSAsyncCall[Request,Result]): Flow[Request, Result, NotUsed] =
    pagedByMarkerEither(asyncCall).map {
      case Left(exception) => throw exception
      case Right(result) => result
    }

  /** A paged flow may return more than one result for each request, in this case pagination is managed by
    * `Result.getMarker` and `Request.withMarker`.  Amazon exceptions will cause the flow to fail.
    */
  def pagedByMarkerEither[Request <: AmazonWebServiceRequest: TakesMarker, Result: HasMarker]
      (asyncCall: AWSAsyncCall[Request,Result]): Flow[Request, Either[AmazonClientException,Result], NotUsed] =
    akka.stream.scaladsl.Flow.fromGraph(new AWSGraphStage(markerFlowAdapter(asyncCall)))

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
