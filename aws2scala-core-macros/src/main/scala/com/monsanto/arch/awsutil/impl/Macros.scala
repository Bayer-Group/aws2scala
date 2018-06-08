package com.monsanto.arch.awsutil.impl

import scala.reflect.macros.blackbox

/** Contains macro implementations. */
object Macros {

  def hasNextTokenImpl[Result](c: blackbox.Context)(implicit resultType: c.WeakTypeTag[Result]): c.Tree = {
    import c.universe._
    q"new HasNextToken[$resultType] { def getToken(res: $resultType) = Option(res.getNextToken).filter(_.nonEmpty) }"
  }

  def takesNextTokenImpl[Request](c: blackbox.Context)(implicit requestType: c.WeakTypeTag[Request]): c.Tree = {
    import c.universe._
    q"new TakesNextToken[$requestType] { def withToken(req: $requestType, tok: String) = req.withNextToken(tok) }"
  }

  def hasNextMarkerImpl[Result](c: blackbox.Context)(implicit resultType: c.WeakTypeTag[Result]): c.Tree = {
    import c.universe._
    q"new HasNextMarker[$resultType] { def getNextMarker(res: $resultType) = Option(res.getNextMarker).filter(_.nonEmpty) }"
  }

  def hasMarkerImpl[Result](c: blackbox.Context)(implicit resultType: c.WeakTypeTag[Result]): c.Tree = {
    import c.universe._
    q"new HasMarker[$resultType] { def getMarker(res: $resultType) = Option(res.getMarker).filter(_.nonEmpty) }"
  }

  def takesMarkerImpl[Request](c: blackbox.Context)(implicit requestType: c.WeakTypeTag[Request]): c.Tree = {
    import c.universe._
    q"new TakesMarker[$requestType] { def withMarker(req: $requestType, marker:String) = req.withMarker(marker) }"
  }
}
