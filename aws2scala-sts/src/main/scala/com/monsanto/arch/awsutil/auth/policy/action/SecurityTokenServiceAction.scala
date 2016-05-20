package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.SecurityTokenServiceActions
import com.monsanto.arch.awsutil.auth.policy.Action

/** Type for all AWS access control policy actions for Amazon SNS. */
sealed abstract class SecurityTokenServiceAction(_name: String) extends Action(s"sts:${_name}")

object SecurityTokenServiceAction {
  /** Represents any action executed on AWS STS. */
  case object AllSecurityTokenServiceActions extends SecurityTokenServiceAction("*")

  /** Action for the AssumeRole operation. */
  case object AssumeRole extends SecurityTokenServiceAction("AssumeRole")
  /** Action for the AssumeRoleWithSAML operation. */
  case object AssumeRoleWithSAML extends SecurityTokenServiceAction("AssumeRoleWithSAML")
  /** Action for the AssumeRoleWithWebIdentity operation. */
  case object AssumeRoleWithWebIdentity extends SecurityTokenServiceAction("AssumeRoleWithWebIdentity")
  /** Action for the DecodeAuthorizationMessage operation. */
  case object DecodeAuthorizationMessage extends SecurityTokenServiceAction("DecodeAuthorizationMessage")
  /** Action for the GetCallerIdentity operation. */
  case object GetCallerIdentity extends SecurityTokenServiceAction("GetCallerIdentity")
  /** Action for the GetFederationToken operation. */
  case object GetFederationToken extends SecurityTokenServiceAction("GetFederationToken")
  /** Action for the GetSessionToken operation. */
  case object GetSessionToken extends SecurityTokenServiceAction("GetSessionToken")

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
