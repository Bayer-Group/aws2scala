package com.monsanto.arch.awsutil.impl

import java.util.concurrent.{Future ⇒ JFuture}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import com.amazonaws.{AmazonClientException, AmazonWebServiceRequest}
import com.monsanto.arch.awsutil.impl.AWSGraphStageSpec._
import com.monsanto.arch.awsutil._
import com.typesafe.config.ConfigFactory
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers._
import org.scalatest.{BeforeAndAfterAll, FreeSpec}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Promise}
import scala.util.{Failure, Success}

/** Lower-level unit tests of the AWSGraphStage. */
class AWSGraphStageSpec extends FreeSpec with BeforeAndAfterAll with MockFactory {
  implicit val actorSystem = ActorSystem("AWSGraphStageSpec", TestConfig)

  private case class IdleFixture(source: TestPublisher.Probe[TestRequest],
                                 sink: TestSubscriber.Probe[Either[AmazonClientException,TestResult]],
                                 asyncCall: AWSAsyncCall[TestRequest,TestResult])

  private def withIdleFixture(test: IdleFixture ⇒ Any): Unit = {
    implicit val m = ActorMaterializer()
    val asyncCall = mock[AWSAsyncCall[TestRequest,TestResult]]
    val (source, sink) = TestSource.probe[TestRequest]
      .via(AWSFlow.pagedByNextTokenEither(asyncCall))
      .toMat(TestSink.probe[Either[AmazonClientException,TestResult]])(Keep.both)
      .run()
    source.ensureSubscription()
    sink.ensureSubscription()
    source.expectRequest() shouldBe 1
    try {
      test(IdleFixture(source, sink, asyncCall))
      sink.expectNoMsg()
      source.expectNoMsg()
    } finally {
      m.shutdown()
    }
  }

  private case class AwaitingAwsFixture(source: TestPublisher.Probe[TestRequest],
                                        sink: TestSubscriber.Probe[Either[AmazonClientException,TestResult]],
                                        asyncCall: AWSAsyncCall[TestRequest,TestResult],
                                        request: TestRequest,
                                        promise: Promise[TestResult],
                                        jFuture: JFuture[TestResult])

  private def withAwaitingAwsFixture(test: AwaitingAwsFixture ⇒ Any): Unit = {
    withIdleFixture { f ⇒
      val request = TestRequest(1, None)
      val jFuture = mock[JFuture[TestResult]]
      val promise = Promise[TestResult]
      (f.asyncCall.apply _).expects(request, *).onCall { (_, handler) ⇒
        promise.future.onComplete {
          case Success(result) ⇒ handler.onSuccess(request, result)
          case Failure(cause: Exception) ⇒ handler.onError(cause)
          case Failure(cause) ⇒ handler.onError(new RuntimeException("Got a non-exception", cause))
        }(actorSystem.dispatcher)
        jFuture
      }.noMoreThanOnce()
      f.source.unsafeSendNext(request)
      f.source.expectRequest() shouldBe 1
      test(AwaitingAwsFixture(f.source, f.sink, f.asyncCall, request, promise, jFuture))
    }
  }

  private def withAwaitingAwsFixtureWithDemand(test: AwaitingAwsFixture ⇒ Any) = {
    withAwaitingAwsFixture { f ⇒
      f.sink.request(1)
      test(f)
    }
  }

  case class CachedResultFixture(source: TestPublisher.Probe[TestRequest],
                                 sink: TestSubscriber.Probe[Either[AmazonClientException,TestResult]],
                                 asyncCall: AWSAsyncCall[TestRequest,TestResult],
                                 cachedResult: Either[AmazonClientException,TestResult],
                                 nextRequest: Option[TestRequest])

  private def withCachedResultFixture(test: CachedResultFixture ⇒ Any): Unit = {
    withAwaitingAwsFixture { f ⇒
      val result = TestResult(1, 1, None)
      f.promise.success(result)
      // allow things to settle
      f.source.expectNoMsg()
      f.sink.expectNoMsg()
      test(CachedResultFixture(f.source, f.sink, f.asyncCall, Right(result), None))
    }
  }

  private def withCachedResultWithNextRequestFixture(test: CachedResultFixture ⇒ Any): Unit = {
    withAwaitingAwsFixture { f ⇒
      val token = Some("a token")
      val result = TestResult(1, 1, token)
      f.promise.success(result)
      // allow things to settle
      f.source.expectNoMsg()
      f.sink.expectNoMsg()
      test(CachedResultFixture(f.source, f.sink, f.asyncCall, Right(result), Some(TestRequest(1, token))))
    }
  }

  val dynamite = new Exception("KABOOM!")

  "an AWSGraphStage," - {
    "when idle, it" - {
      "does nothing on its own" in withIdleFixture { f ⇒
        f.source.expectNoMsg()
        f.sink.expectNoMsg()
      }

      "does nothing with downstream demand" in withIdleFixture { f ⇒
        f.sink.request(1)
      }

      "starts processing with upstream supply" in withIdleFixture { f ⇒
        val request: TestRequest = TestRequest(1)
        (f.asyncCall.apply _).expects(request, *)
        // be unsafe here because we requested in the fixture
        f.source.unsafeSendNext(request)
        // buffer requesting refill
        f.source.expectRequest() shouldBe 1
      }

      "finishes when downstream completes" in withIdleFixture { f ⇒
        f.sink.cancel()
        f.source.expectCancellation()
      }

      "finishes when upstream completes" in withIdleFixture { f ⇒
        f.source.sendComplete()
        f.sink.expectComplete()
      }

      "fails when upstream fails" in withIdleFixture { f ⇒
        f.source.sendError(dynamite)
        f.sink.expectError(dynamite)
      }
    }

    "when awaiting AWS, it" - {
      "does nothing with downstream demand" in withAwaitingAwsFixture { f ⇒
        f.sink.request(1)
      }

      "fails with a non-AWS exception" in withAwaitingAwsFixture { f ⇒
        f.promise.failure(dynamite)
        f.sink.expectError(dynamite)
        f.source.expectCancellation()
      }

      "absorbs upstream termination" in withAwaitingAwsFixture { f ⇒
        f.source.sendComplete()
      }

      "fails with upstream failure (and AWS future is cancelled)" in withAwaitingAwsFixture { f ⇒
        (f.jFuture.cancel _).expects(true)
        f.source.sendError(dynamite)
        f.sink.expectError(dynamite)
      }

      "cancels with downstream cancellation (and AWS future is cancelled" in withAwaitingAwsFixture { f ⇒
        (f.jFuture.cancel _).expects(true)
        f.sink.cancel()
        f.source.expectCancellation()
      }
    }

    "when it gets a result from AWS" - {
      "with no more pages and downstream is" - {
        val result = TestResult(1, 1, None)

        "idle, it caches the result" in withAwaitingAwsFixture { f ⇒
          f.promise.success(result)
        }

        "waiting, and upstream is" - {
          "not closed, it delivers the result" in withAwaitingAwsFixtureWithDemand { f ⇒
            f.promise.success(result)
            f.sink.expectNext(Right(result))
          }

          "closed, it delivers the result and finishes" in withAwaitingAwsFixtureWithDemand { f ⇒
            f.source.sendComplete()
            f.promise.success(result)
            f.sink.expectNext(Right(result))
            f.sink.expectComplete()
          }
        }
      }

      "with a next page and downstream is" - {
        val result = TestResult(1, 1, Some("a token"))

        "idle, it caches the result" in withAwaitingAwsFixture { f ⇒
          f.promise.success(result)
        }

        "waiting, it sends the result and gets the next page" in withAwaitingAwsFixture { f ⇒
          (f.asyncCall.apply _).expects(TestRequest(1, Some("a token")), *)
          f.sink.request(1)
          f.promise.success(result)
          f.sink.expectNext(Right(result))
        }
      }

      "indicating an AWS exception and downstream is" - {
        val awsException = new AmazonClientException("AWS set up us the bomb!")

        "idle, it caches the result" in withAwaitingAwsFixture { f ⇒
          f.promise.failure(awsException)
        }

        "waiting, and upstream is" - {
          "not closed, it delivers the exception" in withAwaitingAwsFixtureWithDemand { f ⇒
            f.promise.failure(awsException)
            f.sink.expectNext(Left(awsException))
          }

          "closed, it delivers the result and finishes" in withAwaitingAwsFixtureWithDemand { f ⇒
            f.source.sendComplete()
            f.promise.failure(awsException)
            f.sink.expectNext(Left(awsException))
            f.sink.expectComplete()
          }
        }
      }
    }

    "when it has cached result with no pending page, it" - {
      "delivers the result on downstream demand when upstream is" - {
        "idle" in withCachedResultFixture { f ⇒
          f.sink.requestNext(f.cachedResult)
        }

        "closed (completing the stream)" in withCachedResultFixture { f ⇒
          f.source.sendComplete()
          f.sink.requestNext(f.cachedResult)
          f.sink.expectComplete()
        }
      }

      "does nothing when upstream completes" in withCachedResultFixture { f ⇒
        f.source.sendComplete()
      }

      "fails when upstream fails" in withCachedResultFixture { f ⇒
        f.source.sendError(dynamite)
        f.sink.expectError(dynamite)
      }

      "cancels when downstream closes" in withCachedResultFixture { f ⇒
        f.sink.cancel()
        f.source.expectCancellation()
      }
    }

    "when it has cached result with a pending page, it" - {
      "delivers the result on downstream demand requests the next page" in withCachedResultWithNextRequestFixture { f ⇒
        (f.asyncCall.apply _).expects(f.nextRequest.get, *)
        f.sink.requestNext(f.cachedResult)
      }

      "does nothing when upstream completes" in withCachedResultWithNextRequestFixture { f ⇒
        f.source.sendComplete()
      }

      "fails when upstream fails" in withCachedResultWithNextRequestFixture { f ⇒
        f.source.sendError(dynamite)
        f.sink.expectError(dynamite)
      }

      "cancels when downstream closes" in withCachedResultWithNextRequestFixture { f ⇒
        f.sink.cancel()
        f.source.expectCancellation()
      }
    }
  }

  override protected def afterAll(): Unit = {
    try Await.result(actorSystem.terminate(), 3.seconds)
    finally super.afterAll()
  }
}

object AWSGraphStageSpec {
  lazy val TestConfig = ConfigFactory.parseString(
    """akka {
      |  stream.materializer {
      |    initial-input-buffer-size = 1
      |    max-input-buffer-size = 1
      |  }
      |  test.single-expect-default = 100 milliseconds
      |}""".stripMargin).withFallback(ConfigFactory.load())

  case class TestRequest(id: Int, token: Option[String] = None) extends AmazonWebServiceRequest {
    def withNextToken(token: String) = TestRequest(id, Some(token))
  }
  case class TestResult(requestId: Int, pageId: Int, token: Option[String] = None) {
    def getNextToken = token.orNull
  }
}
