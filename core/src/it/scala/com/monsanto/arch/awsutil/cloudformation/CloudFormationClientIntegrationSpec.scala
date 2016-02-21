package com.monsanto.arch.awsutil.cloudformation

import akka.stream.scaladsl.{Keep, Sink, Source}
import com.amazonaws.services.cloudformation.model._
import com.monsanto.arch.awsutil.StreamingAwsClient
import com.monsanto.arch.awsutil.cloudformation.AsyncCloudFormationClient.Implicits._
import com.monsanto.arch.awsutil.cloudformation.CloudFormationClientIntegrationSpec._
import com.monsanto.arch.awsutil.cloudformation.model.ValidatedTemplate
import com.monsanto.arch.awsutil.s3.model.BucketNameAndKey
import com.monsanto.arch.awsutil.test.AwsScalaFutures._
import com.monsanto.arch.awsutil.test.{AwsIntegrationSpec, IntegrationCleanup, IntegrationTest, TestDefaults}
import com.monsanto.arch.cloudformation.model.resource.`AWS::SNS::Topic`
import com.monsanto.arch.cloudformation.model.{ParameterRef, StringParameter, Template}
import com.typesafe.scalalogging.StrictLogging
import org.scalactic.Equality
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import spray.json.JsonWriter

@IntegrationTest
class CloudFormationClientIntegrationSpec extends FreeSpec with AwsIntegrationSpec with StrictLogging with IntegrationCleanup {
  val client = asyncAwsClient.cloudFormation
  val stackPrefix = "aws2scala-it-cf"
  val stackName = s"$stackPrefix-$testId"
  var stackId: String = _

  "the default AsyncCloudFormationClient" - {
    "can create a stack" in {
      logger.info(s"Creating stack with name ‘$stackName’")
      val request = new CreateStackRequest()
        .withTags(TestDefaults.Tags)
        .withParameters(Parameters)
        .withStackName(stackName)
        .withTemplateBody(testStackTemplate(stackName))
        .withOnFailure(OnFailure.DELETE)
      stackId = client.createStack(request).futureValue
      stackId should not be empty
      logger.debug(s"Stack created with ID ‘$stackId‘")
    }

    "can list all stacks" in {
      val stackSummaries = client.listStacks().futureValue
      (stackSummaries should contain (stackId)) (decided by theStackSummaryId)
    }

    "can list stacks filtered using" - {
      val filter = Seq(StackStatus.CREATE_COMPLETE, StackStatus.CREATE_IN_PROGRESS)

      "stack status objects" in {
        val stackSummaries = client.listStacks(filter).futureValue
        (stackSummaries should contain (stackId)) (decided by theStackSummaryId)
      }

      "strings" in {
        val stringFilter: Seq[String] = filter.map(_.toString)
        val stackSummaries = client.listStacks(stringFilter).futureValue
        (stackSummaries should contain (stackId)) (decided by theStackSummaryId)
      }
    }

    "can describe all of the stacks" in {
      val stacks = client.describeStacks().futureValue
      (stacks should contain (stackId)) (decided by theStackId)
    }

    "can describe a given stack" in {
      val stack = client.describeStack(stackId).futureValue
      stack.getStackName shouldBe stackName
    }

    "can describe the events for a stack" in {
      val stackEvents = client.describeStackEvents(stackName).futureValue
      stackEvents should not be empty
    }

    "can validate templates" - {
      val body = testStackTemplate(stackName)
      val expectedValidatedTemplate = {
        val cftgParameter = testTemplateParameter(stackName)
        val awsParameter = new TemplateParameter()
          .withDefaultValue(cftgParameter.Default.get)
          .withDescription(cftgParameter.Description.get)
          .withNoEcho(false)
          .withParameterKey(TopicDisplayName)

        ValidatedTemplate(Some(TemplateDescription), Seq.empty, None, Seq(awsParameter))
      }

      "from a body" in {
        val validatedTemplate = client.validateTemplateBody(body).futureValue
        validatedTemplate shouldBe expectedValidatedTemplate
      }

      "from a bucket" in {
        val s3 = StreamingAwsClient.Default.s3
        val asyncS3 = StreamingAwsClient.Default.asyncClient.s3
        val s3Key = "template.json"
        try {
          val validatedTemplate =
            Source.single(stackName)
              .via(s3.bucketCreator)
              .map(bucket ⇒ (BucketNameAndKey(bucket.getName, s3Key), body))
              .via(s3.uploader)
              .map(BucketNameAndKey.fromObjectSummary)
              .via(s3.objectUrlGetter)
              .mapAsync(1)(url ⇒ client.validateTemplateURL(url))
              .toMat(Sink.head)(Keep.right).run()
          validatedTemplate.futureValue shouldBe expectedValidatedTemplate
        } finally {
          asyncS3.emptyAndDeleteBucket(stackName)
        }
      }
    }

    "can describe the resources in a stack" in {
      val result = client.listStackResources(stackName).futureValue
      (result should contain (SNSTopicName)) (decided by theLogicalResourceID)
    }

    "can delete the stack" in {
      logger.info(s"Deleting stack with name ‘$stackName’")
      val result = client.deleteStack(stackName)
      result.futureValue shouldBe stackName
      logger.debug("Successfully request stack deletion.")
    }

    behave like cleanupStacks(stackPrefix)
    behave like cleanupS3Buckets(stackPrefix)
  }

  private val theStackId = new Equality[Stack] {
    override def areEqual(a: Stack, b: Any): Boolean = {
      b match {
        case s: String => a.getStackId == s
      }
    }
  }

  private val theStackSummaryId = new Equality[StackSummary] {
    override def areEqual(a: StackSummary, b: Any): Boolean = {
      b match {
        case s: String => a.getStackId == s
      }
    }
  }

  private val theLogicalResourceID = new Equality[StackResourceSummary] {
    override def areEqual(a: StackResourceSummary, b: Any): Boolean = {
      b match {
        case s: String => a.getLogicalResourceId == s
      }
    }
  }
}

object CloudFormationClientIntegrationSpec {
  val TemplateDescription = "DefaultCloudFormationClientSpec template"
  val TopicDisplayName = "TopicDisplayName"
  val Parameters = Map(TopicDisplayName -> "delete-me-too")
  val SNSTopicName = "TestSNSTopic"

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
        `AWS::SNS::Topic`(SNSTopicName,
          DisplayName = Some(ParameterRef(topicDisplayNameParameter)),
          Subscription = None,
          TopicName = None
        ))),
      Routables = None,
      Outputs = None)

    implicitly[JsonWriter[Template]].write(template).prettyPrint
  }
}
