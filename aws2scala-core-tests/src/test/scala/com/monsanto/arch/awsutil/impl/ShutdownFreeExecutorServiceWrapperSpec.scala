package com.monsanto.arch.awsutil.impl

import java.util
import java.util.concurrent._

import akka.Done
import com.monsanto.arch.awsutil.impl.ShutdownFreeExecutorServiceWrapperSpec._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

import scala.collection.JavaConverters._
import scala.concurrent.Promise

class ShutdownFreeExecutorServiceWrapperSpec extends FreeSpec {
  val wrapper = new ShutdownFreeExecutorServiceWrapper(FakeExecutorService)

  "a ShutdownFreeExecutorServiceWrapper" - {
    "passes through" - {
      "invokeAll" in {
        forAll { tasks: Seq[FakeCallable[Int]] ⇒
          val expectedResult = tasks.map(_.call())
          val result = wrapper.invokeAll(tasks.asJavaCollection).asScala.map(_.get())
          result shouldBe expectedResult
        }
      }

      "invokeAll with timeout" in {
        forAll { (tasks: Seq[FakeCallable[Int]], timeout: Long, timeUnit: TimeUnit) ⇒
          val expectedResult = tasks.map(_.call())
          val result = wrapper.invokeAll(tasks.asJavaCollection, timeout, timeUnit).asScala.map(_.get())
          result shouldBe expectedResult
        }
      }

      "invokeAny" in {
        forAll { tasks: Seq[FakeCallable[Int]] ⇒
          whenever(tasks.nonEmpty) {
            val expectedResult = tasks.head.call()
            val result = wrapper.invokeAny(tasks.asJavaCollection)
            result shouldBe expectedResult
          }
        }
      }

      "invokeAny with timeout" in {
        forAll { (tasks: Seq[FakeCallable[Int]], timeout: Long, timeUnit: TimeUnit) ⇒
          whenever(tasks.nonEmpty) {
            val expectedResult = tasks.head.call()
            val result = wrapper.invokeAny(tasks.asJavaCollection, timeout, timeUnit)
            result shouldBe expectedResult
          }
        }
      }

      "isShutdown" in {
        wrapper.isShutdown shouldBe false
      }

      "isTerminated" in {
        wrapper.isTerminated shouldBe false
      }

      "submit(Callable[T])" in {
        forAll { task: FakeCallable[Int] ⇒
          wrapper.submit(task) shouldBe theSameInstanceAs (task.toFakeFuture)
        }
      }

      "submit(Runnable)" in {
        forAll(arbitrary[Runnable]) { task ⇒
          wrapper.submit(task) shouldBe theSameInstanceAs (FakeRunnableFuture)
        }
      }

      "submit(Runnable,T)" in {
        forAll { (runnable: Runnable, str: String) ⇒
          wrapper.submit(runnable, str).get shouldBe str
        }
      }

      "execute" in {
        forAll { str: String ⇒
          val promise = Promise[String]
          val runnable =  new Runnable {
            override def run() = promise.success(str)
          }
          wrapper.execute(runnable)
          promise.future.value.get.get shouldBe str

        }
      }
    }

    "intercepts" - {
      "awaitTerminationCalls" in {
        forAll(
          Gen.posNum[Long] → "timeout",
          arbitrary[TimeUnit] → "timeUnit"
        ) { (timeout, timeUnit) ⇒
          wrapper.awaitTermination(timeout, timeUnit) shouldBe true
        }
      }

      "shutdown" in {
        wrapper.shutdown()
      }

      "shutdownNow" in {
        wrapper.shutdownNow() shouldBe empty
      }
    }
  }
}

object ShutdownFreeExecutorServiceWrapperSpec {
  class FakeCallable[T](t: T) extends Callable[T] {
    override def call() = t
    lazy val toFakeFuture: Future[T] = new Future[T] {
      override def isCancelled = false
      override def get() = t
      override def get(timeout: Long, unit: TimeUnit) = t
      override def cancel(mayInterruptIfRunning: Boolean) = false
      override def isDone = true
    }
  }
  implicit def arbFakeCallable[T: Arbitrary]: Arbitrary[FakeCallable[T]] = Arbitrary(Gen.resultOf(new FakeCallable(_: T)))

  implicit val arbRunnable: Arbitrary[Runnable] =
    Arbitrary {
      Gen.const(new Runnable {
        override def run() = throw new UnsupportedOperationException
      })
    }

  val FakeRunnableFuture = new FakeCallable(Done.getInstance()).toFakeFuture

  object FakeExecutorService extends ExecutorService {
    override def shutdown() = throw new UnsupportedOperationException

    override def isTerminated = false

    override def awaitTermination(timeout: Long, unit: TimeUnit) = throw new UnsupportedOperationException

    override def shutdownNow() = throw new UnsupportedOperationException

    override def isShutdown = false

    override def execute(command: Runnable) = command.run()

    override def invokeAll[T](tasks: util.Collection[_ <: Callable[T]]) =
      tasks.asScala.toList.map {
        case x: FakeCallable[T] ⇒ x.toFakeFuture
      }.asJava

    override def invokeAll[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit) =
      tasks.asScala.toList.map {
        case x: FakeCallable[T] ⇒ x.toFakeFuture
      }.asJava

    override def invokeAny[T](tasks: util.Collection[_ <: Callable[T]]) =
      tasks.asScala.head.asInstanceOf[FakeCallable[T]].call()

    override def invokeAny[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit) =
      tasks.asScala.head.asInstanceOf[FakeCallable[T]].call()

    override def submit[T](task: Callable[T]) =
      task match {
        case x: FakeCallable[T] ⇒ x.toFakeFuture
      }

    override def submit[T](task: Runnable, result: T) = new FakeCallable(result).toFakeFuture

    override def submit(task: Runnable) = FakeRunnableFuture
  }
}
