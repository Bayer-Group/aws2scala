package com.monsanto.arch.awsutil.sns.model

import org.scalacheck.{Arbitrary, Gen}
import spray.json.DefaultJsonProtocol._
import spray.json._

case class RetryPolicy(minDelayTarget: Int,
                       maxDelayTarget: Int,
                       numRetries: Int,
                       numNoDelayRetries: Int,
                       numMinDelayRetries: Int,
                       numMaxDelayRetries: Int,
                       backoffFunction: RetryPolicy.BackoffFunction)

object RetryPolicy {
  implicit lazy val arbRetryPolicy: Arbitrary[RetryPolicy] =
    Arbitrary {
      for {
        numRetries ← Gen.choose(0,100)
        numNoDelayRetries ← Gen.choose(0,numRetries)
        minDelayTarget ← Gen.choose(0,120)
        numMinDelayRetries ← Gen.choose(0,numRetries)
        maxDelayTarget ← Gen.choose(minDelayTarget,3600)
        numMaxDelayRetries ← Gen.choose(0,numRetries)
        backoffFunction ← Gen.oneOf(BackoffFunction.values)
      } yield
        RetryPolicy(
          minDelayTarget,
          maxDelayTarget,
          numRetries,
          numNoDelayRetries,
          numMinDelayRetries,
          numMaxDelayRetries,
          backoffFunction)
    }

  implicit lazy val jsonFormat: RootJsonFormat[RetryPolicy] = jsonFormat7(RetryPolicy.apply)


  sealed trait BackoffFunction
  object BackoffFunction {
    case object Linear extends BackoffFunction
    case object Arithmetic extends BackoffFunction
    case object Geometric extends BackoffFunction
    case object Exponential extends BackoffFunction

    val values: Seq[BackoffFunction] = Seq(Linear, Arithmetic, Geometric, Exponential)

    implicit lazy val jsonFormat: JsonFormat[BackoffFunction] = new JsonFormat[BackoffFunction] {
      override def read(json: JsValue): BackoffFunction =
        json match {
          case JsString("linear")      ⇒ Linear
          case JsString("arithmetic")  ⇒ Arithmetic
          case JsString("geometric")   ⇒ Geometric
          case JsString("exponential") ⇒ Exponential
          case x                       ⇒ deserializationError(s"Expected a backoff function name, but got a $x")
        }

      override def write(obj: BackoffFunction): JsValue =
        obj match {
          case Linear      ⇒ JsString("linear")
          case Arithmetic  ⇒ JsString("arithmetic")
          case Geometric   ⇒ JsString("geometric")
          case Exponential ⇒ JsString("exponential")
        }
    }
  }
}
