package com.monsanto.arch.awsutil.auth.policy

import com.monsanto.arch.awsutil.identitymanagement.model.{RoleArn, SamlProviderArn, UserArn}
import com.monsanto.arch.awsutil.securitytoken.model.AssumedRoleArn
import com.monsanto.arch.awsutil.{Account, AccountArn}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

/** A principal specifies the user (IAM user, federated user, or assumed-role user), AWS account, AWS service, or other
  * principal entity that it allowed or denied access to a resource.
  *
  * ==Specifying a principal==
  *
  * To specify a principal, use one of the following methods:
  *
  *  - [[Principal.allUsers]] to match everyone, including anonymous users
  *  - [[Principal.account]] to specify an individual AWS account
  *  - [[Principal.iamUser]] to match a specific IAM user
  *  - [[Principal.webProvider]] to match federated users from a specific service
  *  - [[Principal.samlProvider]] to match federated users from a SAML provider
  *  - [[Principal.iamRole]] to match an IAM role
  *  - [[Principal.stsAssumedRole]] to match a specific assumed-role user
  *  - [[Principal.service]] to match an AWS service
  */
sealed trait Principal {
  /** Returns the provider for this principal, which indicates in what group of users this principal resides. */
  def provider: String
  /** Returns the unique ID for this principal. */
  def id: String
}

object Principal {
  /** Principal that includes all AWS web services. */
  val allServices: Principal = service(Service.AllServices)

  /** Principal that includes all the web identity providers. */
  val allWebProviders: Principal = webProvider(WebIdentityProvider.AllProviders)

  /** Principal that includes all users, including anonymous users. */
  val allUsers: Principal = AllUsers

  /** Returns a principal for the given AWS account. */
  def account(account: Account): Principal = AccountPrincipal(account)

  /** Returns a principal for the given AWS service. */
  def service(service: Service): Principal = ServicePrincipal(service)

  /** Returns a principal for federated users authenticated by the given provider. */
  def webProvider(webIdentityProvider: WebIdentityProvider): Principal = WebProviderPrincipal(webIdentityProvider)

  /** Returns a principal for users from a SAML identity provider.
    *
    * @param samlProviderArn the ARN of the SAML provider for which to generate a principal
    */
  def samlProvider(samlProviderArn: SamlProviderArn): Principal = SamlProviderPrincipal(samlProviderArn)

  /** Returns a principal for the given IAM user.
    *
    * @param userArn the ARN of the user for which to generate a principal
    */
  def iamUser(userArn: UserArn): Principal = IamUserPrincipal(userArn)

  /** Returns a principal for the given IAM role.
    *
    * @param roleArn the ARN of the role for which to generate a principal
    */
  def iamRole(roleArn: RoleArn): Principal = IamRolePrincipal(roleArn)

  /** Returns a principal for a specific IAM assumed-role user.
    *
    * @param assumedRoleArn the ARN of the assumed role session for which to generate a principal
    */
  def stsAssumedRole(assumedRoleArn: AssumedRoleArn): Principal = StsAssumedRolePrincipal(assumedRoleArn)

  /** Base class for all possible service values.
    *
    * @param id uniquely identifies the service
    */
  sealed abstract class Service(val id: String)

  //noinspection SpellCheckingInspection
  object Service {
    /** Tags a [[com.monsanto.arch.awsutil.auth.policy.Principal.Service Service]] to denote that there is an AWS
      * enumeration value that corresponds to it.
      */
    sealed trait AwsEnumerated { this: Service ⇒ }

    /** Matches all Amazon services. */
    case object AllServices extends Service("*") with AwsEnumerated

    /** Matches the Amazon API Gateway service. */
    case object AmazonAPIGateway extends Service("apigateway.amazonaws.com")

    /** Matches the Amazon Elastic Compute Cloud (EC2) service. */
    case object AmazonEC2 extends Service("ec2.amazonaws.com") with AwsEnumerated

    /** Matches the Amazon EC2 Container Service (ECS). */
    case object AmazonEC2ContainerService extends Service("ecs.amazonaws.com")

    /** Matches the Amazon EC2 Spot Fleet service. */
    case object AmazonEC2SpotFleet extends Service("spotfleet.amazonaws.com")

    /** Matches the Amazon Elastic MapReduce service. */
    case object AmazonElasticMapReduce extends Service("elasticmapreduce.amazonaws.com")

    /** Matches the Amazon Elastic Transcoder service. */
    case object AmazonElasticTranscoder extends Service("elastictranscoder.amazonaws.com") with AwsEnumerated

    /** Matches the Amazon Inspector service. */
    case object AmazonInspector extends Service("inspector.amazonaws.com")

    /** Matches the Amazon VPC Flow Logs service. */
    case object AmazonVPCFlowLogs extends Service("vpc-flow-logs.amazonaws.com")

    /** Matches the Amazon WorkSpaces service. */
    case object AmazonWorkSpaces extends Service("workspaces.amazonaws.com")

    /** Matches the AWS CloudHSM service. */
    case object AWSCloudHSM extends Service("cloudhsm.amazonaws.com") with AwsEnumerated

    /** Matches the AWS CloudTrail service. */
    case object AWSCloudTrail extends Service("cloudtrail.amazonaws.com")

    /** Matches the AWS Config service. */
    case object AWSConfig extends Service("config.amazonaws.com")

    /** Matches the AWS Data Pipeline service. */
    case object AWSDataPipeline extends Service("datapipeline.amazonaws.com") with AwsEnumerated

    /** Matches the AWS Lambda service. */
    case object AWSLambda extends Service("lambda.amazonaws.com")

    /** Matches the AWS OpsWorks service. */
    case object AWSOpsWorks extends Service("opsworks.amazonaws.com") with AwsEnumerated

    /** Matches the AWS Service Catalog service. */
    case object AWSServiceCatalog extends Service("servicecatalog.amazonaws.com")

    /** This is used in cases where we need a `Service` instance that is not one of the enumerated types. */
    private[awsutil] case class GenericService(_id: String) extends Service(_id)

    /** All valid values for the enumeration. */
    val values: Seq[Service] =
      Seq(AllServices, AmazonAPIGateway, AmazonEC2, AmazonEC2ContainerService, AmazonEC2SpotFleet,
        AmazonElasticMapReduce, AmazonElasticTranscoder, AmazonInspector, AmazonVPCFlowLogs, AmazonWorkSpaces,
        AWSCloudHSM, AWSCloudTrail, AWSConfig, AWSDataPipeline, AWSLambda, AWSOpsWorks, AWSServiceCatalog)

    object fromId {
      def unapply(id: String): Option[Service] = values.find(_.id.equalsIgnoreCase(id))
    }
  }

  sealed abstract class WebIdentityProvider(val provider: String)

  object WebIdentityProvider {
    case object AllProviders extends WebIdentityProvider("*")
    case object Amazon extends WebIdentityProvider("www.amazon.com")
    case object Facebook extends WebIdentityProvider("graph.facebook.com")
    case object Google extends WebIdentityProvider("accounts.google.com")

    val values: Seq[WebIdentityProvider] = Seq(AllProviders, Amazon, Facebook, Google)

    object fromProvider {
      def unapply(name: String): Option[WebIdentityProvider] = values.find(_.provider.equalsIgnoreCase(name))
    }
  }

  /** Utility to build/extract `Principal` instances from a provider name and identifier. */
  object fromProviderAndId {
    /** Returns a `Principal` instance that corresponds to the given provider name and identifier.
      *
      * @param provider indicates in what group of users the principal resides
      * @param id       the unique identifier for the principal
      * @throws java.lang.IllegalArgumentException if no Principal can be constructed that matches the given
      *                                            provider and identifier
      */
    def apply(provider: String, id: String): Principal =
      unapply((provider, id))
        .getOrElse(throw new IllegalArgumentException(s"($provider, $id) does not resolve to a valid principal."))

    /** Extracts a principal given a tuple containing the provider name and identifier. */
    def unapply(providerAndId: (String, String)): Option[Principal] = {
      providerAndId match {
        case ("*", "*") ⇒
          Some(Principal.AllPrincipals)
        case ("AWS", "*") ⇒
          Some(Principal.allUsers)
        case ("Service", "*") ⇒
          Some(Principal.allServices)
        case ("Service", Principal.Service.fromId(service)) ⇒
          Some(Principal.service(service))
        case ("Service", serviceId) ⇒
          val logger = Logger(LoggerFactory.getLogger(classOf[Principal].getName))
          logger.warn(s"Using GenericService for service ID: $serviceId")
          Some(Principal.service(Principal.Service.GenericService(serviceId)))
        case ("Federated", "*") ⇒
          Some(Principal.allWebProviders)
        case ("Federated", Principal.WebIdentityProvider.fromProvider(webIdentityProvider)) ⇒
          Some(Principal.webProvider(webIdentityProvider))
        case ("Federated", SamlProviderArn.fromArnString(samlProviderArn)) ⇒
          Some(Principal.SamlProviderPrincipal(samlProviderArn))
        case ("AWS", AccountArn.fromArnString(AccountArn(account))) ⇒
          Some(Principal.AccountPrincipal(account))
        case ("AWS", Account.fromNumber(account)) ⇒
          Some(Principal.AccountPrincipal(account))
        case ("AWS", UserArn.fromArnString(userArn)) ⇒
          Some(Principal.IamUserPrincipal(userArn))
        case ("AWS", RoleArn.fromArnString(roleArn)) ⇒
          Some(Principal.IamRolePrincipal(roleArn))
        case ("AWS", AssumedRoleArn.fromArnString(assumedRoleArn)) ⇒
          Some(Principal.StsAssumedRolePrincipal(assumedRoleArn))
        case _ ⇒
          None
      }
    }
  }

  private[awsutil] case object AllPrincipals extends Principal {
    override val provider = "*"
    override val id = "*"
  }

  private[awsutil] case object AllUsers extends Principal {
    override val provider = "AWS"
    override val id = "*"
  }

  private[awsutil] case class AccountPrincipal(account: Account) extends Principal {
    override val provider = "AWS"
    override val id = account.arn.arnString
  }

  private[awsutil] case class ServicePrincipal(service: Service) extends Principal {
    override val provider = "Service"
    override val id = service.id
  }

  private[awsutil] case class WebProviderPrincipal(webIdentityProvider: WebIdentityProvider) extends Principal {
    override val provider = "Federated"
    override val id = webIdentityProvider.provider
  }

  private[awsutil] case class SamlProviderPrincipal(samlProviderArn: SamlProviderArn) extends Principal {
    override val provider = "Federated"
    override val id = samlProviderArn.arnString
  }

  private[awsutil] case class IamUserPrincipal(userArn: UserArn) extends Principal {
    override val provider = "AWS"
    override val id = userArn.arnString
  }

  private[awsutil] case class IamRolePrincipal(roleArn: RoleArn) extends Principal {
    override val provider = "AWS"
    override val id = roleArn.arnString
  }

  private[awsutil] case class StsAssumedRolePrincipal(assumedRoleArn: AssumedRoleArn) extends Principal {
    override val provider = "AWS"
    override val id = assumedRoleArn.arnString
  }
}
