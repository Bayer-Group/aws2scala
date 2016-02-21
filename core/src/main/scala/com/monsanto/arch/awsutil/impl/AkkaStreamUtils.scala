package com.monsanto.arch.awsutil.impl

import akka.stream.scaladsl.Sink

import scala.concurrent.Future

private[awsutil] object AkkaStreamUtils {
  object Implicits {
    implicit class EnhancedSinkObject(val theSink: Sink.type) extends AnyVal {
      /** A `Sink` that keeps on counting elements until upstream terminates.  Materialises into a `Future[Long]`
        * containing a count of the elements.
        */
      def count[T]: Sink[T, Future[Long]] = Sink.fold(0L)((count, _: T) ⇒ count + 1)
    }
  }
}
