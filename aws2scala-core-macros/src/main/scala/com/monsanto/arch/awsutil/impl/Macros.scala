package com.monsanto.arch.awsutil.impl

import com.amazonaws.AmazonWebServiceRequest
import com.monsanto.arch.awsutil.{AWSAsyncCall, AWSFlowAdapter}
import scala.reflect.macros.blackbox

/** Contains macro implementations. */
object Macros {
  def nextTokenFlowAdapter[Request <: AmazonWebServiceRequest, Result]
        (c: blackbox.Context)
        (asyncCall: c.Expr[AWSAsyncCall[Request,Result]])
        (implicit requestType: c.WeakTypeTag[Request], resultType: c.WeakTypeTag[Result]): c.Tree = {
    import c.universe._
    val adapterType = weakTypeOf[AWSFlowAdapter[Request,Result]]
    q"""
      new $adapterType {
        def processRequest: RequestProcessor = $asyncCall
        def getToken(result: $resultType) = Option(result.getNextToken).filter(_.nonEmpty)
        def withToken(request: $requestType, token: String) = request.withNextToken(token)
      }
    """
  }

  def pagedByNextToken[Request <: AmazonWebServiceRequest, Result]
        (c: blackbox.Context)
        (asyncCall: c.Expr[AWSAsyncCall[Request,Result]])
        (implicit requestType: c.WeakTypeTag[Request], resultType: c.WeakTypeTag[Result]): c.Tree = {
    import c.universe._
    val stageType = weakTypeTag[AWSGraphStage[Request,Result]]
    val flowAdapterObject = weakTypeTag[AWSFlowAdapter[Request,Result]].tpe.companion
    q"""
      akka.stream.scaladsl.Flow.fromGraph(new $stageType($flowAdapterObject.nextTokenFlowAdapter($asyncCall)))
        .map {
          case Left(exception) ⇒ throw exception
          case Right(result)   ⇒ result
        }
    """
  }

  def pagedByNextTokenEither[Request <: AmazonWebServiceRequest, Result]
        (c: blackbox.Context)
        (asyncCall: c.Expr[AWSAsyncCall[Request,Result]])
        (implicit requestType: c.WeakTypeTag[Request], resultType: c.WeakTypeTag[Result]): c.Tree = {
    import c.universe._
    val stageType = weakTypeTag[AWSGraphStage[Request,Result]]
    val flowAdapterObject = weakTypeTag[AWSFlowAdapter[Request,Result]].tpe.companion
    q"""
      akka.stream.scaladsl.Flow.fromGraph(new $stageType($flowAdapterObject.nextTokenFlowAdapter($asyncCall)))
    """
  }

  def nextMarkerFlowAdapter[Request <: AmazonWebServiceRequest, Result]
        (c: blackbox.Context)
        (asyncCall: c.Expr[AWSAsyncCall[Request,Result]])
        (implicit requestType: c.WeakTypeTag[Request], resultType: c.WeakTypeTag[Result]): c.Tree = {
    import c.universe._
    val adapterType = weakTypeOf[AWSFlowAdapter[Request,Result]]
    q"""
      new $adapterType {
        def processRequest: RequestProcessor = $asyncCall
        def getToken(result: $resultType) = Option(result.getNextMarker).filter(_.nonEmpty)
        def withToken(request: $requestType, token: String) = request.withMarker(token)
      }
      """
  }

  def pagedByNextMarker[Request <: AmazonWebServiceRequest, Result]
        (c: blackbox.Context)
        (asyncCall: c.Expr[AWSAsyncCall[Request,Result]])
        (implicit requestType: c.WeakTypeTag[Request], resultType: c.WeakTypeTag[Result]): c.Tree = {
    import c.universe._
    val stageType = weakTypeTag[AWSGraphStage[Request,Result]]
    val flowAdapterObject = weakTypeTag[AWSFlowAdapter[Request,Result]].tpe.companion
    q"""
      akka.stream.scaladsl.Flow.fromGraph(new $stageType($flowAdapterObject.nextMarkerFlowAdapter($asyncCall)))
        .map {
          case Left(exception) ⇒ throw exception
          case Right(result)   ⇒ result
        }
    """
  }

  def pagedByNextMarkerEither[Request <: AmazonWebServiceRequest, Result]
        (c: blackbox.Context)
        (asyncCall: c.Expr[AWSAsyncCall[Request,Result]])
        (implicit requestType: c.WeakTypeTag[Request], resultType: c.WeakTypeTag[Result]): c.Tree = {
    import c.universe._
    val stageType = weakTypeTag[AWSGraphStage[Request,Result]]
    val flowAdapterObject = weakTypeTag[AWSFlowAdapter[Request,Result]].tpe.companion
    q"""
      akka.stream.scaladsl.Flow.fromGraph(new $stageType($flowAdapterObject.nextMarkerFlowAdapter($asyncCall)))
    """
  }

  def markerFlowAdapter[Request <: AmazonWebServiceRequest, Result]
        (c: blackbox.Context)
        (asyncCall: c.Expr[AWSAsyncCall[Request,Result]])
        (implicit requestType: c.WeakTypeTag[Request], resultType: c.WeakTypeTag[Result]): c.Tree = {
    import c.universe._
    val adapterType = weakTypeOf[AWSFlowAdapter[Request,Result]]
    q"""
      new $adapterType {
        def processRequest: RequestProcessor = $asyncCall
        def getToken(result: $resultType) = Option(result.getMarker).filter(_.nonEmpty)
        def withToken(request: $requestType, token: String) = request.withMarker(token)
      }
      """
  }

  def pagedByMarker[Request <: AmazonWebServiceRequest, Result]
        (c: blackbox.Context)
        (asyncCall: c.Expr[AWSAsyncCall[Request,Result]])
        (implicit requestType: c.WeakTypeTag[Request], resultType: c.WeakTypeTag[Result]): c.Tree = {
    import c.universe._
    val stageType = weakTypeTag[AWSGraphStage[Request,Result]]
    val flowAdapterObject = weakTypeTag[AWSFlowAdapter[Request,Result]].tpe.companion
    q"""
      akka.stream.scaladsl.Flow.fromGraph(new $stageType($flowAdapterObject.markerFlowAdapter($asyncCall)))
        .map {
          case Left(exception) ⇒ throw exception
          case Right(result)   ⇒ result
        }
    """
  }

  def pagedByMarkerEither[Request <: AmazonWebServiceRequest, Result]
        (c: blackbox.Context)
        (asyncCall: c.Expr[AWSAsyncCall[Request,Result]])
        (implicit requestType: c.WeakTypeTag[Request], resultType: c.WeakTypeTag[Result]): c.Tree = {
    import c.universe._
    val stageType = weakTypeTag[AWSGraphStage[Request,Result]]
    val flowAdapterObject = weakTypeTag[AWSFlowAdapter[Request,Result]].tpe.companion
    q"""
      akka.stream.scaladsl.Flow.fromGraph(new $stageType($flowAdapterObject.markerFlowAdapter($asyncCall)))
    """
  }
}
