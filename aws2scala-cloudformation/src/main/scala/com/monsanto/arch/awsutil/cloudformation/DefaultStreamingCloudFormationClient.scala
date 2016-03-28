package com.monsanto.arch.awsutil.cloudformation

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsync
import com.amazonaws.services.cloudformation.model._
import com.monsanto.arch.awsutil.cloudformation.model.ValidatedTemplate
import com.monsanto.arch.awsutil.{AWSFlow, AWSFlowAdapter}

import scala.collection.JavaConverters._

private[cloudformation] class DefaultStreamingCloudFormationClient(client: AmazonCloudFormationAsync) extends StreamingCloudFormationClient {
  override val stackCreator =
    Flow[CreateStackRequest]
      .via[CreateStackResult,NotUsed](AWSFlow.simple(client.createStackAsync))
      .map(_.getStackId)
      .named("CloudFormation.stackCreator")

  override val stackLister =
    Flow[Seq[StackStatus]]
      .map { filter => new ListStacksRequest().withStackStatusFilters(filter: _*) }
      .via[ListStacksResult,NotUsed](AWSFlow.pagedByNextToken(client.listStacksAsync))
      .mapConcat(_.getStackSummaries.asScala.toList)
      .named("CloudFormation.stackLister")

  override val stackDescriber =
    Flow[Option[String]]
      .map { maybeStackName =>
        val req = new DescribeStacksRequest
        maybeStackName.foreach(name ⇒ req.setStackName(name))
        req
      }
      .via[DescribeStacksResult,NotUsed](AWSFlow.pagedByNextToken(client.describeStacksAsync))
      .mapConcat(_.getStacks.asScala.toList)
      .named("CloudFormation.stackDescriber")

  override val stackEventsDescriber =
    Flow[String]
      .map(stackNameOrId ⇒ new DescribeStackEventsRequest().withStackName(stackNameOrId))
      .via[DescribeStackEventsResult,NotUsed](AWSFlow.pagedByNextToken(client.describeStackEventsAsync))
      .mapConcat(_.getStackEvents.asScala.toList)
      .named("CloudFormation.stackEventsDescriber")

  override val stackDeleter =
    Flow[String]
      .map(stackName => new DeleteStackRequest().withStackName(stackName))
      .via(AWSFlow.simple(AWSFlowAdapter.devoid(client.deleteStackAsync)))
      .map(_.getStackName)
      .named("CloudFormation.stackDeleter")

  override val templateValidator =
    Flow[ValidateTemplateRequest]
      .via[ValidateTemplateResult,NotUsed](AWSFlow.simple(client.validateTemplateAsync))
      .map(r ⇒ ValidatedTemplate(r))
      .named("CloudFormation.templateValidator")

  override val stackResourceLister =
    Flow[String]
      .map(x ⇒ new ListStackResourcesRequest().withStackName(x))
      .via[ListStackResourcesResult,NotUsed](AWSFlow.pagedByNextToken(client.listStackResourcesAsync))
      .mapConcat(_.getStackResourceSummaries.asScala.toList)
      .named("CloudFormation.stackResourceLister")
}
