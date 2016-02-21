package com.monsanto.arch.awsutil.test

import java.util.Date

import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import com.amazonaws.services.cloudformation.model.StackStatus
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest
import com.amazonaws.services.sqs.AmazonSQSClient
import com.monsanto.arch.awsutil.identitymanagement.model.{DetachRolePolicyRequest, ListAttachedRolePoliciesRequest, ListRolesRequest}
import com.monsanto.arch.awsutil.impl.AkkaStreamUtils.Implicits._
import com.monsanto.arch.awsutil.sns.model.ListSubscriptionsRequest
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.FreeSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

import scala.collection.JavaConverters._
import scala.concurrent.duration.DurationInt

trait IntegrationCleanup { this: FreeSpec with StrictLogging with AwsIntegrationSpec ⇒
  implicit protected def materialiser: Materializer
  private val oneHourAgo = new Date(System.currentTimeMillis() - IntegrationCleanup.OneHourMillis)
  private implicit val patienceConfig = ScalaFutures.PatienceConfig(5.minutes, 5.seconds)

  protected def cleanupS3Buckets(prefix: String): Unit = {
    "cleans up old S3 buckets" in {
      val deletedCount =
        streamingAwsClient.s3.bucketLister
          .filter(_.getName.startsWith(prefix))
          .filter(_.getCreationDate.before(oneHourAgo))
          .map(_.getName)
          .buffer(100, OverflowStrategy.backpressure)
          .map { b ⇒ logger.info(s"Removing old bucket: $b"); b }
          .via(streamingAwsClient.s3.bucketEmptierAndDeleter)
          .runWith(Sink.count)
          .futureValue
      logger.info(s"Deleted $deletedCount old buckets")
    }
  }

  protected def cleanupStacks(prefix: String): Unit = {
    "cleans up old stacks" in {
      val deletedCount =
        Source.single(Seq(StackStatus.CREATE_COMPLETE, StackStatus.DELETE_FAILED))
          .via(streamingAwsClient.cloudFormation.stackLister)
          .filter { summary ⇒
            summary.getCreationTime.before(oneHourAgo) && summary.getStackName.startsWith(prefix)
          }
          .map(_.getStackName)
          .buffer(100, OverflowStrategy.backpressure)
          .map { n ⇒ logger.info(s"Removing old stack: $n"); n }
          .via(streamingAwsClient.cloudFormation.stackDeleter)
          .runWith(Sink.count)
          .futureValue
      logger.info(s"Deleted $deletedCount old stacks")
    }
  }

  protected def cleanupDBInstances(prefix: String): Unit = {
    "cleans up old DB instances" in {
      val deletedCount =
        Source.single(new DescribeDBInstancesRequest)
          .via(streamingAwsClient.rds.rawDbInstanceDescriber)
          .filter { dbInstance ⇒
            dbInstance.getDBInstanceStatus == "available" &&
              dbInstance.getInstanceCreateTime.before(oneHourAgo) &&
              dbInstance.getDBInstanceIdentifier.startsWith(prefix)
          }
          .map(_.getDBInstanceIdentifier)
          .buffer(100, OverflowStrategy.backpressure)
          .map { n ⇒ logger.info(s"Removing old DB instance: $n"); (n, None) }
          .via(streamingAwsClient.rds.dbInstanceDeleter)
          .runWith(Sink.count)
          .futureValue
      logger.info(s"Deleted $deletedCount old DB instances")
    }
  }

  protected def cleanupSNSSubscriptions(prefix: String): Unit = {
    "cleans up old SNS subscriptions" in {
      val Timestamp = s"^arn:aws:sns:[^:]+:[^:]+:$prefix-([0-9]+)-[0-9a-f]+$$".r
      val deletedCount =
        Source.single(ListSubscriptionsRequest.allSubscriptions)
          .via(streamingAwsClient.sns.subscriptionLister)
          .filter(_.arn.isDefined)
          .map(_.arn.get)
          .filter {
            case Timestamp(ts) ⇒
              val created = new Date(ts.toLong)
              created.before(oneHourAgo)
            case _ ⇒ false
          }
          .buffer(100, OverflowStrategy.backpressure)
          .map { arn ⇒ logger.info(s"Removing old SNS subscription: $arn"); arn }
          .via(streamingAwsClient.sns.unsubscriber)
          .runWith(Sink.count)
          .futureValue
      logger.info(s"Deleted $deletedCount old SNS subscriptions")
    }
  }

  protected def cleanupSNSTopics(prefix: String): Unit = {
    "cleans up old SNS topics" in {
      val Timestamp = s"^arn:aws:sns:[^:]+:[^:]+:$prefix-([0-9]+)-[0-9a-f]+$$".r
      val deletedCount =
        streamingAwsClient.sns.topicLister
          .filter {
            case Timestamp(ts) ⇒
              val created = new Date(ts.toLong)
              created.before(oneHourAgo)
            case _ ⇒ false
          }
          .buffer(100, OverflowStrategy.backpressure)
          .map { arn ⇒ logger.info(s"Removing old SNS topic: $arn"); arn }
          .via(streamingAwsClient.sns.topicDeleter)
          .runWith(Sink.count)
          .futureValue
      logger.info(s"Deleted $deletedCount old SNS topics")
    }
  }

  protected def cleanupSNSPlatformApplications(prefix: String): Unit = {
    "cleans up old SNS platform applications" in {
      val Timestamp = s"^arn:aws:sns:[^:]+:[^:]+:app/[^/]+/$prefix-([0-9]+)-[0-9a-f]+$$".r
      val deletedCount =
        streamingAwsClient.sns.platformApplicationLister
          .map(_.arn)
          .filter {
            case Timestamp(ts) ⇒
              val created = new Date(ts.toLong)
              created.before(oneHourAgo)
            case _ ⇒ false
          }
          .buffer(100, OverflowStrategy.backpressure)
          .map { arn ⇒ logger.info(s"Removing old SNS platform application: $arn"); arn }
          .via(streamingAwsClient.sns.platformApplicationDeleter)
          .runWith(Sink.count)
          .futureValue
      logger.info(s"Deleted $deletedCount old SNS platform applications")
    }
  }

  protected def cleanupSQSQueues(prefix: String): Unit = {
    "cleans up old SQS queues" in {
      // TODO: get credentials from main client
      val sqs = new AmazonSQSClient()
      try {
        val queueUrls = sqs.listQueues(prefix).getQueueUrls.asScala
        val deletedCount =
          queueUrls.map { queueUrl ⇒
            val createdSeconds = sqs.getQueueAttributes(queueUrl, Seq("CreatedTimestamp").asJava).getAttributes.get("CreatedTimestamp").toInt
            val createdAt = new Date(createdSeconds * 1000L)
            if (createdAt.before(oneHourAgo)) {
              logger.info(s"Removing old SQS queue: $queueUrl")
              sqs.deleteQueue(queueUrl)
              Some(queueUrl)
            } else {
              None
            }
          }.count(_.isDefined)
        logger.info(s"Deleted $deletedCount old SQS queues")
      } finally sqs.shutdown()
    }
  }

  protected def cleanupIAMRoles(pathPrefix: String): Unit = {
    "cleans up old IAM roles" in {
      val iam = streamingAwsClient.identityManagement
      val deletedCount =
        Source.single(ListRolesRequest.withPathPrefix(pathPrefix))
          .via(iam.roleLister)
          .filter(role ⇒ role.created.before(oneHourAgo))
          .buffer(100, OverflowStrategy.backpressure)
          .map { role ⇒ logger.info(s"Found old IAM role: ${role.name}"); role }
          .flatMapConcat { role ⇒
            Source.single(ListAttachedRolePoliciesRequest(role.name))
              .via(iam.attachedRolePolicyLister)
              .map { policy ⇒
                logger.info(s"Detaching ${policy.name} from ${role.name}")
                DetachRolePolicyRequest(role.name, policy.arn)
              }
              .via(iam.rolePolicyDetacher)
              .fold(0)((count, _) ⇒ count + 1)
              .map { count ⇒ logger.info(s"Detached $count policies from ${role.name}"); role.name }
          }
          .map { roleName ⇒ logger.info(s"Removing old IAM role: $roleName"); roleName }
          .via(iam.roleDeleter)
          .runWith(Sink.count)
          .futureValue
      logger.info(s"Deleted $deletedCount old IAM roles")
    }
  }
}

object IntegrationCleanup {
  val OneHourMillis = 60 * 60 * 1000
}
