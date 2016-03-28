package com.monsanto.arch.awsutil.test_support

import akka.NotUsed
import akka.stream.scaladsl.Flow
import org.scalamock.handlers.CallHandler0
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers._

import scala.collection.immutable

/** Utilities for making mocking flows relatively pain-free. */
trait FlowMockUtils { this: MockFactory ⇒
  implicit class FlowCallHandler[In,Out](call: CallHandler0[Flow[In,Out,NotUsed]]) {
    /** Returns a flow that will verify the input and emit the output. */
    def returningFlow(expected: In, result: Out): CallHandler0[Flow[In,Out,NotUsed]] =
      call.returning(Flow[In].map { actual ⇒ actual shouldBe expected; result })

    /** Returns a flow that will verify the input and emit the output one element at a time. */
    def returningConcatFlow(expected: In, result: immutable.Seq[Out]): CallHandler0[Flow[In,Out,NotUsed]] =
      call.returning(Flow[In].mapConcat { actual ⇒ actual shouldBe expected; result })

    /** Returns a flow that will cause a test to fail. */
    def returningFailingFlow(): CallHandler0[Flow[In,Out,NotUsed]] =
      call.returning(Flow[In].map(_ ⇒ fail("This flow should not have been invoked.")))
  }
}
