package com.monsanto.arch.awsutil.impl

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, FlowShape}
import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler
import com.monsanto.arch.awsutil.{AWSAsyncCall, AWSFlow}
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{BeforeAndAfterAll, FreeSpec, Matchers}

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{Await, Future}

/** Mid-level integration tests of the AWSGraphStage using streams. */
class AWSGraphStageStreamSpec extends FreeSpec with MockFactory with BeforeAndAfterAll {
  import AWSGraphStageStreamSpec._
  import Eventually.{patienceConfig ⇒ _, _}
  import GeneratorDrivenPropertyChecks._
  import Matchers._
  import ScalaFutures.{patienceConfig ⇒ _, _}

  private val throttle = 10.milliseconds
  private implicit val actorSystem = ActorSystem("AWSGraphStageStreamSpec")
  private implicit val dispatcher = actorSystem.dispatcher

  private val dynamite = new Exception("KABOOM!!!")
  private val sink = Sink.fold(Seq.empty[Int]) { (seq, item: TestResult) ⇒ seq :+ item.pageId }

  "an AWSGraphStage streams results" - {
    import Eventually.{patienceConfig ⇒ _}
    implicit val streamMaterialiser = ActorMaterializer()(actorSystem)
    implicit val futurePatienceConfig = ScalaFutures.PatienceConfig(5.seconds, 10.milliseconds)
    val eventuallyPatienceConfig = Eventually.PatienceConfig(5.seconds, 10.milliseconds)

    "from a single request terminating" - {
      "normally" in {
        forAll(Gen.nonEmptyListOf(Gen.posNum[Int]) → "pages") { pages ⇒
          val flow = createFlowFor(Seq(pages))
          val graph = Source.single(TestRequest(0)).via(flow).toMat(sink)(Keep.right)
          graph.run().futureValue shouldBe pages
        }
      }

      "with an error" in {
        forAll(Gen.nonEmptyListOf(Gen.posNum[Int]) → "pages") { pages ⇒
          val flow = createFlowFor(Seq(pages), terminalError = Some(dynamite))
          val graph = Source.single(TestRequest(0)).via(flow).toMat(sink)(Keep.right)
          val result = graph.run()
          eventually {
            result.eitherValue.get shouldBe Left(dynamite)
          }(eventuallyPatienceConfig)
        }
      }
    }

    "from multiple requests" - {
      "with no throttling" - {
        "terminating normally" in {
          forAll(Gen.nonEmptyListOf(Gen.nonEmptyListOf(Gen.posNum[Int])) → "list of pages", maxSize(10)) { listOfPages ⇒
            val flow = createFlowFor(listOfPages)
            val source = Source(listOfPages.indices.map(TestRequest(_)))
            val graph = source.via(flow).toMat(sink)(Keep.right)
            graph.run().futureValue shouldBe listOfPages.flatten
          }
        }

        "terminating with an error" in {
          forAll(Gen.nonEmptyListOf(Gen.nonEmptyListOf(Gen.posNum[Int])) → "list of pages", maxSize(10)) { listOfPages ⇒
            val flow = createFlowFor(listOfPages, terminalError = Some(dynamite))
            val source = Source(listOfPages.indices.map(TestRequest(_)))
            val result = source.via(flow).toMat(sink)(Keep.right).run()
            eventually {
              result.eitherValue.get shouldBe Left(dynamite)
            }(eventuallyPatienceConfig)
          }
        }
      }

      "with the source throttled" - {
        "terminating normally" in {
          forAll(Gen.nonEmptyListOf(Gen.nonEmptyListOf(Gen.posNum[Int])) → "list of pages", maxSize(8)) { listOfPages ⇒
            val flow = createFlowFor(listOfPages)
            val source = Source(listOfPages.indices.map(TestRequest(_))).via(rateLimit(throttle))
            val graph = source.via(flow).toMat(sink)(Keep.right)
            graph.run().futureValue shouldBe listOfPages.flatten
          }
        }

        "terminating with an error" in {
          forAll(Gen.nonEmptyListOf(Gen.nonEmptyListOf(Gen.posNum[Int])) → "list of pages", maxSize(8)) { listOfPages ⇒
            val flow = createFlowFor(listOfPages, terminalError = Some(dynamite))
            val source = Source(listOfPages.indices.map(TestRequest(_))).via(rateLimit(throttle))
            val result = source.via(flow).toMat(sink)(Keep.right).run()
            eventually {
              result.eitherValue.get shouldBe Left(dynamite)
            }(eventuallyPatienceConfig)
          }
        }
      }

      "with the sink throttled" - {
        "terminating normally" in {
          forAll(Gen.nonEmptyListOf(Gen.nonEmptyListOf(Gen.posNum[Int])) → "list of pages", maxSize(6)) { listOfPages ⇒
            val flow = createFlowFor(listOfPages)
            val source = Source(listOfPages.indices.map(TestRequest(_)))
            val graph = source.via(flow).via(rateLimit(throttle)).toMat(sink)(Keep.right)
            graph.run().futureValue shouldBe listOfPages.flatten
          }
        }

        "terminating with an error" in {
          forAll(Gen.nonEmptyListOf(Gen.nonEmptyListOf(Gen.posNum[Int])) → "list of pages", maxSize(6)) { listOfPages ⇒
            val flow = createFlowFor(listOfPages, terminalError = Some(dynamite))
            val source = Source(listOfPages.indices.map(TestRequest(_)))
            val result = source.via(flow).via(rateLimit(throttle)).toMat(sink)(Keep.right).run()
            eventually {
              result.eitherValue.get shouldBe Left(dynamite)
            }(eventuallyPatienceConfig)
          }
        }
      }

      "when the processor is slow" - {
        "terminating normally" in {
          forAll(Gen.nonEmptyListOf(Gen.nonEmptyListOf(Gen.posNum[Int])) → "list of pages", maxSize(6)) { listOfPages ⇒
            val flow = createFlowFor(listOfPages, throttle = throttle)
            val source = Source(listOfPages.indices.map(TestRequest(_)))
            val graph = source.via(flow).toMat(sink)(Keep.right)
            graph.run().futureValue shouldBe listOfPages.flatten
          }
        }

        "terminating with an error" in {
          forAll(Gen.nonEmptyListOf(Gen.nonEmptyListOf(Gen.posNum[Int])) → "list of pages", maxSize(6)) { listOfPages ⇒
            val flow = createFlowFor(listOfPages, throttle = throttle, terminalError = Some(dynamite))
            val source = Source(listOfPages.indices.map(TestRequest(_)))
            val result = source.via(flow).toMat(sink)(Keep.right).run()
            eventually {
              result.eitherValue.get shouldBe Left(dynamite)
            }(eventuallyPatienceConfig)
          }
        }
      }
    }

    "from a simple flow" in {
      forAll(Gen.nonEmptyListOf(Gen.posNum[Int]) → "pages") { pages ⇒
        val asyncCall = mock[AWSAsyncCall[TestRequest,TestResult]]
        pages.zipWithIndex.foreach { case (pageId, requestId) ⇒
          val request = TestRequest(requestId)
          val result = TestResult(requestId, pageId)
          (asyncCall.apply _).expects(request, *).onCall { (_, handler) ⇒
            Future {
              handler.onSuccess(request, result)
            }
            null
          }
        }
        val source = Source(pages.indices.map(TestRequest(_)))
        val flow = AWSFlow.simple(asyncCall)
        val result = source.via(flow).toMat(sink)(Keep.right).run()
        result.futureValue shouldBe pages
      }
    }
  }

  private def createFlowFor(allPageIds: Seq[Seq[Int]], terminalError: Option[Exception] = None, throttle: FiniteDuration = Duration.Zero) = {
    val processor = mock[AWSAsyncCall[TestRequest,TestResult]]
    for {
      (pageIds, requestIndex) ← allPageIds.zipWithIndex
      tokens = Range(1, pageIds.size).map(i ⇒ Some(i.toString))
      ((pageId, requestToken), resultToken) ← pageIds.zip(None +: tokens).zip(tokens :+ None)
      request = TestRequest(requestIndex, requestToken)
    } {
      (processor.apply _).expects(request, *).onCall { (r: TestRequest, h: AsyncHandler[TestRequest, TestResult]) ⇒
        Future {
          if (throttle > Duration.Zero) {
            Thread.sleep(throttle.toMillis)
          }
          if (terminalError.isDefined && resultToken.isEmpty && (requestIndex + 1) == allPageIds.size) {
            val error = terminalError.get
            h.onError(error)
          } else {
            val result = TestResult(requestIndex, pageId, resultToken)
            h.onSuccess(r, result)
          }
        }
        null
      }
    }
    AWSFlow.pagedByNextToken(processor)
  }

  private def rateLimit[T](interval: FiniteDuration): Flow[T,T,NotUsed] = Flow.fromGraph(
    GraphDSL.create() { implicit b ⇒
      import GraphDSL.Implicits._

      val ticks = Source.tick(0.days, interval, "TICK")
      val rateLimited = b.add(ZipWith((_: String, t: T) ⇒ t))

      ticks ~> rateLimited.in0
      FlowShape(rateLimited.in1, rateLimited.out)
    }
  )

  override protected def afterAll(): Unit = {
    try Await.result(actorSystem.terminate(), 3.seconds)
    finally super.afterAll()
  }
}

object AWSGraphStageStreamSpec {
  case class TestRequest(id: Int, token: Option[String] = None) extends AmazonWebServiceRequest {
    def withNextToken(token: String) = TestRequest(id, Some(token))
  }
  case class TestResult(requestId: Int, pageId: Int, token: Option[String] = None) {
    def getNextToken = token.orNull
  }
}
