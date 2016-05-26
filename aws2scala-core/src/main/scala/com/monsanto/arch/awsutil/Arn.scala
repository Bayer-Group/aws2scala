package com.monsanto.arch.awsutil

import com.monsanto.arch.awsutil.identitymanagement.model.{RoleArn, SamlProviderArn, UserArn}
import com.monsanto.arch.awsutil.partitions.Partition
import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.securitytoken.model.AssumedRoleArn

import scala.collection.mutable

/** Amazon Resource Names (ARNs) uniquely identify AWS resources.  These are required when you need to specify
  * a resource unambiguously across all of AWS, such as in IAM policies, Amazon Relation Database Service (Amazon
  * RDS) tags, and API calls.
  *
  * @param _partition the partition that the resource is in
  * @param _namespace the service namespace that identifies the AWS product
  * @param _region the region the resource resides in
  * @param _account the AWS account that owns the resource, without the hyphens
  */
abstract class Arn(_partition: Partition,
                   _namespace: Arn.Namespace,
                   _region: Option[Region],
                   _account: Option[Account]) {
  /** Allows creation of an ARN where the partition is derived from the account. */
  def this(namespace: Arn.Namespace, region: Option[Region], account: Account) =
    this(account.partition, namespace, region, Some(account))

  /** Returns the ARN’s partition. */
  final def partition: Partition = _partition
  /** Returns the ARN’s namespace. */
  final def namespace: Arn.Namespace = _namespace
  /** Returns the ARN’s region, if any. */
  final def regionOption: Option[Region] = _region
  /** Returns the ARN’s account, if any. */
  final def accountOption: Option[Account] = _account

  /** Returns the service-dependent content identifying the resource. */
  def resource: String

  /** Returns the string representation of the ARN. */
  final def arnString: String =
    s"arn:${_partition.id}:${_namespace.id}:${_region.map(_.name).getOrElse("")}:${_account.map(_.id).getOrElse("")}:$resource"
}

object Arn {
  private[awsutil] type ArnParts = (Partition, Arn.Namespace, Option[Region], Option[Account], String)

  /** The set of all possible partial functions that can extract an `Arn` subclass given the components of an ARN. */
  private val arnPartialFunctions: mutable.Set[PartialFunction[ArnParts,Arn]] =
    mutable.LinkedHashSet(
      AccountArn.accountArnPF,
      AssumedRoleArn.assumeRoleArnPF,
      RoleArn.roleArnPF,
      SamlProviderArn.samlProviderArnPF,
      UserArn.userArnPF
    )

  /** Registers partial functions that can be used to extract `Arn` subclasses given a set of ARN parts. */
  private[awsutil] def registerArnPartialFunctions(partialFunctions: PartialFunction[ArnParts,Arn]*): Unit = {
    arnPartialFunctions.synchronized {
      arnPartialFunctions ++= partialFunctions
    }
  }


  /** Generic ARN subclass that may be used when no registered matchers match a parsed ARN. */
  private[awsutil] case class GenericArn(_partition: Partition,
                                         _namespace: Arn.Namespace,
                                         _region: Option[Region],
                                         _account: Option[Account],
                                         resource: String) extends Arn(_partition, _namespace, _region, _account)

  /** Enumerated type for ARN namespaces. */
  sealed abstract class Namespace(val id: String)

  /** Utility to extract/build an `Arn` instance from a string containing an ARN. */
  object fromArnString {
    def apply(arnString: String): Arn =
      unapply(arnString).getOrElse(throw new IllegalArgumentException(s"’$arnString‘ is not a valid ARN."))

    /** Given a string, extract an ARN object instance.  This extractor will attempt to return an `Arn` subclass
      * that is specific to the ARN (if such a subclass has registered an applicable partial function).  If no
      * such registered partial function exists, but the string is still an ARN, a generic ARN instance will
      * be returned.
      */
    def unapply(arnString: String): Option[Arn] = {
      // first, try to get the parts of the ARN
      val maybeArnParts = arnString match {
        // no region or account
        case ArnRegex(Partition(partition), Arn.Namespace.fromId(namespace), "", "", resource) ⇒
          Some((partition, namespace, None, None, resource))

        // region, but no account
        case ArnRegex(Partition(partition), Arn.Namespace.fromId(namespace), Region(region), "", resource) ⇒
          Some((partition, namespace, Some(region), None, resource))

        // account, but no region
        case ArnRegex(Partition(partition), Arn.Namespace.fromId(namespace), "", accountId, resource) ⇒
          Some((partition, namespace, None, Some(Account(accountId, partition)), resource))

        // account and region
        case ArnRegex(Partition(partition), Arn.Namespace.fromId(namespace), Region(region), accountId, resource) ⇒
          Some((partition, namespace, Some(region), Some(Account(accountId, partition)), resource))

        case _ ⇒ None
      }
      // now, extract an arn by finding it in the registered extractors, ending with GenericArn if no extractors
      // are found.
      maybeArnParts.flatMap { arnParts ⇒
        arnPartialFunctions.synchronized {
          arnPartialFunctions.view
            .filter(_.isDefinedAt(arnParts))
            .map(_.apply(arnParts))
            .headOption
            .orElse(Some((GenericArn.apply _).tupled.apply(arnParts)))
        }
      }
    }

    /** Regular expression to match the parts of a regular expression. */
    private val ArnRegex = "^arn:([^:]+):([^:]+):([^:]*):([^:]*):(.+)$".r
  }

  //noinspection SpellCheckingInspection
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

    /** Extractor to get a Namespace from a namespace identifier. */
    object fromId {
      def unapply(str: String): Option[Namespace] = values.find(_.id == str)
    }
  }
}
