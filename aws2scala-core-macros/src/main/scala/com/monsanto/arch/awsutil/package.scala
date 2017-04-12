package com.monsanto.arch

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler
import java.util.concurrent.{Future => JFuture}

import scala.annotation.implicitNotFound
import scala.language.experimental.macros
import com.monsanto.arch.awsutil.impl.Macros

package object awsutil {
  /** Handy type alias for an asynchronous AWS call. */
  type AWSAsyncCall[Request <: AmazonWebServiceRequest, Result] = (Request, AsyncHandler[Request,Result]) => JFuture[Result]

  /** HasNextToken is a typeclass representing classes with a 'getNextToken' method */
  @implicitNotFound("Cannot prove that type ${R} has a getNextToken method")
  trait HasNextToken[R] {
    def getToken(res: R): Option[String]
  }

  object HasNextToken {
    def apply[R: HasNextToken]: HasNextToken[R] = implicitly

    implicit def instance[R]: HasNextToken[R] = macro Macros.hasNextTokenImpl[R]

    final implicit class HTSyntax[R](val ht: R) extends AnyVal {
      def getToken(implicit r: HasNextToken[R]): Option[String] = r.getToken(ht)
    }
  }

  /** TakesNextToken is a typeclass representing classes with a 'withNextToken' method */
  @implicitNotFound("Cannot prove that type ${R} has a withNextToken method")
  trait TakesNextToken[R] {
    def withToken(req: R, tok: String): R
  }

  object TakesNextToken {
    def apply[R: TakesNextToken]: TakesNextToken[R] = implicitly

    implicit def instance[R]: TakesNextToken[R] = macro Macros.takesNextTokenImpl[R]

    final implicit class TTSyntax[R](val tt: R) extends AnyVal {
      def withToken(tok: String)(implicit r: TakesNextToken[R]): R = r.withToken(tt,tok)
    }
  }

  /** HasNextMarker is a typeclass representing classes with a 'getNextMarker' method */
  @implicitNotFound("Cannot prove that type ${R} has a getNextMarker method")
  trait HasNextMarker[R] {
    def getNextMarker(res: R): Option[String]
  }

  object HasNextMarker {
    def apply[R: HasNextMarker]: HasNextMarker[R] = implicitly

    implicit def instance[R]: HasNextMarker[R] = macro Macros.hasNextMarkerImpl[R]

    final implicit class HNMSyntax[R](val hnm: R) extends AnyVal {
      def getNextMarker(implicit r: HasNextMarker[R]): Option[String] = r.getNextMarker(hnm)
    }
  }

  /** HasMarker is a typeclass representing classes with a 'getMarker' method */
  @implicitNotFound("Cannot prove that type ${R} has a getMarker method")
  trait HasMarker[R] {
    def getMarker(res: R): Option[String]
  }

  object HasMarker {
    def apply[R: HasMarker]: HasMarker[R] = implicitly

    implicit def instance[R]: HasMarker[R] = macro Macros.hasMarkerImpl[R]

    final implicit class HMSyntax[R](val hm: R) extends AnyVal {
      def getMarker(implicit r: HasMarker[R]): Option[String] = r.getMarker(hm)
    }
  }

  /**
    * TakesMarker is a typeclass representing classes with a 'withMarker' method
    */
  @implicitNotFound("Cannot prove that type ${R} has a withMarker method")
  trait TakesMarker[R] {
    def withMarker(req: R, marker: String): R
  }

  object TakesMarker {
    def apply[R: TakesMarker]: TakesMarker[R] = implicitly

    implicit def instance[R]: TakesMarker[R] = macro Macros.takesMarkerImpl[R]

    final implicit class TMSyntax[R](val tm: R) extends AnyVal {
      def withMarker(marker: String)(implicit r: TakesMarker[R]): R = r.withMarker(tm,marker)
    }
  }
}
