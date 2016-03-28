package com.monsanto.arch.awsutil.cloudformation

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.cloudformation.{model ⇒ aws}
import com.monsanto.arch.awsutil.StreamingAwsClient
import com.monsanto.arch.awsutil.cloudformation.model.{DeleteStackRequest, ValidatedTemplate}

trait StreamingCloudFormationClient extends StreamingAwsClient {
  /** Creates an Akka flow that will take as input [[com.amazonaws.services.cloudformation.model.CreateStackRequest CreateStackRequest]]
    * instances and emit for each one a string with the resulting ID of the stack being created.
    */
  def stackCreator: Flow[aws.CreateStackRequest, String, NotUsed]

  /** Creates an Akka flow that will take a filter and emit stack summaries that match the filter. */
  def stackLister: Flow[Seq[aws.StackStatus], aws.StackSummary, NotUsed]

  /** Creates a flow that will describe all stacks if passed a `None` or just stack with the given name or ID in a
    * `Some`.
    */
  def stackDescriber: Flow[Option[String], aws.Stack, NotUsed]

  /** Creates a flow that will emit all events for a given stack name or ID. */
  def stackEventsDescriber: Flow[String, aws.StackEvent, NotUsed]

  /** Creates an Akka flow that will take as input a delete stack request, request their deletion, and emit the stack
    * name (or ID) of the stack that will be deleted.
    */
  def stackDeleter: Flow[DeleteStackRequest, String, NotUsed]

  /** Creates an Akka flow that takes template validation requests and emits validated template results. */
  def templateValidator: Flow[aws.ValidateTemplateRequest, ValidatedTemplate, NotUsed]

  /** Creates an Akka flow that given a stack name or ID will emit summaries for all of its resources. */
  def stackResourceLister : Flow[String, aws.StackResourceSummary, NotUsed]
}
