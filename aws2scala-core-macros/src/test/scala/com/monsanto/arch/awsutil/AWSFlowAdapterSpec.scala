package com.monsanto.arch.awsutil

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler
import java.util.concurrent.{Future => JFuture, TimeUnit}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpec, Matchers}

class AWSFlowAdapterSpec extends FreeSpec with MockFactory {
  import AWSFlowAdapterSpec._
  import Matchers._

  "the nextTokenFlowAdapter" - {
    "processes requests" in {
      val requestProcessor = mock[AWSAsyncCall[NextTokenRequest,NextTokenResult]]
      val request = NextTokenRequest(Some("foo"))
      val result = NextTokenResult(Some("bar"))
      val jFuture = new JFuture[NextTokenResult] {
        override def isCancelled: Boolean = false
        override def get(): NextTokenResult = result
        override def get(timeout: Long, unit: TimeUnit): NextTokenResult = result
        override def cancel(mayInterruptIfRunning: Boolean): Boolean = false
        override def isDone: Boolean = true
      }
      (requestProcessor.apply _).expects(request, *).returning(jFuture)
      val adapter = AWSFlowAdapter.nextTokenFlowAdapter(requestProcessor)
      adapter.processRequest(request, null).get() shouldBe result
    }

    "sets request tokens" in {
      val adapter = AWSFlowAdapter.nextTokenFlowAdapter(mock[AWSAsyncCall[NextTokenRequest,NextTokenResult]])
      val token = "foo"
      adapter.withToken(NextTokenRequest(None), token) shouldBe NextTokenRequest(Some(token))
    }

    "gets request tokens when underlying token is" - {
      "null" in {
        val adapter = AWSFlowAdapter.nextTokenFlowAdapter(mock[AWSAsyncCall[NextTokenRequest,NextTokenResult]])
        val result = NextTokenResult(None)
        result.getNextToken shouldBe null
        adapter.getToken(result) shouldBe None
      }

      "empty" in {
        val adapter = AWSFlowAdapter.nextTokenFlowAdapter(mock[AWSAsyncCall[NextTokenRequest,NextTokenResult]])
        val result = NextTokenResult(Some(""))
        result.getNextToken shouldBe ""
        adapter.getToken(result) shouldBe None
      }

      "non-empty" in {
        val adapter = AWSFlowAdapter.nextTokenFlowAdapter(mock[AWSAsyncCall[NextTokenRequest,NextTokenResult]])
        val result = NextTokenResult(Some("foo"))
        result.getNextToken shouldBe "foo"
        adapter.getToken(result) shouldBe Some("foo")
      }
    }
  }

  "the nextMarkerFlowAdapter" - {
    "processes requests" in {
      val requestProcessor = mock[AWSAsyncCall[NextMarkerRequest,NextMarkerResult]]
      val request = NextMarkerRequest(Some("foo"))
      val result = NextMarkerResult(Some("bar"))
      val jFuture = new JFuture[NextMarkerResult] {
        override def isCancelled: Boolean = false
        override def get(): NextMarkerResult = result
        override def get(timeout: Long, unit: TimeUnit): NextMarkerResult = result
        override def cancel(mayInterruptIfRunning: Boolean): Boolean = false
        override def isDone: Boolean = true
      }
      (requestProcessor.apply _).expects(request, *).returning(jFuture)
      val adapter = AWSFlowAdapter.nextMarkerFlowAdapter(requestProcessor)
      adapter.processRequest(request, null).get() shouldBe result
    }

    "sets request tokens" in {
      val adapter = AWSFlowAdapter.nextMarkerFlowAdapter(mock[AWSAsyncCall[NextMarkerRequest,NextMarkerResult]])
      val token = "foo"
      adapter.withToken(NextMarkerRequest(None), token) shouldBe NextMarkerRequest(Some(token))
    }

    "gets request tokens when underlying token is" - {
      "null" in {
        val adapter = AWSFlowAdapter.nextMarkerFlowAdapter(mock[AWSAsyncCall[NextMarkerRequest,NextMarkerResult]])
        val result = NextMarkerResult(None)
        result.getNextMarker shouldBe null
        adapter.getToken(result) shouldBe None
      }

      "empty" in {
        val adapter = AWSFlowAdapter.nextMarkerFlowAdapter(mock[AWSAsyncCall[NextMarkerRequest,NextMarkerResult]])
        val result = NextMarkerResult(Some(""))
        result.getNextMarker shouldBe ""
        adapter.getToken(result) shouldBe None
      }

      "non-empty" in {
        val adapter = AWSFlowAdapter.nextMarkerFlowAdapter(mock[AWSAsyncCall[NextMarkerRequest,NextMarkerResult]])
        val result = NextMarkerResult(Some("foo"))
        result.getNextMarker shouldBe "foo"
        adapter.getToken(result) shouldBe Some("foo")
      }
    }
  }

  "the markerFlowAdapter" - {
    "processes requests" in {
      val requestProcessor = mock[AWSAsyncCall[MarkerRequest,MarkerResult]]
      val request = MarkerRequest(Some("foo"))
      val result = MarkerResult(Some("bar"))
      val jFuture = new JFuture[MarkerResult] {
        override def isCancelled: Boolean = false
        override def get(): MarkerResult = result
        override def get(timeout: Long, unit: TimeUnit): MarkerResult = result
        override def cancel(mayInterruptIfRunning: Boolean): Boolean = false
        override def isDone: Boolean = true
      }
      (requestProcessor.apply _).expects(request, *).returning(jFuture)
      val adapter = AWSFlowAdapter.markerFlowAdapter(requestProcessor)
      adapter.processRequest(request, null).get() shouldBe result
    }

    "sets request tokens" in {
      val adapter = AWSFlowAdapter.markerFlowAdapter(mock[AWSAsyncCall[MarkerRequest,MarkerResult]])
      val token = "foo"
      adapter.withToken(MarkerRequest(None), token) shouldBe MarkerRequest(Some(token))
    }

    "gets request tokens when underlying token is" - {
      "null" in {
        val adapter = AWSFlowAdapter.markerFlowAdapter(mock[AWSAsyncCall[MarkerRequest,MarkerResult]])
        val result = MarkerResult(None)
        result.getMarker shouldBe null
        adapter.getToken(result) shouldBe None
      }

      "empty" in {
        val adapter = AWSFlowAdapter.markerFlowAdapter(mock[AWSAsyncCall[MarkerRequest,MarkerResult]])
        val result = MarkerResult(Some(""))
        result.getMarker shouldBe ""
        adapter.getToken(result) shouldBe None
      }

      "non-empty" in {
        val adapter = AWSFlowAdapter.markerFlowAdapter(mock[AWSAsyncCall[MarkerRequest,MarkerResult]])
        val result = MarkerResult(Some("foo"))
        result.getMarker shouldBe "foo"
        adapter.getToken(result) shouldBe Some("foo")
      }
    }
  }

  "the devoid utility" - {
    val request = NextTokenRequest(None)
    "will return a Java future that passes through" - {
      def prepareFuture(): (JFuture[Void], JFuture[NextTokenRequest]) = {
        val voidCall = mock[AWSAsyncCall[NextTokenRequest,Void]]
        val voidFuture = mock[JFuture[Void]]
        (voidCall.apply _).expects(request, *).returning(voidFuture)
        val handler = mock[AsyncHandler[NextTokenRequest,NextTokenRequest]]
        val devoid = AWSFlowAdapter.devoid(voidCall)
        val jFuture = devoid(request, handler)

        (voidFuture, jFuture)
      }

      "get and return the request" in {
        val (voidFuture, jFuture) = prepareFuture()
        (voidFuture.get _).expects()
        jFuture.get() shouldBe request
      }

      "get with timeout and return the request" in {
        val (voidFuture, jFuture) = prepareFuture()
        (voidFuture.get(_: Long, _: TimeUnit)).expects(4, TimeUnit.DAYS)
        jFuture.get(4, TimeUnit.DAYS) shouldBe request
      }

      "isCancelled" in {
        val (voidFuture, jFuture) = prepareFuture()
        (voidFuture.isCancelled _).expects().returning(true)
        jFuture.isCancelled shouldBe true
      }

      "isDone" in {
        val (voidFuture, jFuture) = prepareFuture()
        (voidFuture.isDone _).expects().returning(true)
        jFuture.isDone shouldBe true
      }

      "cancel" in {
        val (voidFuture, jFuture) = prepareFuture()
        (voidFuture.cancel _).expects(true).returning(true)
        jFuture.cancel(true) shouldBe true
      }
    }

    "passes results through the handler" - {
      "onSuccess" in {
        val voidCall = mock[AWSAsyncCall[NextTokenRequest,Void]]
        (voidCall.apply _).expects(request, *).onCall { (_, handler) =>
          handler.onSuccess(request, null)
          mock[JFuture[Void]]
        }
        val handler = mock[AsyncHandler[NextTokenRequest,NextTokenRequest]]
        (handler.onSuccess _).expects(request, request)
        val devoid = AWSFlowAdapter.devoid(voidCall)
        devoid(request, handler)
      }

      "onError" in {
        val dynamite = new Exception("KABOOM!")
        val voidCall = mock[AWSAsyncCall[NextTokenRequest,Void]]
        (voidCall.apply _).expects(request, *).onCall { (_, handler) =>
          handler.onError(dynamite)
          mock[JFuture[Void]]
        }
        val handler = mock[AsyncHandler[NextTokenRequest,NextTokenRequest]]
        (handler.onError _).expects(dynamite)
        val devoid = AWSFlowAdapter.devoid(voidCall)
        devoid(request, handler)
      }
    }
  }
}

object AWSFlowAdapterSpec {
  case class NextTokenRequest(token: Option[String]) extends AmazonWebServiceRequest {
    def withNextToken(token: String) = NextTokenRequest(Some(token))
  }
  case class NextTokenResult(token: Option[String]) {
    val getNextToken: String = token.orNull
  }
  case class NextMarkerRequest(marker: Option[String]) extends AmazonWebServiceRequest {
    def withMarker(token: String) = NextMarkerRequest(Some(token))
  }
  case class NextMarkerResult(marker: Option[String]) {
    val getNextMarker: String = marker.orNull
  }
  case class MarkerRequest(marker: Option[String]) extends AmazonWebServiceRequest {
    def withMarker(token: String) = MarkerRequest(Some(token))
  }
  case class MarkerResult(marker: Option[String]) {
    val getMarker: String = marker.orNull
  }
}
