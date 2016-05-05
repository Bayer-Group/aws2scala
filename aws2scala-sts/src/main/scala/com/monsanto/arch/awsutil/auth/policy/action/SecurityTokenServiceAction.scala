package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.SecurityTokenServiceActions
import com.monsanto.arch.awsutil.auth.policy.Action

/** Type for all AWS access control policy actions for Amazon SNS. */
sealed trait SecurityTokenServiceAction extends Action

object SecurityTokenServiceAction {
  /** Represents any action executed on AWS STS. */
  case object AllSecurityTokenServiceActions extends SecurityTokenServiceAction

  /** Action for the AssumeRole operation. */
  case object AssumeRole extends SecurityTokenServiceAction
  /** Action for the AssumeRoleWithSAML operation. */
  case object AssumeRoleWithSAML extends SecurityTokenServiceAction
  /** Action for the AssumeRoleWithWebIdentity operation. */
  case object AssumeRoleWithWebIdentity extends SecurityTokenServiceAction
  /** Action for the DecodeAuthorizationMessage operation. */
  case object DecodeAuthorizationMessage extends SecurityTokenServiceAction
  /** Action for the GetCallerIdentity operation. */
  case object GetCallerIdentity extends SecurityTokenServiceAction
  /** Action for the GetFederationToken operation. */
  case object GetFederationToken extends SecurityTokenServiceAction
  /** Action for the GetSessionToken operation. */
  case object GetSessionToken extends SecurityTokenServiceAction

  val values: Seq[SecurityTokenServiceAction] = Seq(
    AllSecurityTokenServiceActions, AssumeRole, AssumeRoleWithSAML, AssumeRoleWithWebIdentity,
    DecodeAuthorizationMessage, GetCallerIdentity, GetFederationToken, GetSessionToken)

  private[awsutil] def registerActions(): Unit =
    Action.registerActions(
      SecurityTokenServiceActions.AllSecurityTokenServiceActions → AllSecurityTokenServiceActions,
      SecurityTokenServiceActions.AssumeRole → AssumeRole,
      SecurityTokenServiceActions.AssumeRoleWithSAML → AssumeRoleWithSAML,
      SecurityTokenServiceActions.AssumeRoleWithWebIdentity → AssumeRoleWithWebIdentity,
      SecurityTokenServiceActions.DecodeAuthorizationMessage → DecodeAuthorizationMessage,
      SecurityTokenServiceActions.GetCallerIdentity → GetCallerIdentity,
      SecurityTokenServiceActions.GetFederationToken → GetFederationToken,
      SecurityTokenServiceActions.GetSessionToken → GetSessionToken
    )
}
