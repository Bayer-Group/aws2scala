package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

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
  *  - [[Principal.iamAssumedRole]] to match a specific assumed-role user
  *  - [[Principal.service]] to match an AWS service
  */
sealed trait Principal {
  /** Returns the provider for this principal, which indicates in what group of users this principal resides. */
  def provider: String
  /** Returns the unique ID for this principal. */
  def id: String
}

object Principal {
  /** Principal that includes all AWS accounts, AWS web services, and web identity providers. */
  val all: Principal = AllPrincipals

  /** Principal that includes all AWS web services. */
  val allServices: Principal = service(Service.AllServices)

  /** Principal that includes all the web identity providers. */
  val allWebProviders: Principal = webProvider(WebIdentityProvider.AllProviders)

  /** Principal that includes all users, including anonymous users. */
  val allUsers: Principal = AllUsers

  /** Principal a principal for the given AWS account. */
  def account(account: String): Principal = AccountPrincipal(Account(account))

  /** Principal a principal for the given AWS service. */
  def service(service: Service): Principal = ServicePrincipal(service)

  /** Principal a principal for federated users authenticated by the given provider. */
  def webProvider(webIdentityProvider: WebIdentityProvider): Principal = WebProviderPrincipal(webIdentityProvider)

  /** Returns a principal for users from a SAML idenity provider.
    *
    * @param account the AWS account number of the SAML identity provider
    * @param name the name of the SAML identity provider
    */
  def samlProvider(account: String, name: String): Principal = SamlProviderPrincipal(Account(account), name)

  /** Returns a principal for the IAM user in the given account.
    *
    * @param account the AWS account number to which the IAM user belongs (without hyphens)
    * @param user the case-sensitive IAM user name for the principal
    */
  def iamUser(account: String, user: String): Principal = IamUserPrincipal(Account(account), user, None)

  /** Returns a principal for the IAM role in the given account.
    *
    * @param account the AWS account number to which the IAM role belongs (without hyphens)
    * @param roleName the name of the IAM role for the principal
    */
  def iamRole(account: String, roleName: String): Principal = IamRolePrincipal(Account(account), roleName, None)

  /** Returns a principal for a specific IAM assumed-role user.
    *
    * @param account the AWS account number to which the IAM assumed role belongs (without hyphens)
    * @param roleName the name of the IAM role for the principal
    * @param sessionName the name of the assumed role session of the principal
    */
  def iamAssumedRole(account: String, roleName: String, sessionName: String): Principal = IamAssumedRolePrincipal(Account(account), roleName, sessionName)

  sealed abstract class Service(val toAws: aws.Principal.Services) extends AwsEnumeration[aws.Principal.Services] {
    def id = toAws.getServiceId
  }

  object Service extends AwsEnumerationCompanion[Service, aws.Principal.Services] {
    case object AllServices extends Service(aws.Principal.Services.AllServices)
    case object AmazonEC2 extends Service(aws.Principal.Services.AmazonEC2)
    case object AmazonElasticTranscoder extends Service(aws.Principal.Services.AmazonElasticTranscoder)
    case object AWSCloudHSM extends Service(aws.Principal.Services.AWSCloudHSM)
    case object AWSDataPipeline extends Service(aws.Principal.Services.AWSDataPipeline)
    case object AWSOpsWorks extends Service(aws.Principal.Services.AWSOpsWorks)

    /** All valid values for the enumeration. */
    override def values: Seq[Service] =
      Seq(AllServices, AmazonEC2, AmazonElasticTranscoder, AWSCloudHSM, AWSDataPipeline, AWSOpsWorks)

    object ById {
      def unapply(id: String): Option[Service] = values.find(_.id.equalsIgnoreCase(id))
    }
  }

  sealed abstract class WebIdentityProvider(val toAws: aws.Principal.WebIdentityProviders) extends AwsEnumeration[aws.Principal.WebIdentityProviders] {
    def id: String = toAws.getWebIdentityProvider
  }

  object WebIdentityProvider extends AwsEnumerationCompanion[WebIdentityProvider,aws.Principal.WebIdentityProviders] {
    case object AllProviders extends WebIdentityProvider(aws.Principal.WebIdentityProviders.AllProviders)
    case object Amazon extends WebIdentityProvider(aws.Principal.WebIdentityProviders.Amazon)
    case object Facebook extends WebIdentityProvider(aws.Principal.WebIdentityProviders.Facebook)
    case object Google extends WebIdentityProvider(aws.Principal.WebIdentityProviders.Google)

    override def values: Seq[WebIdentityProvider] = Seq(AllProviders, Amazon, Facebook, Google)

    object ById {
      def unapply(id: String): Option[WebIdentityProvider] = values.find(_.id.equalsIgnoreCase(id))
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
    override val id = account.arn.value
  }

  private[awsutil] case class ServicePrincipal(service: Service) extends Principal {
    override val provider = "Service"
    override val id = service.id
  }

  private[awsutil] case class WebProviderPrincipal(webIdentityProvider: WebIdentityProvider) extends Principal {
    override val provider = "Federated"
    override val id = webIdentityProvider.id
  }

  private[awsutil] case class SamlProviderPrincipal(account: Account, name: String) extends Principal {
    override val provider = "Federated"
    override val id = s"arn:${account.partition}:iam::${account.id}:saml-provider/$name"
  }

  private[awsutil] case class IamUserPrincipal(account: Account, name: String, path: Option[String]) extends Principal {
    override val provider = "AWS"
    override val id = s"arn:${account.partition}:iam::${account.id}:user${path.getOrElse("/")}$name"
  }

  private[awsutil] case class IamRolePrincipal(account: Account, name: String, path: Option[String]) extends Principal {
    override val provider = "AWS"
    override val id = s"arn:${account.partition}:iam::${account.id}:role${path.getOrElse("/")}$name"
  }

  private[awsutil] case class IamAssumedRolePrincipal(account: Account, roleName: String, sessionName: String) extends Principal {
    override val provider = "AWS"
    override val id = s"arn:${account.partition}:iam::${account.id}:assumed-role/$roleName/$sessionName"
  }
}
