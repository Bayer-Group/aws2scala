package com.monsanto.arch.awsutil.cloudformation

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.cloudformation.{AmazonCloudFormationAsync, model ⇒ aws}
import com.monsanto.arch.awsutil.cloudformation.model.AwsConverters._
import com.monsanto.arch.awsutil.cloudformation.model.{DeleteStackRequest, ValidatedTemplate}
import com.monsanto.arch.awsutil.{AWSFlow, AWSFlowAdapter}

import scala.collection.JavaConverters._

private[cloudformation] class DefaultStreamingCloudFormationClient(client: AmazonCloudFormationAsync) extends StreamingCloudFormationClient {
  override val stackCreator =
    Flow[aws.CreateStackRequest]
      .via[aws.CreateStackResult,NotUsed](AWSFlow.simple(client.createStackAsync))
      .map(_.getStackId)
      .named("CloudFormation.stackCreator")

  override val stackLister =
    Flow[Seq[aws.StackStatus]]
      .map { filter => new aws.ListStacksRequest().withStackStatusFilters(filter: _*) }
      .via[aws.ListStacksResult,NotUsed](AWSFlow.pagedByNextToken(client.listStacksAsync))
      .mapConcat(_.getStackSummaries.asScala.toList)
      .named("CloudFormation.stackLister")

  override val stackDescriber =
    Flow[Option[String]]
      .map { maybeStackName =>
        val req = new aws.DescribeStacksRequest
        maybeStackName.foreach(name ⇒ req.setStackName(name))
        req
      }
      .via[aws.DescribeStacksResult,NotUsed](AWSFlow.pagedByNextToken(client.describeStacksAsync))
      .mapConcat(_.getStacks.asScala.toList)
      .named("CloudFormation.stackDescriber")

  override val stackEventsDescriber =
    Flow[String]
      .map(stackNameOrId ⇒ new aws.DescribeStackEventsRequest().withStackName(stackNameOrId))
      .via[aws.DescribeStackEventsResult,NotUsed](AWSFlow.pagedByNextToken(client.describeStackEventsAsync))
      .mapConcat(_.getStackEvents.asScala.toList)
      .named("CloudFormation.stackEventsDescriber")

  override val stackDeleter =
    Flow[DeleteStackRequest]
      .map(_.asAws)
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput(client.deleteStackAsync)))
      .map(_.getStackName)
      .named("CloudFormation.stackDeleter")

  override val templateValidator =
    Flow[aws.ValidateTemplateRequest]
      .via[aws.ValidateTemplateResult,NotUsed](AWSFlow.simple(client.validateTemplateAsync))
      .map(r ⇒ ValidatedTemplate(r))
      .named("CloudFormation.templateValidator")

  override val stackResourceLister =
    Flow[String]
      .map(x ⇒ new aws.ListStackResourcesRequest().withStackName(x))
      .via[aws.ListStackResourcesResult,NotUsed](AWSFlow.pagedByNextToken(client.listStackResourcesAsync))
      .mapConcat(_.getStackResourceSummaries.asScala.toList)
      .named("CloudFormation.stackResourceLister")
}
