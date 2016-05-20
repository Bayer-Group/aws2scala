package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.IdentityManagementActions
import com.monsanto.arch.awsutil.auth.policy.Action

/** Type for all AWS access control policy actions for AWS Identity and Access Management. */
sealed abstract class IdentityManagementAction(_name: String) extends Action(s"iam:${_name}")

object IdentityManagementAction {
  /** Represents any action executed on AWS Identity and Access Management. */
  case object AllIdentityManagementActions extends IdentityManagementAction("*")

  /** Action for the AddRoleToInstanceProfile operation. */
  case object AddRoleToInstanceProfile extends IdentityManagementAction("AddRoleToInstanceProfile")

  /** Action for the AddUserToGroup operation. */
  case object AddUserToGroup extends IdentityManagementAction("AddUserToGroup")

  /** Action for the ChangePassword operation. */
  case object ChangePassword extends IdentityManagementAction("ChangePassword")

  /** Action for the CreateAccessKey operation. */
  case object CreateAccessKey extends IdentityManagementAction("CreateAccessKey")

  /** Action for the CreateAccountAlias operation. */
  case object CreateAccountAlias extends IdentityManagementAction("CreateAccountAlias")

  /** Action for the CreateGroup operation. */
  case object CreateGroup extends IdentityManagementAction("CreateGroup")

  /** Action for the CreateInstanceProfile operation. */
  case object CreateInstanceProfile extends IdentityManagementAction("CreateInstanceProfile")

  /** Action for the CreateLoginProfile operation. */
  case object CreateLoginProfile extends IdentityManagementAction("CreateLoginProfile")

  /** Action for the CreateRole operation. */
  case object CreateRole extends IdentityManagementAction("CreateRole")

  /** Action for the CreateUser operation. */
  case object CreateUser extends IdentityManagementAction("CreateUser")

  /** Action for the CreateVirtualMFADevice operation. */
  case object CreateVirtualMFADevice extends IdentityManagementAction("CreateVirtualMFADevice")

  /** Action for the DeactivateMFADevice operation. */
  case object DeactivateMFADevice extends IdentityManagementAction("DeactivateMFADevice")

  /** Action for the DeleteAccessKey operation. */
  case object DeleteAccessKey extends IdentityManagementAction("DeleteAccessKey")

  /** Action for the DeleteAccountAlias operation. */
  case object DeleteAccountAlias extends IdentityManagementAction("DeleteAccountAlias")

  /** Action for the DeleteAccountPasswordPolicy operation. */
  case object DeleteAccountPasswordPolicy extends IdentityManagementAction("DeleteAccountPasswordPolicy")

  /** Action for the DeleteGroup operation. */
  case object DeleteGroup extends IdentityManagementAction("DeleteGroup")

  /** Action for the DeleteGroupPolicy operation. */
  case object DeleteGroupPolicy extends IdentityManagementAction("DeleteGroupPolicy")

  /** Action for the DeleteInstanceProfile operation. */
  case object DeleteInstanceProfile extends IdentityManagementAction("DeleteInstanceProfile")

  /** Action for the DeleteLoginProfile operation. */
  case object DeleteLoginProfile extends IdentityManagementAction("DeleteLoginProfile")

  /** Action for the DeleteRole operation. */
  case object DeleteRole extends IdentityManagementAction("DeleteRole")

  /** Action for the DeleteRolePolicy operation. */
  case object DeleteRolePolicy extends IdentityManagementAction("DeleteRolePolicy")

  /** Action for the DeleteServerCertificate operation. */
  case object DeleteServerCertificate extends IdentityManagementAction("DeleteServerCertificate")

  /** Action for the DeleteSigningCertificate operation. */
  case object DeleteSigningCertificate extends IdentityManagementAction("DeleteSigningCertificate")

  /** Action for the DeleteUser operation. */
  case object DeleteUser extends IdentityManagementAction("DeleteUser")

  /** Action for the DeleteUserPolicy operation. */
  case object DeleteUserPolicy extends IdentityManagementAction("DeleteUserPolicy")

  /** Action for the DeleteVirtualMFADevice operation. */
  case object DeleteVirtualMFADevice extends IdentityManagementAction("DeleteVirtualMFADevice")

  /** Action for the EnableMFADevice operation. */
  case object EnableMFADevice extends IdentityManagementAction("EnableMFADevice")

  /** Action for the GetAccountPasswordPolicy operation. */
  case object GetAccountPasswordPolicy extends IdentityManagementAction("GetAccountPasswordPolicy")

  /** Action for the GetAccountSummary operation. */
  case object GetAccountSummary extends IdentityManagementAction("GetAccountSummary")

  /** Action for the GetGroup operation. */
  case object GetGroup extends IdentityManagementAction("GetGroup")

  /** Action for the GetGroupPolicy operation. */
  case object GetGroupPolicy extends IdentityManagementAction("GetGroupPolicy")

  /** Action for the GetInstanceProfile operation. */
  case object GetInstanceProfile extends IdentityManagementAction("GetInstanceProfile")

  /** Action for the GetLoginProfile operation. */
  case object GetLoginProfile extends IdentityManagementAction("GetLoginProfile")

  /** Action for the GetRole operation. */
  case object GetRole extends IdentityManagementAction("GetRole")

  /** Action for the GetRolePolicy operation. */
  case object GetRolePolicy extends IdentityManagementAction("GetRolePolicy")

  /** Action for the GetServerCertificate operation. */
  case object GetServerCertificate extends IdentityManagementAction("GetServerCertificate")

  /** Action for the GetUser operation. */
  case object GetUser extends IdentityManagementAction("GetUser")

  /** Action for the GetUserPolicy operation. */
  case object GetUserPolicy extends IdentityManagementAction("GetUserPolicy")

  /** Action for the ListAccessKeys operation. */
  case object ListAccessKeys extends IdentityManagementAction("ListAccessKeys")

  /** Action for the ListAccountAliases operation. */
  case object ListAccountAliases extends IdentityManagementAction("ListAccountAliases")

  /** Action for the ListGroupPolicies operation. */
  case object ListGroupPolicies extends IdentityManagementAction("ListGroupPolicies")

  /** Action for the ListGroups operation. */
  case object ListGroups extends IdentityManagementAction("ListGroups")

  /** Action for the ListGroupsForUser operation. */
  case object ListGroupsForUser extends IdentityManagementAction("ListGroupsForUser")

  /** Action for the ListInstanceProfiles operation. */
  case object ListInstanceProfiles extends IdentityManagementAction("ListInstanceProfiles")

  /** Action for the ListInstanceProfilesForRole operation. */
  case object ListInstanceProfilesForRole extends IdentityManagementAction("ListInstanceProfilesForRole")

  /** Action for the ListMFADevices operation. */
  case object ListMFADevices extends IdentityManagementAction("ListMFADevices")

  /** Action for the ListRolePolicies operation. */
  case object ListRolePolicies extends IdentityManagementAction("ListRolePolicies")

  /** Action for the ListRoles operation. */
  case object ListRoles extends IdentityManagementAction("ListRoles")

  /** Action for the ListServerCertificates operation. */
  case object ListServerCertificates extends IdentityManagementAction("ListServerCertificates")

  /** Action for the ListSigningCertificates operation. */
  case object ListSigningCertificates extends IdentityManagementAction("ListSigningCertificates")

  /** Action for the ListUserPolicies operation. */
  case object ListUserPolicies extends IdentityManagementAction("ListUserPolicies")

  /** Action for the ListUsers operation. */
  case object ListUsers extends IdentityManagementAction("ListUsers")

  /** Action for the ListVirtualMFADevices operation. */
  case object ListVirtualMFADevices extends IdentityManagementAction("ListVirtualMFADevices")

  /** Action for the PassRole operation. */
  case object PassRole extends IdentityManagementAction("PassRole")

  /** Action for the PutGroupPolicy operation. */
  case object PutGroupPolicy extends IdentityManagementAction("PutGroupPolicy")

  /** Action for the PutRolePolicy operation. */
  case object PutRolePolicy extends IdentityManagementAction("PutRolePolicy")

  /** Action for the PutUserPolicy operation. */
  case object PutUserPolicy extends IdentityManagementAction("PutUserPolicy")

  /** Action for the RemoveRoleFromInstanceProfile operation. */
  case object RemoveRoleFromInstanceProfile extends IdentityManagementAction("RemoveRoleFromInstanceProfile")

  /** Action for the RemoveUserFromGroup operation. */
  case object RemoveUserFromGroup extends IdentityManagementAction("RemoveUserFromGroup")

  /** Action for the ResyncMFADevice operation. */
  case object ResyncMFADevice extends IdentityManagementAction("ResyncMFADevice")

  /** Action for the UpdateAccessKey operation. */
  case object UpdateAccessKey extends IdentityManagementAction("UpdateAccessKey")

  /** Action for the UpdateAccountPasswordPolicy operation. */
  case object UpdateAccountPasswordPolicy extends IdentityManagementAction("UpdateAccountPasswordPolicy")

  /** Action for the UpdateAssumeRolePolicy operation. */
  case object UpdateAssumeRolePolicy extends IdentityManagementAction("UpdateAssumeRolePolicy")

  /** Action for the UpdateGroup operation. */
  case object UpdateGroup extends IdentityManagementAction("UpdateGroup")

  /** Action for the UpdateLoginProfile operation. */
  case object UpdateLoginProfile extends IdentityManagementAction("UpdateLoginProfile")

  /** Action for the UpdateServerCertificate operation. */
  case object UpdateServerCertificate extends IdentityManagementAction("UpdateServerCertificate")

  /** Action for the UpdateSigningCertificate operation. */
  case object UpdateSigningCertificate extends IdentityManagementAction("UpdateSigningCertificate")

  /** Action for the UpdateUser operation. */
  case object UpdateUser extends IdentityManagementAction("UpdateUser")

  /** Action for the UploadServerCertificate operation. */
  case object UploadServerCertificate extends IdentityManagementAction("UploadServerCertificate")

  /** Action for the UploadSigningCertificate operation. */
  case object UploadSigningCertificate extends IdentityManagementAction("UploadSigningCertificate")

  val values: Seq[IdentityManagementAction] = Seq(
    AllIdentityManagementActions, AddRoleToInstanceProfile, AddUserToGroup, ChangePassword, CreateAccessKey,
    CreateAccountAlias, CreateGroup, CreateInstanceProfile, CreateLoginProfile, CreateRole, CreateUser,
    CreateVirtualMFADevice, DeactivateMFADevice, DeleteAccessKey, DeleteAccountAlias, DeleteAccountPasswordPolicy,
    DeleteGroup, DeleteGroupPolicy, DeleteInstanceProfile, DeleteLoginProfile, DeleteRole, DeleteRolePolicy,
    DeleteServerCertificate, DeleteSigningCertificate, DeleteUser, DeleteUserPolicy, DeleteVirtualMFADevice,
    EnableMFADevice, GetAccountPasswordPolicy, GetAccountSummary, GetGroup, GetGroupPolicy, GetInstanceProfile,
    GetLoginProfile, GetRole, GetRolePolicy, GetServerCertificate, GetUser, GetUserPolicy, ListAccessKeys,
    ListAccountAliases, ListGroupPolicies, ListGroups, ListGroupsForUser, ListInstanceProfiles,
    ListInstanceProfilesForRole, ListMFADevices, ListRolePolicies, ListRoles, ListServerCertificates,
    ListSigningCertificates, ListUserPolicies, ListUsers, ListVirtualMFADevices, PassRole, PutGroupPolicy,
    PutRolePolicy, PutUserPolicy, RemoveRoleFromInstanceProfile, RemoveUserFromGroup, ResyncMFADevice,
    UpdateAccessKey, UpdateAccountPasswordPolicy, UpdateAssumeRolePolicy, UpdateGroup, UpdateLoginProfile,
    UpdateServerCertificate, UpdateSigningCertificate, UpdateUser, UploadServerCertificate, UploadSigningCertificate
  )

  private[awsutil] def registerActions(): Unit =
    Action.registerActions(
      IdentityManagementActions.AllIdentityManagementActions → AllIdentityManagementActions,
      IdentityManagementActions.AddRoleToInstanceProfile → AddRoleToInstanceProfile,
      IdentityManagementActions.AddUserToGroup → AddUserToGroup,
      IdentityManagementActions.ChangePassword → ChangePassword,
      IdentityManagementActions.CreateAccessKey → CreateAccessKey,
      IdentityManagementActions.CreateAccountAlias → CreateAccountAlias,
      IdentityManagementActions.CreateGroup → CreateGroup,
      IdentityManagementActions.CreateInstanceProfile → CreateInstanceProfile,
      IdentityManagementActions.CreateLoginProfile → CreateLoginProfile,
      IdentityManagementActions.CreateRole → CreateRole,
      IdentityManagementActions.CreateUser → CreateUser,
      IdentityManagementActions.CreateVirtualMFADevice → CreateVirtualMFADevice,
      IdentityManagementActions.DeactivateMFADevice → DeactivateMFADevice,
      IdentityManagementActions.DeleteAccessKey → DeleteAccessKey,
      IdentityManagementActions.DeleteAccountAlias → DeleteAccountAlias,
      IdentityManagementActions.DeleteAccountPasswordPolicy → DeleteAccountPasswordPolicy,
      IdentityManagementActions.DeleteGroup → DeleteGroup,
      IdentityManagementActions.DeleteGroupPolicy → DeleteGroupPolicy,
      IdentityManagementActions.DeleteInstanceProfile → DeleteInstanceProfile,
      IdentityManagementActions.DeleteLoginProfile → DeleteLoginProfile,
      IdentityManagementActions.DeleteRole → DeleteRole,
      IdentityManagementActions.DeleteRolePolicy → DeleteRolePolicy,
      IdentityManagementActions.DeleteServerCertificate → DeleteServerCertificate,
      IdentityManagementActions.DeleteSigningCertificate → DeleteSigningCertificate,
      IdentityManagementActions.DeleteUser → DeleteUser,
      IdentityManagementActions.DeleteUserPolicy → DeleteUserPolicy,
      IdentityManagementActions.DeleteVirtualMFADevice → DeleteVirtualMFADevice,
      IdentityManagementActions.EnableMFADevice → EnableMFADevice,
      IdentityManagementActions.GetAccountPasswordPolicy → GetAccountPasswordPolicy,
      IdentityManagementActions.GetAccountSummary → GetAccountSummary,
      IdentityManagementActions.GetGroup → GetGroup,
      IdentityManagementActions.GetGroupPolicy → GetGroupPolicy,
      IdentityManagementActions.GetInstanceProfile → GetInstanceProfile,
      IdentityManagementActions.GetLoginProfile → GetLoginProfile,
      IdentityManagementActions.GetRole → GetRole,
      IdentityManagementActions.GetRolePolicy → GetRolePolicy,
      IdentityManagementActions.GetServerCertificate → GetServerCertificate,
      IdentityManagementActions.GetUser → GetUser,
      IdentityManagementActions.GetUserPolicy → GetUserPolicy,
      IdentityManagementActions.ListAccessKeys → ListAccessKeys,
      IdentityManagementActions.ListAccountAliases → ListAccountAliases,
      IdentityManagementActions.ListGroupPolicies → ListGroupPolicies,
      IdentityManagementActions.ListGroups → ListGroups,
      IdentityManagementActions.ListGroupsForUser → ListGroupsForUser,
      IdentityManagementActions.ListInstanceProfiles → ListInstanceProfiles,
      IdentityManagementActions.ListInstanceProfilesForRole → ListInstanceProfilesForRole,
      IdentityManagementActions.ListMFADevices → ListMFADevices,
      IdentityManagementActions.ListRolePolicies → ListRolePolicies,
      IdentityManagementActions.ListRoles → ListRoles,
      IdentityManagementActions.ListServerCertificates → ListServerCertificates,
      IdentityManagementActions.ListSigningCertificates → ListSigningCertificates,
      IdentityManagementActions.ListUserPolicies → ListUserPolicies,
      IdentityManagementActions.ListUsers → ListUsers,
      IdentityManagementActions.ListVirtualMFADevices → ListVirtualMFADevices,
      IdentityManagementActions.PassRole → PassRole,
      IdentityManagementActions.PutGroupPolicy → PutGroupPolicy,
      IdentityManagementActions.PutRolePolicy → PutRolePolicy,
      IdentityManagementActions.PutUserPolicy → PutUserPolicy,
      IdentityManagementActions.RemoveRoleFromInstanceProfile → RemoveRoleFromInstanceProfile,
      IdentityManagementActions.RemoveUserFromGroup → RemoveUserFromGroup,
      IdentityManagementActions.ResyncMFADevice → ResyncMFADevice,
      IdentityManagementActions.UpdateAccessKey → UpdateAccessKey,
      IdentityManagementActions.UpdateAccountPasswordPolicy → UpdateAccountPasswordPolicy,
      IdentityManagementActions.UpdateAssumeRolePolicy → UpdateAssumeRolePolicy,
      IdentityManagementActions.UpdateGroup → UpdateGroup,
      IdentityManagementActions.UpdateLoginProfile → UpdateLoginProfile,
      IdentityManagementActions.UpdateServerCertificate → UpdateServerCertificate,
      IdentityManagementActions.UpdateSigningCertificate → UpdateSigningCertificate,
      IdentityManagementActions.UpdateUser → UpdateUser,
      IdentityManagementActions.UploadServerCertificate → UploadServerCertificate,
      IdentityManagementActions.UploadSigningCertificate → UploadSigningCertificate
    )
}
