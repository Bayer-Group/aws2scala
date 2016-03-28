package com.monsanto.arch.awsutil.cloudformation

import java.net.URL
import java.util.{Date, UUID}

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsync
import com.amazonaws.services.cloudformation.model._
import com.monsanto.arch.awsutil.cloudformation.AsyncCloudFormationClient.Implicits._
import com.monsanto.arch.awsutil.cloudformation.DefaultCloudFormationClientSpec._
import com.monsanto.arch.awsutil.cloudformation.model.ValidatedTemplate
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.{AwsMockUtils, Materialised}
import com.monsanto.arch.cloudformation.model.resource.{`AWS::IAM::Group`, `AWS::SNS::Topic`}
import com.monsanto.arch.cloudformation.model.{ParameterRef, StringParameter, Template}
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers._
import org.scalatest._
import spray.json.JsonWriter

import scala.collection.JavaConverters._

class DefaultCloudFormationClientSpec extends FreeSpec with MockFactory with Materialised with AwsMockUtils {
  val stackName = s"aws2scala-it-cf-${UUID.randomUUID()}"
  val stackId = "some:stack:id"

  case class Fixture(awsClient: AmazonCloudFormationAsync,
                     streamingClient: StreamingCloudFormationClient,
                     asyncClient: AsyncCloudFormationClient)

  private def withFixture(test: Fixture => Any): Unit = {
    val awsClient = mock[AmazonCloudFormationAsync]
    val streamingClient = new DefaultStreamingCloudFormationClient(awsClient)
    val asyncClient = new DefaultAsyncCloudFormationClient(streamingClient)
    test(Fixture(awsClient, streamingClient, asyncClient))
  }

  case class ListStacksFixture(awsClient: AmazonCloudFormationAsync,
                               asyncClient: AsyncCloudFormationClient,
                               stackSummaries: Seq[StackSummary])

  private def withListStacksFixture(filter: Seq[StackStatus])(test: ListStacksFixture => Any) = {
    withFixture { baseFixture =>
      import baseFixture._

      val stackSummaries = Seq.tabulate(20) { n =>
        val uuid = UUID.randomUUID()
        val name = s"stack-$uuid"
        new StackSummary()
          .withStackId(s"arn:aws:cloudformation:us-east-1:$n:$name")
          .withCreationTime(new Date())
          .withStackName(name)
          .withStackStatus(StackStatus.CREATE_COMPLETE)
      }

      (awsClient.listStacksAsync(_: ListStacksRequest, _: AsyncHandler[ListStacksRequest, ListStacksResult]))
        .expects(whereRequest(_.getStackStatusFilters.asScala == filter.map(_.toString)))
        .withAwsSuccess(new ListStacksResult().withStackSummaries(stackSummaries.asJavaCollection))

      test(ListStacksFixture(awsClient, asyncClient, stackSummaries))
    }
  }


  "the default AsyncCloudFormationClient" - {
    "can create a stack" in withFixture { f =>
      val request = new CreateStackRequest()
        .withTags(Tags)
        .withParameters(Parameters)
        .withStackName(stackName)
        .withTemplateBody(testStackTemplate(stackName))
        .withOnFailure(OnFailure.DELETE)
        .withCapabilities(Capability.CAPABILITY_IAM)
      (f.awsClient.createStackAsync(_: CreateStackRequest, _: AsyncHandler[CreateStackRequest,CreateStackResult]))
        .expects(request, *)
        .withAwsSuccess(new CreateStackResult().withStackId(stackId))

      val result = f.asyncClient.createStack(request).futureValue
      result shouldBe stackId
    }

    "can list all stacks" in withListStacksFixture(Seq.empty) { f =>
      val result = f.asyncClient.listStacks().futureValue
      result shouldBe f.stackSummaries
    }

    "can list stacks filtered using" - {
      val filter = Seq(StackStatus.CREATE_COMPLETE, StackStatus.CREATE_IN_PROGRESS)

      "stack status objects" in withListStacksFixture(filter) { f =>
        val stackSummaries = f.asyncClient.listStacks(filter).futureValue
        stackSummaries shouldBe f.stackSummaries
      }

      "strings" in withListStacksFixture(filter) { f =>
        val stringFilter: Seq[String] = filter.map(_.toString)
        val stackSummaries = f.asyncClient.listStacks(stringFilter).futureValue
        stackSummaries shouldBe f.stackSummaries
      }
    }

    "can describe all of the stacks" in withFixture { f =>
      val stacks = Seq.tabulate(20) { n =>
        val uuid = UUID.randomUUID()
        val name = s"stack-$uuid"
        new Stack()
          .withStackId(s"arn:aws:cloudformation:us-east-1:$n:$name")
          .withCreationTime(new Date())
          .withStackName(name)
          .withStackStatus(StackStatus.CREATE_COMPLETE)
      }
      (f.awsClient.describeStacksAsync(_: DescribeStacksRequest, _: AsyncHandler[DescribeStacksRequest, DescribeStacksResult]))
        .expects(whereRequest(request ⇒ request.getStackName == null && request.getNextToken == null))
        .withAwsSuccess(new DescribeStacksResult().withStacks(stacks: _*))

      val result = f.asyncClient.describeStacks().futureValue
      result shouldBe stacks
    }

    "can describe a given stack" in withFixture { f =>
      val stack = new Stack()
        .withStackId(stackId)
        .withCreationTime(new Date())
        .withStackName(stackName)
        .withStackStatus(StackStatus.CREATE_COMPLETE)
      (f.awsClient.describeStacksAsync(_: DescribeStacksRequest, _: AsyncHandler[DescribeStacksRequest, DescribeStacksResult]))
        .expects(whereRequest(request ⇒ request.getStackName == stackId && request.getNextToken == null))
        .withAwsSuccess(new DescribeStacksResult().withStacks(Seq(stack): _*))

      val result = f.asyncClient.describeStack(stackId).futureValue
      result shouldBe stack
    }

    "can describe the events for a stack" in withFixture { f =>
      val stackEvents = Seq.tabulate(20) { n =>
        new StackEvent()
          .withEventId(n.toString)
          .withStackId(stackId)
          .withStackName(stackName)
          .withTimestamp(new Date())
      }
      (f.awsClient.describeStackEventsAsync(_: DescribeStackEventsRequest, _: AsyncHandler[DescribeStackEventsRequest, DescribeStackEventsResult]))
        .expects(whereRequest(request ⇒ request.getStackName == stackName && request.getNextToken == null))
        .withAwsSuccess(new DescribeStackEventsResult().withStackEvents(stackEvents: _*))

      val result = f.asyncClient.describeStackEvents(stackName).futureValue
      result shouldBe stackEvents
    }

    "can validate templates" - {
      val body = testStackTemplate(stackName)
      val awsParameter = {
        val cftgParameter = testTemplateParameter(stackName)
        new TemplateParameter()
          .withDefaultValue(cftgParameter.Default.get)
          .withDescription(cftgParameter.Description.get)
          .withNoEcho(false)
          .withParameterKey(TopicDisplayName)
      }
      val capabilitiesReason = "The following resource(s) require capabilities: [AWS::IAM::Group]"
      val expectedValidatedTemplate = ValidatedTemplate(Some(TemplateDescription), Seq(Capability.CAPABILITY_IAM),
        Some(capabilitiesReason), Seq(awsParameter))

      "from a body" in withFixture { f =>
        (f.awsClient.validateTemplateAsync(_: ValidateTemplateRequest, _: AsyncHandler[ValidateTemplateRequest,ValidateTemplateResult]))
          .expects(whereRequest(request ⇒ request.getTemplateBody == body && request.getTemplateURL == null))
          .withAwsSuccess(
            new ValidateTemplateResult()
              .withDescription(TemplateDescription)
              .withParameters(Seq(awsParameter): _*)
              .withCapabilities(Capability.CAPABILITY_IAM)
              .withCapabilitiesReason(capabilitiesReason))

        val validatedTemplate = f.asyncClient.validateTemplateBody(body).futureValue
        validatedTemplate shouldBe expectedValidatedTemplate
      }

      "from a bucket" in withFixture { f =>
        val s3url = new URL("https://s3.amazonaws.com/some-bucket/template.json")
        (f.awsClient.validateTemplateAsync(_: ValidateTemplateRequest, _: AsyncHandler[ValidateTemplateRequest,ValidateTemplateResult]))
          .expects(whereRequest(request ⇒ request.getTemplateBody == null && request.getTemplateURL == s3url.toString))
          .withAwsSuccess(
              new ValidateTemplateResult()
                .withDescription(TemplateDescription)
                .withParameters(Seq(awsParameter): _*)
                .withCapabilities(Capability.CAPABILITY_IAM)
                .withCapabilitiesReason(capabilitiesReason))

        val validatedTemplate = f.asyncClient.validateTemplateURL(s3url).futureValue
        validatedTemplate shouldBe expectedValidatedTemplate
      }
    }

    "can list stack resources" in withFixture { f ⇒
      val stackResourceSummaries = Seq.tabulate(20) { i ⇒
        new StackResourceSummary()
          .withLogicalResourceId(s"resource$i")
      }

      (f.awsClient.listStackResourcesAsync(_: ListStackResourcesRequest, _: AsyncHandler[ListStackResourcesRequest, ListStackResourcesResult]))
        .expects(whereRequest(_.getStackName == stackName))
        .withAwsSuccess(new ListStackResourcesResult().withStackResourceSummaries(stackResourceSummaries.asJavaCollection))

      val result = f.asyncClient.listStackResources(stackName).futureValue
      result shouldBe stackResourceSummaries
    }

    "can delete the stack" in withFixture { f =>
      (f.awsClient.deleteStackAsync(_: DeleteStackRequest, _: AsyncHandler[DeleteStackRequest,Void]))
        .expects(whereRequest(_.getStackName == stackName))
        .withVoidAwsSuccess()

      val result = f.asyncClient.deleteStack(stackName).futureValue
      result shouldBe stackName
    }
  }
}

object DefaultCloudFormationClientSpec {
  val TemplateDescription = "DefaultCloudFormationClientSpec template"
  val TopicDisplayName = "TopicDisplayName"
  val Tags = Map("mon:TagA" -> "foo", "mon:TagB" -> "bar")
  val Parameters = Map(TopicDisplayName -> "delete-me-too")

  def testTemplateParameter(stackName: String): StringParameter = {
    StringParameter(
      TopicDisplayName,
      "The display name for the topic generated as part of this stack.",
      s"delete-me-$stackName")
  }

  def testStackTemplate(stackName: String): String = {
    val topicDisplayNameParameter = testTemplateParameter(stackName)

    val template = Template(
      Description = TemplateDescription,
      Parameters = Some(Seq(topicDisplayNameParameter)),
      Conditions = None,
      Mappings = None,
      Resources = Some(Seq(
        `AWS::SNS::Topic`("TestSNSTopic",
          DisplayName = Some(ParameterRef(topicDisplayNameParameter)),
          Subscription = None,
          TopicName = None
        ),
        `AWS::IAM::Group`("TestGroup")
      )),
      Routables = None,
      Outputs = None)

    implicitly[JsonWriter[Template]].write(template).prettyPrint
  }
}
