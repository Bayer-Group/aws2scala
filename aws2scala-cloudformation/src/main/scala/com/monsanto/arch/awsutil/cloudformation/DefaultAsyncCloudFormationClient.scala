package com.monsanto.arch.awsutil.cloudformation

import java.net.URL

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.cloudformation.model._
import com.monsanto.arch.awsutil.cloudformation.AsyncCloudFormationClient.StackStatusConvertible

import scala.concurrent.Future

private[cloudformation] class DefaultAsyncCloudFormationClient(streamingClient: StreamingCloudFormationClient) extends AsyncCloudFormationClient {
  override def createStack(request: CreateStackRequest)(implicit m: Materializer) =
    Source.single(request)
      .via(streamingClient.stackCreator)
      .runWith(Sink.head)

  override def listStacks[T](statuses: Seq[T] = Seq.empty[StackStatus])
                                  (implicit toStackStatus: StackStatusConvertible[T], m: Materializer) =
    Source.single(statuses.map(toStackStatus))
      .via(streamingClient.stackLister)
      .runWith(Sink.seq)

  override def describeStacks()(implicit m: Materializer): Future[Seq[Stack]] =
    Source.single(None)
      .via(streamingClient.stackDescriber)
      .runWith(Sink.seq)

  override def describeStack(stackNameOrID: String)(implicit m: Materializer) =
    Source.single(Some(stackNameOrID))
      .via(streamingClient.stackDescriber)
      .runWith(Sink.head)

  override def describeStackEvents(stackNameOrID: String)(implicit m: Materializer) =
    Source.single(stackNameOrID)
      .via(streamingClient.stackEventsDescriber)
      .runWith(Sink.seq)

  override def deleteStack(stackNameOrID: String)(implicit m: Materializer) =
    Source.single(stackNameOrID)
      .via(streamingClient.stackDeleter)
      .runWith(Sink.head)

  override def validateTemplateBody(body: String)(implicit m: Materializer) =
    Source.single(new ValidateTemplateRequest().withTemplateBody(body))
      .via(streamingClient.templateValidator)
      .runWith(Sink.head)

  override def validateTemplateURL(url: URL)(implicit m: Materializer) =
    Source.single(new ValidateTemplateRequest().withTemplateURL(url.toString))
      .via(streamingClient.templateValidator)
      .runWith(Sink.head)

  override def listStackResources(stackNameOrID: String)(implicit m: Materializer) =
    Source.single(stackNameOrID)
      .via(streamingClient.stackResourceLister)
      .runWith(Sink.seq)
}
