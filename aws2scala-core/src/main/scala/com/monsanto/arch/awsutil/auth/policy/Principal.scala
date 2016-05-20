package com.monsanto.arch.awsutil.auth.policy

import com.monsanto.arch.awsutil.identitymanagement.model.{RoleArn, SamlProviderArn, UserArn}
import com.monsanto.arch.awsutil.securitytoken.model.AssumedRoleArn
import com.monsanto.arch.awsutil.{Account, AccountArn, Arn}

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

  sealed abstract class Service(val id: String)

  object Service {
    case object AllServices extends Service("*")
    case object AmazonEC2 extends Service("ec2.amazonaws.com")
    case object AmazonElasticTranscoder extends Service("elastictranscoder.amazonaws.com")
    case object AWSCloudHSM extends Service("cloudhsm.amazonaws.com")
    case object AWSDataPipeline extends Service("datapipeline.amazonaws.com")
    case object AWSOpsWorks extends Service("opsworks.amazonaws.com")

    /** All valid values for the enumeration. */
    val values: Seq[Service] =
      Seq(AllServices, AmazonEC2, AmazonElasticTranscoder, AWSCloudHSM, AWSDataPipeline, AWSOpsWorks)

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

  /** Returns a `Provider` instance that corresponds to the given provider and
    * identifier.
    *
    * @param provider indicates in what group of users the principal resides
    * @param id       the unique identifier for the principal
    * @throws IllegalArgumentException if no Principal can be constructed that
    *                                  matches the given provider and
    *                                  identifier
    */
  def apply(provider: String, id: String): Principal =
    fromProviderAndId.unapply((provider, id))
      .getOrElse(throw new IllegalArgumentException(s"($provider, $id) does not resolve to a valid principal."))

  object fromProviderAndId {
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
        case ("Federated", "*") ⇒
          Some(Principal.allWebProviders)
        case ("Federated", Principal.WebIdentityProvider.fromProvider(webIdentityProvider)) ⇒
          Some(Principal.webProvider(webIdentityProvider))
        case ("Federated", Arn(samlProviderArn: SamlProviderArn)) ⇒
          Some(Principal.SamlProviderPrincipal(samlProviderArn))
        case ("AWS", Arn(AccountArn(account))) ⇒
          Some(Principal.AccountPrincipal(account))
        case ("AWS", Account.fromNumber(account)) ⇒
          Some(Principal.AccountPrincipal(account))
        case ("AWS", Arn(userArn: UserArn)) ⇒
          Some(Principal.IamUserPrincipal(userArn))
        case ("AWS", Arn(roleArn: RoleArn)) ⇒
          Some(Principal.IamRolePrincipal(roleArn))
        case ("AWS", Arn(assumedRoleArn: AssumedRoleArn)) ⇒
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
