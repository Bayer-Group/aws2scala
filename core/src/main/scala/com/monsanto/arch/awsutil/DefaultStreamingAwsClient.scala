package com.monsanto.arch.awsutil

import java.util.concurrent.{ExecutorService, Executors, TimeUnit}

import akka.util.Timeout
import com.amazonaws.auth.{AWSCredentialsProvider, DefaultAWSCredentialsProviderChain}
import com.amazonaws.services.cloudformation.AmazonCloudFormationAsyncClient
import com.amazonaws.services.ec2.AmazonEC2AsyncClient
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementAsyncClient
import com.amazonaws.services.kms.AWSKMSAsyncClient
import com.amazonaws.services.rds.AmazonRDSAsyncClient
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceAsyncClient
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import com.monsanto.arch.awsutil.cloudformation.DefaultStreamingCloudFormationClient
import com.monsanto.arch.awsutil.ec2.DefaultStreamingEC2Client
import com.monsanto.arch.awsutil.identitymanagement.DefaultStreamingIdentityManagementClient
import com.monsanto.arch.awsutil.impl.{ShutdownFreeExecutorServiceWrapper, ShutdownHook}
import com.monsanto.arch.awsutil.kms.DefaultStreamingKMSClient
import com.monsanto.arch.awsutil.rds.DefaultStreamingRDSClient
import com.monsanto.arch.awsutil.s3.DefaultStreamingS3Client
import com.monsanto.arch.awsutil.securitytoken.DefaultStreamingSecurityTokenServiceClient
import com.monsanto.arch.awsutil.sns.DefaultStreamingSNSClient
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext

private[awsutil] class DefaultStreamingAwsClient(private[awsutil] val settings: AwsSettings,
                                                 private[awsutil] val credentialsProvider: AWSCredentialsProvider,
                                                 private[awsutil] val executorService: ExecutorService) extends StreamingAwsClient with LazyLogging {
  /** Create a new streaming client using the default credentials provider change and a fixed thread pool. */
  def this(settings: AwsSettings) = this(settings, new DefaultAWSCredentialsProviderChain, Executors.newFixedThreadPool(50))

  /** A list of callbacks that should called to ensure that all clients have shut down. */
  private[awsutil] val shutdownHooks = ListBuffer.empty[ShutdownHook]

  private def registerShutdownHook(hook: ShutdownHook): Unit = {
    shutdownHooks.synchronized {
      shutdownHooks += hook
    }
  }

  /** The objects hereunder allow for lazy loading of AWS class dependencies.  This allows aws2scala to be shipped
    * with ’provided‘ dependencies on the AWS Java SDK.  As such, clients need only pull in the dependencies they
    * actually need.
    */
  private object Init {
    object CloudFormation {
      def apply() = {
        val aws = new AmazonCloudFormationAsyncClient(credentialsProvider, executorService)
        aws.setRegion(settings.region)
        registerShutdownHook(ShutdownHook.clientHook("CloudFormation", aws))
        new DefaultStreamingCloudFormationClient(aws)
      }
    }

    object EC2 {
      def apply() = {
        val aws = new AmazonEC2AsyncClient(credentialsProvider, executorService)
        aws.setRegion(settings.region)
        registerShutdownHook(ShutdownHook.clientHook("EC2", aws))
        new DefaultStreamingEC2Client(aws)
      }
    }

    object IAM {
      def apply() = {
        val aws = new AmazonIdentityManagementAsyncClient(credentialsProvider, executorService)
        aws.setRegion(settings.region)
        registerShutdownHook(ShutdownHook.clientHook("IAM", aws))
        new DefaultStreamingIdentityManagementClient(aws)
      }
    }

    object KMS {
      def apply() = {
        val aws = new AWSKMSAsyncClient(credentialsProvider, executorService)
        aws.setRegion(settings.region)
        registerShutdownHook(ShutdownHook.clientHook("KMS", aws))
        new DefaultStreamingKMSClient(aws)
      }
    }

    object RDS {
      def apply() = {
        val aws = new AmazonRDSAsyncClient(credentialsProvider, executorService)
        aws.setRegion(settings.region)
        registerShutdownHook(ShutdownHook.clientHook("RDS", aws))
        new DefaultStreamingRDSClient(aws)
      }
    }

    object S3 {
      def apply() = {
        val as3 = new AmazonS3Client(credentialsProvider)
        val atm = new TransferManager(as3, executorService)
        as3.setRegion(settings.region)
        registerShutdownHook(ShutdownHook.s3Hook(atm))
        val executionContext = ExecutionContext.fromExecutorService(executorService)
        new DefaultStreamingS3Client(as3, atm, settings)(executionContext)
      }
    }

    object SNS {
      def apply() = {
        val aws = new AmazonSNSAsyncClient(credentialsProvider, executorService)
        aws.setRegion(settings.region)
        registerShutdownHook(ShutdownHook.clientHook("SNS", aws))
        new DefaultStreamingSNSClient(aws)
      }
    }

    object STS {
      def apply() = {
        val aws = new AWSSecurityTokenServiceAsyncClient(credentialsProvider, executorService)
        aws.setRegion(settings.region)
        registerShutdownHook(ShutdownHook.clientHook("STS", aws))
        new DefaultStreamingSecurityTokenServiceClient(aws)
      }
    }
  }

  override lazy val cloudFormation = Init.CloudFormation()

  override lazy val ec2 = Init.EC2()

  override lazy val identityManagement = Init.IAM()

  override lazy val kms = Init.KMS()

  override lazy val rds = Init.RDS()

  override lazy val s3 = Init.S3()

  override lazy val securityTokenService = Init.STS()

  override lazy val sns = Init.SNS()

  override def shutdown(timeout: Timeout): Unit = {
    logger.info("Commencing shutdown of StreamingAwsClient")
    // allow submitted tasks to terminate gracefully by requesting
    executorService.shutdown()
    try {
      // wait for things to die gracefully
      if (executorService.awaitTermination(timeout.duration.toNanos, TimeUnit.NANOSECONDS)) {
        logger.info("Executor service terminated gracefully")
      } else {
        logger.info("Executor service did not terminate gracefully.")
      }
    } catch {
      case _: InterruptedException =>
        logger.warn("Shutdown of executor service was interrupted")
    }
    // now, we get ugly
    executorService.shutdownNow()
    // ensure that all instantiated AWS clients are properly shut down
    shutdownHooks.synchronized {
      shutdownHooks.foreach(_.shutdown())
    }
  }

  override val asyncClient: AsyncAwsClient = new DefaultAsyncAwsClient(this)

  override def withCredentialsProvider(credentialsProvider: AWSCredentialsProvider) = {
    val childExecutorService = new ShutdownFreeExecutorServiceWrapper(executorService)
    val child = new DefaultStreamingAwsClient(settings, credentialsProvider, childExecutorService)
    registerShutdownHook(ShutdownHook.childClientHook(child))
    child
  }
}
