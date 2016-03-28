package com.monsanto.arch.awsutil.impl

import java.util.concurrent.{Future ⇒ JFuture}

import akka.stream._
import akka.stream.stage._
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.{AmazonClientException, AmazonWebServiceRequest}
import com.monsanto.arch.awsutil.AWSFlowAdapter
import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Success, Try}

/** A custom stream processor that for each request emits one or more results based on whether each result indicates
  * that further requests are necessary.   The implementation here uses a state machine in order to make the logic
  * as easy as possible to understand.  The states the stage can be in are:
  *
  *  * Idle, indicating that there is no pending activity AWS requests or results.
  *  * AwaitingAws, indicating that the stage is waiting for a result from AWS.
  *  * CachedResult, indicating we have a result from AWS but downstream has not yet requested it.
  *  * CachedResultWithNextRequest, indicating we have a result with a next page from AWS but downstream has not yet
  *    requested it.
  *  * Finished, indicating that the stage has finished working either due to downstream cancellation or upstream
  *    completion.
  *  * Failed, indicating that the stage has failed either due to an upstream failure or because AWS failed.
  *
  * For each state, there are up to eight possible inputs, though not every input is valid for every state:
  *
  *  1. `onPush`, when upstream submits a new request
  *  1. `onPull`, when downstream requests a result
  *  1. `onAwsError`, when AWS replies with an Amazon-specific error
  *  1. `onAsyncError`, when AWS replies with some other error
  *  1. `onAsyncResult`, when AWS replies with a result
  *  1. `onAsyncResultWithNextRequest`, when AWS replies with a result that indicates further requests are necessary
  *  1. `onUpstreamFinish`, indicating that upstream has completed
  *  1. `onDownstreamFinish`, indicating that downstream has cancelled
  *  1. `onUpstreamFailure`, indicating that upstream has failed
  *
  * A couple notes about the implementation:
  *
  *  1. The handling of `onAsyncError`, `onDownstreamFinish`, and `onUpstreamFailure` are not state-dependent so are
  *     not reflected in each individual state.
  *  1. In the cases of when downstream cancels or upstream fails, the stage will attempt to cancel any pending AWS
  *     requests.  However, there is no way to ensure the callback will not be invoked.
  *
  * @author Daniel Solano Gómez
  */
private[awsutil] class AWSGraphStage[Request <: AmazonWebServiceRequest,Result](adapter: AWSFlowAdapter[Request,Result])
      extends GraphStage[FlowShape[Request, Either[AmazonClientException,Result]]] with LazyLogging {
  /** The input to the stage. */
  private val in: Inlet[Request] = Inlet("AWSGraphStage.in")
  /** The output from the stage. */
  private val out: Outlet[Either[AmazonClientException,Result]] = Outlet("AWSGraphStage.out")

  override val shape: FlowShape[Request, Either[AmazonClientException,Result]] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new Logic(shape)

  /** Encapsulates the logic for the stage. */
  private class Logic(shape: Shape) extends GraphStageLogic(shape) {
    /** The handler that manages responses from AWS. */
    private val awsHandler = new AWSHandler(getAsyncCallback(onAwsInput))
    /** The state of the stage. */
    private var state: State = Idle
    /** Possible handle to the Java future returned by AWS, used for cancellation. */
    private var awsFuture: Option[JFuture[Result]] = None

    // manages input from upstream
    setHandler(in, new InHandler {
      // defer to state
      override def onPush(): Unit = state.doOnPush()

      // defer to state
      override def onUpstreamFinish(): Unit = state.doOnUpstreamFinish()

      // Always fail on upstream failure, cancelling AWS future if any
      override def onUpstreamFailure(ex: Throwable): Unit = {
        cancelAwsFuture()
        failStage(ex)
        transition(Failed(ex), s"onUpstreamFailure(${ex.getMessage}")
      }
    })

    // manages input from downstream
    setHandler(out, new OutHandler {
      // defer to state
      override def onPull(): Unit = state.doOnPull()

      // Always complete on downstream cancellation, cancelling AWS future if any
      override def onDownstreamFinish(): Unit = {
        cancelAwsFuture()
        completeStage()
        transition(Finished, "onDownstreamFinish")
      }
    })

    /** Utility for cancelling the active AWS operation, if any. */
    private def cancelAwsFuture(): Unit = {
      awsFuture.foreach(_.cancel(true))
      awsFuture = None
    }

    /** Utility for submitting an operation to AWS. */
    private def processRequest(request: Request): Unit = {
      assert(awsFuture.isEmpty)
      awsFuture = Some(adapter.processRequest(request, awsHandler))
    }


    /** Trait for defining the current state of the stage. */
    sealed trait State {
      // $COVERAGE-OFF$ Default functionality should never be called

      /** Handles upstream input. */
      def doOnPush(): Unit = notAllowed("onPush")
      /** Handles downstream demand. */
      def doOnPull(): Unit = notAllowed("onPull")
      /** Handles an AWS result. */
      def doOnAsyncResult(result: Result): Unit = notAllowed(s"onAsyncResult($result)")
      /** Handles an AWS result that has a next page. */
      def doOnAsyncResultWithNextRequest(result: Result, nextRequest: Request): Unit = notAllowed(s"onAsyncResult($result, $nextRequest)")
      /** Handles an AWS error. */
      def doOnAwsError(exception: AmazonClientException): Unit = notAllowed(s"onAwsError($exception)")
      /** Handles upstream signalling it is done. */
      def doOnUpstreamFinish(): Unit = notAllowed("onUpstreamFinish")

      /** Default implementation that signals that the operation was not expected for the state. */
      private def notAllowed(method: String) = {
        val msg = s"$this.$method: not allowed in this state"
        val cause = new IllegalStateException(msg)
        logger.error(msg)
        failStage(cause)
      }

      // $COVERAGE-ON$
    }

    /** The stage is idle when it is not waiting for a result from AWS and does not have a result pending downstream
      * demand.
      */
    case object Idle extends State {
      /** When upstream submits something, begin processing it. */
      override def doOnPush(): Unit = {
        assert(awsFuture.isEmpty)
        val request = grab(in)
        processRequest(request)
        transition(AwaitingAws(request), s"onPush($request)")
      }

      /** There is nothing to do when downstream asks for something. */
      override def doOnPull(): Unit = transition(this, "onPull")

      /** If upstream completes, then complete the stage. */
      override def doOnUpstreamFinish(): Unit = {
        transition(Finished, "onUpstreamFinish")
        completeStage()
      }
    }

    /** State for when the stage is waiting for a response for AWS. */
    case class AwaitingAws(request: Request) extends State {
      /** Cannot send anything downstream, yet. */
      override def doOnPull(): Unit = transition(this, "onPull")

      /** When we get a result from AWS, we either send it downstream or cache it.  If we send it downstream, then we
        * can either complete if upstream is done, or ask for more input from upstream.
        */
      override def doOnAsyncResult(result: Result): Unit = {
        val method: String = s"onAsyncResult($result)"
        if (isAvailable(out)) {
          push(out, Right(result))
          if (isClosed(in)) {
            completeStage()
            transition(Finished, method)
          } else {
            assert(!hasBeenPulled(in) && !isAvailable(in))
            pull(in)
            transition(Idle, method)
          }
        } else {
          transition(CachedResult(Right(result)), method)
        }
      }


      /** Handles an AWS error. */
      override def doOnAwsError(exception: AmazonClientException) = {
        val method: String = s"onAwsError($exception)"
        if (isAvailable(out)) {
          push(out, Left(exception))
          if (isClosed(in)) {
            completeStage()
            transition(Finished, method)
          } else {
            assert(!hasBeenPulled(in) && !isAvailable(in))
            pull(in)
            transition(Idle, method)
          }
        } else {
          transition(CachedResult(Left(exception)), method)
        }
      }

      /** When we get a result from AWS that requires a next page, we either send it downstream or cache it.  If we
        * send it downstream, then we can go ahead and make the new request.
        */
      override def doOnAsyncResultWithNextRequest(result: Result, nextRequest: Request): Unit = {
        val method = s"onAsyncResultWithNextRequest($result, $nextRequest)"
        if (isAvailable(out)) {
          push(out, Right(result))
          processRequest(nextRequest)
          transition(AwaitingAws(nextRequest), method)
        } else {
          transition(CachedResultWithNextRequest(result, nextRequest), method)
        }
      }

      /** Absorb upstream termination since we are waiting for AWS. */
      override def doOnUpstreamFinish(): Unit = transition(this, "onUpstreamFinish")
    }

    /** This state occurs when we have a result from AWS that has not been consumed downstream.  Also, there is no next
      * page to request.
      */
    case class CachedResult(result: Either[AmazonClientException,Result]) extends State {
      /** Finally, downstream wants the data, so push it downstream.  If upstream is complete, then we can finish.
        * Otherwise, we request for more data from upstream.
        */
      override def doOnPull(): Unit = {
        push(out, result)
        if (isClosed(in)) {
          completeStage()
          transition(Finished, "onPull")
        } else {
          assert(!isAvailable(in) && !hasBeenPulled(in))
          pull(in)
          transition(Idle, "onPull")
        }
      }

      /** Absorb termination since we still have data to deliver. */
      override def doOnUpstreamFinish(): Unit = transition(this, "onUpstreamFinish")
    }

    /** Occurs when we have a result from AWS that downstream must consume.  We also have a request to AWS that we need
      * to perform before handling any more input from upstream or shutting down.
      */
    case class CachedResultWithNextRequest(result: Result, nextRequest: Request) extends State {
      /** When demand for the data comes, send it and ask AWS to process the next request. */
      override def doOnPull(): Unit = {
        push(out, Right(result))
        processRequest(nextRequest)
        transition(AwaitingAws(nextRequest), "onPull")
      }

      /** Absorb termination since we have work to do. */
      override def doOnUpstreamFinish(): Unit = transition(this, "onUpstreamFinish")
    }

    /** Indicates that the stage is complete. */
    case object Finished extends State

    /** Indicates that the stage has failed. */
    case class Failed(cause: Throwable) extends State

    /** Utility for changing state and logging it. */
    @inline private def transition(nextState: State, method: String): Unit = {
      logger.trace(s"$state.$method -> $nextState")
      state = nextState
    }

    /** Ask for input at start uo. */
    override def preStart(): Unit = pull(in)

    /** Handles input from AWS.  If it is a success, determines if there is a next page or not and calls the
      * appropriate method on the stage.  If it was an error, then fail the stage.
      */
    private def onAwsInput(input: Try[(Request,Result)]): Unit = {
      assert(awsFuture.isDefined)
      awsFuture = None
      input match {
        case Failure(awsError: AmazonClientException) ⇒
          state.doOnAwsError(awsError)
        case Failure(cause) ⇒
          failStage(cause)
          transition(Failed(cause), s"onAwsError(${cause.getMessage})")
        case Success((request, result)) ⇒
          adapter.getToken(result) match {
            case None =>
              state.doOnAsyncResult(result)
            case Some(token) =>
              state.doOnAsyncResultWithNextRequest(result, adapter.withToken(request, token))
          }
      }
    }

    /** Handles callbacks from AWS so that they get submitted to the stage in a thread-safe way. */
    private class AWSHandler(callback: AsyncCallback[Try[(Request,Result)]]) extends AsyncHandler[Request,Result] {
      override def onError(exception: Exception) = callback.invoke(Failure(exception))
      override def onSuccess(request: Request, result: Result): Unit = callback.invoke(Success((request, result)))
    }
  }
}
