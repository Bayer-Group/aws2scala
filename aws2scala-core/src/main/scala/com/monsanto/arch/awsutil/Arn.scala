package com.monsanto.arch.awsutil

import com.monsanto.arch.awsutil.partitions.Partition
import com.monsanto.arch.awsutil.regions.Region

/** Amazon Resource Names (ARNs) uniquely identify AWS resources.  These are required when you need to specify
  * a resource unambiguously across all of AWS, such as in IAM policies, Amazon Relation Database Service (Amazon
  * RDS) tags, and API calls.
  *
  * @param _partition the partition that the resource is in
  * @param _namespace the service namespace that identifies the AWS product
  * @param _region the region the resource resides in
  * @param _account the AWS account that owns the resource, without the hyphens
  */
private[awsutil] abstract class Arn(_partition: Partition,
                                    _namespace: Arn.Namespace,
                                    _region: Option[Region],
                                    _account: Option[Account]) {
  def this(namespace: Arn.Namespace, region: Option[Region], account: Account) =
    this(account.partition, namespace, region, Some(account))

  final def partition: Partition = _partition
  final def namespace: Arn.Namespace = _namespace
  final def regionOption: Option[Region] = _region
  final def accountOption: Option[Account] = _account

  /** The service-dependent content identifying the resource. */
  def resource: String

  final def value: String = s"arn:${_partition.id}:${_namespace}:${_region.map(_.name).getOrElse("")}:${_account.getOrElse("")}:$resource"

  override def toString = value
}

private[awsutil] object Arn {
  def unapply(arn: String): Option[(Partition, Arn.Namespace, Option[Region], Option[Account], String)] = {
    arn match {
      case ArnRegex(Partition(partition), Arn.Namespace(namespace), region, "", resource) ⇒
        Some((partition, namespace, Region.fromName(region), None, resource))
      case ArnRegex(Partition(partition), Arn.Namespace(namespace), region, accountId, resource) ⇒
        Some((partition, namespace, Region.fromName(region), Some(Account(accountId, partition)), resource))
      case _ ⇒ None
    }
  }

  private val ArnRegex = "^arn:([^:]+):([^:]+):([^:]*):([^:]*):(.+)$".r

  sealed abstract class Namespace(val id: String) {
    override def toString = id
  }

  object Namespace {
    case object ApiGateway extends Namespace("apigateway")
    case object AmazonAppStream extends Namespace("appstream")
    case object AutoScaling extends Namespace("autoscaling")
    case object BillingAndCostManagement extends Namespace("aws-portal")
    case object AwsCloudFormation extends Namespace("cloudformation")
    case object CloudFront extends Namespace("cloudfront")
    case object AmazonCloudSearch extends Namespace("cloudsearch")
    case object CloudTrail extends Namespace("cloudtrail")
    case object CloudWatch extends Namespace("cloudwatch")
    case object CloudWatchEvents extends Namespace("events")
    case object CloudWatchLogs extends Namespace("logs")
    case object AwsCodeCommit extends Namespace("codecommit")
    case object AwsCodeDeploy extends Namespace("codedeploy")
    case object AwsCodePipeline extends Namespace("codepipeline")
    case object AmazonCognito extends Namespace("cognito-identity")
    case object AmazonCognitoSync extends Namespace("cognito-sync")
    case object AwsConfig extends Namespace("config")
    case object AwsDataPipeline extends Namespace("datapipeline")
    case object DeviceFarm extends Namespace("devicefarm")
    case object AwsDirectConnect extends Namespace("directconnect")
    case object AwsDirectoryService extends Namespace("ds")
    case object DynamoDB extends Namespace("dynamodb")
    case object ElasticBeanstalk extends Namespace("elasticbeanstalk")
    case object AmazonVPCEC2 extends Namespace("ec2")
    case object ElasticLoadBalancing extends Namespace("elasticloadbalancing")
    case object AmazonEMR extends Namespace("elasticmapreduce")
    case object ElasticTranscoder extends Namespace("elastictranscoder")
    case object ElastiCache extends Namespace("elasticache")
    case object Elasticsearch extends Namespace("es")
    case object AmazonGameLift extends Namespace("gamelift")
    case object AmazonGlacier extends Namespace("glacier")
    case object IAM extends Namespace("iam")
    case object AWSImportExport extends Namespace("importexport")
    case object AwsKMS extends Namespace("kms")
    case object AmazonKinesis extends Namespace("kinesis")
    case object Lambda extends Namespace("lambda")
    case object AmazonML extends Namespace("machinelearning")
    case object AwsMarketplace extends Namespace("aws-marketplace")
    case object AwsMarketplaceManagementPortal extends Namespace("aws-marketplace-management")
    case object MobileAnalytics extends Namespace("mobileanalytics")
    case object AwsOpsWorks extends Namespace("opsworks")
    case object AmazonRedshift extends Namespace("redshift")
    case object AmazonRDS extends Namespace("rds")
    case object AmazonRoute53 extends Namespace("route53")
    case object AwsSTS extends Namespace("sts")
    case object AwsServiceCatalog extends Namespace("servicecatalog")
    case object AmazonSES extends Namespace("ses")
    case object AmazonSNS extends Namespace("sns")
    case object AmazonSQS extends Namespace("sqs")
    case object AmazonS3 extends Namespace("s3")
    case object AmazonSWF extends Namespace("swf")
    case object AmazonSimpleDB extends Namespace("sdb")
    case object AwsStorageGateway extends Namespace("storagegateway")
    case object AwsSupport extends Namespace("support")
    case object TrustedAdvisor extends Namespace("trustedadvisor")
    case object AwsWAF extends Namespace("waf")
    case object AmazonWorkSpaces extends Namespace("workspaces")

    val values: Seq[Namespace] = Seq(ApiGateway, AmazonAppStream, AutoScaling, BillingAndCostManagement,
      AwsCloudFormation, CloudFront, AmazonCloudSearch, CloudTrail, CloudWatch, CloudWatchEvents, CloudWatchLogs,
      AwsCodeCommit, AwsCodeDeploy, AwsCodePipeline, AmazonCognito, AmazonCognitoSync, AwsConfig, AwsDataPipeline,
      DeviceFarm, AwsDirectConnect, AwsDirectoryService, DynamoDB, ElasticBeanstalk, AmazonVPCEC2, ElasticLoadBalancing,
      AmazonEMR, ElasticTranscoder, ElastiCache, Elasticsearch, AmazonGameLift, AmazonGlacier, IAM, AWSImportExport,
      AwsKMS, AmazonKinesis, Lambda, AmazonML, AwsMarketplace, AwsMarketplaceManagementPortal, MobileAnalytics,
      AwsOpsWorks, AmazonRedshift, AmazonRDS, AmazonRoute53, AwsSTS, AwsServiceCatalog, AmazonSES, AmazonSNS,
      AmazonSQS, AmazonS3, AmazonSWF, AmazonSimpleDB, AwsStorageGateway, AwsSupport, TrustedAdvisor, AwsWAF,
      AmazonWorkSpaces)

    def fromString(str: String): Option[Namespace] = values.find(_.id == str)

    def unapply(str: String): Option[Namespace] = fromString(str)
  }
}
