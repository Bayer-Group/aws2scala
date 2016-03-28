package com.monsanto.arch.awsutil.ec2

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.cloudformation.model.{Output ⇒ _, _}
import com.monsanto.arch.awsutil.cloudformation.AsyncCloudFormationClient.Implicits._
import com.monsanto.arch.awsutil.cloudformation.CloudFormation
import com.monsanto.arch.awsutil.ec2.EC2ClientIntegrationSpec._
import com.monsanto.arch.awsutil.ec2.model.{Instance, KeyPair, KeyPairInfo}
import com.monsanto.arch.awsutil.test_support.AwsScalaFutures._
import com.monsanto.arch.awsutil.test_support._
import com.monsanto.arch.cloudformation.model._
import com.monsanto.arch.cloudformation.model.resource._
import com.typesafe.scalalogging.StrictLogging
import org.scalactic.Equality
import org.scalatest.FreeSpec
import org.scalatest.Matchers._

import scala.collection.JavaConverters._
import scala.concurrent.duration.DurationInt

@IntegrationTest
class EC2ClientIntegrationSpec extends FreeSpec with AwsIntegrationSpec with StrictLogging with IntegrationCleanup {
  val streamingEC2 = awsClient.streaming(EC2)
  val asyncEC2 = awsClient.async(EC2)

  val streamingCloudFormation = awsClient.streaming(CloudFormation)
  val asyncCloudFormation = awsClient.async(CloudFormation)

  val testPrefix = "aws2scala-it-ec2"
  val stackName = s"$testPrefix-$testId"
  val instanceName = s"$InstanceName-$stackName"
  var keyPair: KeyPair = _
  lazy val keyPairInfo = KeyPairInfo(keyPair.name, keyPair.fingerprint)
  var stackID: String = _
  var instanceId: String = _

  val keyName = s"aws2scala-it-ec2-testkey-$testId"

  "the EC2 client" - {
    "can create a key" in {
      keyPair = asyncEC2.createKeyPair(keyName).futureValue
      keyPair.name shouldBe keyName
    }

    "can find a key" - {
      "in a listing" in {
        val result = asyncEC2.describeKeyPairs().futureValue

        result should contain (keyPairInfo)
      }

      "in a filtered listing" in {
        val result = asyncEC2.describeKeyPairs(Map("fingerprint" → Seq(keyPair.fingerprint))).futureValue
        result should contain (keyPairInfo)
      }

      "by name" in {
        val result = asyncEC2.describeKeyPair(keyName).futureValue
        result shouldBe Some(keyPairInfo)
      }
    }

    "can handle not finding a key pair" in {
      val result = asyncEC2.describeKeyPair(s"$keyName-not-found").futureValue
      result shouldBe None
    }

    "creating a stack using generated key" in {
      val createStackRequest = new CreateStackRequest()
        .withOnFailure(OnFailure.DELETE)
        .withTemplateBody(EC2ClientIntegrationSpec.MinimalEC2Template)
        .withStackName(stackName)
        .withParameters(Map("KeyName" → keyName))
        .withTags(TestDefaults.Tags)
      stackID = asyncCloudFormation.createStack(createStackRequest).futureValue
      logger.info(s"Creating stack $stackName")

      val completedStatuses = Set(StackStatus.CREATE_COMPLETE, StackStatus.DELETE_COMPLETE)

      val stack =
        Source.tick(5.seconds, 5.seconds, Some(stackName))
          .via(streamingCloudFormation.stackDescriber)
          .map { s ⇒
            logger.info(s"Stack status is now ${s.stackStatus}")
            s
          }
          .filter(s ⇒ completedStatuses(s.stackStatus))
          .take(1)
          .runWith(Sink.head)
          .futureValue(StackPatience)

      stack.stackStatus shouldBe StackStatus.CREATE_COMPLETE

      logger.info(s"Finished creating stack $stackName")

      val outputs = stack.getOutputs.asScala
      outputs should have size 1

      val output = outputs.head
      output.getOutputKey shouldBe InstanceIDOutputKey
      instanceId = output.getOutputValue
      logger.info(s"EC2 instance ID is $instanceId")
    }

    "can describe instances" - {
      val theInstanceId = new Equality[Instance] {
        override def areEqual(a: Instance, b: Any): Boolean = {
          b match {
            case id: String ⇒ a.id == id
          }
        }
      }

      "all of them" in {
        val result = asyncEC2.describeInstances().futureValue
        (result should contain (instanceId)) (decided by theInstanceId)
      }

      "using a filter" in {
        val filter = Map("key-name" → Seq(keyName))
        val result = asyncEC2.describeInstances(filter).futureValue
        (result should contain (instanceId)) (decided by theInstanceId)
      }

      "by ID" in {
        val result = asyncEC2.describeInstance(instanceId).futureValue
        result shouldBe defined
        result.get.id shouldBe instanceId
      }
    }

    "can handle not finding an instance" in {
      val result = asyncEC2.describeKeyPair("i-ffffffff").futureValue
      result shouldBe None
    }

    "remove the stack" in {
      logger.info(s"Removing stack $stackName")

      val deleteComplete = Source.single(stackName)
        .via(streamingCloudFormation.stackDeleter)
        .flatMapConcat { stackName ⇒
          Source.tick(5.seconds, 5.seconds, Some(stackID))
            .via(streamingCloudFormation.stackDescriber)
            .map { s ⇒
              logger.info(s"Stack status is now ${s.stackStatus}")
              s
            }
            .filter(_.stackStatus == StackStatus.DELETE_COMPLETE)
            .take(1)
        }
        .runWith(Sink.head)
        .futureValue(StackPatience)

      deleteComplete.getStackName shouldBe stackName

      logger.info(s"Deleted stack $stackName")
    }

    "can delete a key" in {
      val deleteResult = asyncEC2.deleteKeyPair(keyName).futureValue
      deleteResult shouldBe keyName

      val findResult = asyncEC2.describeKeyPairs(Map("key-name" → Seq(keyName))).futureValue
      findResult shouldBe empty
    }

    behave like cleanupStacks(testPrefix)
  }
}

object EC2ClientIntegrationSpec {
  val InstanceIDOutputKey = "TestInstanceID"
  val InstanceName = "TestInstance"
  val Tags = TestDefaults.Tags.map(tag ⇒ AmazonTag(tag._1, tag._2)).toSeq

  val MinimalEC2Template = {
    val keyName = `AWS::EC2::KeyPair::KeyName_Parameter`("KeyName",
      Description = Some("Name of an existing EC2 KeyPair to enable SSH access to the instance"),
      ConstraintDescription = Some("must be the name of an existing EC2 KeyPair."))

    val vpc = `AWS::EC2::VPC`("TestVPC",
      CidrBlock = CidrBlock(10, 0, 0, 0, 16),
      Tags = Tags
    )

    val subnet = `AWS::EC2::Subnet`("TestSubnet",
      VpcId = vpc,
      AvailabilityZone = Some("us-east-1a"),
      CidrBlock = CidrBlock(10, 0, 0, 0, 24),
      Tags = Tags
    )

    val instance = `AWS::EC2::Instance`(InstanceName,
      InstanceType = "t2.micro",
      KeyName = ParameterRef(keyName),
      ImageId = AMIId("ami-60b6c60a"),
      SubnetId = subnet,
      Tags = Tags ++ AmazonTag.fromName(InstanceName))

    val instanceOutput = Output(InstanceIDOutputKey,
      Description = "Instance ID of the EC2 instance",
      Value = ResourceRef(instance)
    )

    val template = Template(
      Description = "Minimal EC2 instance template for aws2scala/DefaultEC2ClientIntegrationSpec",
      Parameters = Some(Seq(keyName)),
      Conditions = None,
      Mappings = None,
      Resources = Some(Seq(vpc, subnet, instance)),
      Routables = None,
      Outputs = Some(Seq(instanceOutput)))

    Template.format.write(template).prettyPrint
  }

  private val StackPatience = AwsScalaFutures.PatienceConfig(5.minutes, 5.seconds)
}
