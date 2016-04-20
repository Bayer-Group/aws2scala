package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.{Account, AccountArn, Arn}

object AwsConverters {
  implicit class AwsPrincipal(val principal: aws.Principal) extends AnyVal {
    def asScala: Principal =
      (principal.getProvider, principal.getId) match {
        case ("*", "*") ⇒
          Principal.all
        case ("AWS", "*") ⇒
          Principal.allUsers
        case ("Service", "*") ⇒
          Principal.allServices
        case ("Service", Principal.Service.ById(id)) ⇒
          Principal.service(id)
        case ("Federated", "*") ⇒
          Principal.allWebProviders
        case ("Federated", Principal.WebIdentityProvider.ById(webIdentityProvider)) ⇒
          Principal.webProvider(webIdentityProvider)
        case ("Federated", SamlProviderArn(account, name)) ⇒
          Principal.SamlProviderPrincipal(account, name)
        case ("AWS", AccountArn.FromString(AccountArn(account))) ⇒
          Principal.AccountPrincipal(account)
        case ("AWS", AccountFromNumber(account)) ⇒
          Principal.AccountPrincipal(account)
        case ("AWS", UserArn(account, name, path)) ⇒
          Principal.IamUserPrincipal(account, name, path)
        case ("AWS", RoleArn(account, name, path)) ⇒
          Principal.IamRolePrincipal(account, name, path)
        case ("AWS", AssumedRoleArn(account, roleName, sessionName)) ⇒
          Principal.IamAssumedRolePrincipal(account, roleName, sessionName)
      }
  }

  private object AccountFromNumber {
    def unapply(str: String): Option[Account] = {
      if (str.matches("^\\d{12}$")) {
        Some(Account(str))
      } else {
        None
      }
    }
  }

  private object SamlProviderArn {
    def unapply(str: String): Option[(Account, String)] =
      str match {
        case Arn(_, Arn.Namespace.IAM, None, Some(account), SamlProviderName(name)) ⇒
          Some((account, name))
        case _ ⇒
          None
      }
    
    private val SamlProviderName = "^saml-provider/(.+)$".r
  }

  private object UserArn {
    def unapply(str: String): Option[(Account, String, Option[String])] = {
      str match {
        case Arn(_, Arn.Namespace.IAM, None, Some(account), UserPathAndName("/", name)) ⇒
          Some((account, name, None))
        case Arn(_, Arn.Namespace.IAM, None, Some(account), UserPathAndName(path, name)) ⇒
          Some((account, name, Some(path)))
        case _ ⇒
          None
      }
    }

    private val UserPathAndName = "^user(/|/.*/)([^/]+)$".r
  }

  private object RoleArn {
    def unapply(str: String): Option[(Account, String, Option[String])] = {
      str match {
        case Arn(_, Arn.Namespace.IAM, None, Some(account), RolePathAndName("/", name)) ⇒
          Some((account, name, None))
        case Arn(_, Arn.Namespace.IAM, None, Some(account), RolePathAndName(path, name)) ⇒
          Some((account, name, Some(path)))
        case _ ⇒
          None
      }
    }

    private val RolePathAndName = "^role(/|/.*/)([^/]+)$".r
  }

  private object AssumedRoleArn {
    def unapply(str: String): Option[(Account, String, String)] = {
      str match {
        case Arn(_, Arn.Namespace.IAM, None, Some(account), RoleAndSessionNames(roleName, sessionName)) ⇒
          Some((account, roleName, sessionName))
        case _ ⇒
          None
      }
    }

    private val RoleAndSessionNames = "^assumed-role/([^/]+)/([^/]+)$".r
  }

  implicit class ScalaPrincipal(val principal: Principal) extends AnyVal {
    def asAws: aws.Principal = {
      principal match {
        case Principal.AllPrincipals ⇒
          aws.Principal.All
        case Principal.AllUsers ⇒
          aws.Principal.AllUsers
        case Principal.ServicePrincipal(Principal.Service.AllServices) ⇒
          aws.Principal.AllServices
        case Principal.WebProviderPrincipal(Principal.WebIdentityProvider.AllProviders) ⇒
          aws.Principal.AllWebProviders
        case _ ⇒
          new aws.Principal(principal.provider, principal.id, false)
      }
    }
  }

  implicit class AwsAction(val action: aws.Action) extends AnyVal {
    def asScala: Action = Action.toScalaConversions(action)
  }

  implicit class ScalaAction(val action: Action) extends AnyVal {
    def asAws: aws.Action = Action.toAwsConversions(action)
  }
}
