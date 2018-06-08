package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.IdentityManagementActions
import com.monsanto.arch.awsutil.auth.policy.Action

/** Type for all AWS access control policy actions for AWS Identity and Access Management. */
sealed abstract class IdentityManagementAction(_name: String) extends Action(s"iam:${_name}")

object IdentityManagementAction {
  /** Represents any action executed on AWS Identity and Access Management. */
  case object AllIdentityManagementActions extends IdentityManagementAction("*")

  /** Action for the AddClientIDToOpenIDConnectProvider operation. */
  case object AddClientIDToOpenIDConnectProvider extends IdentityManagementAction("AddClientIDToOpenIDConnectProvider")

  /** Action for the AddRoleToInstanceProfile operation. */
  case object AddRoleToInstanceProfile extends IdentityManagementAction("AddRoleToInstanceProfile")

  /** Action for the AddUserToGroup operation. */
  case object AddUserToGroup extends IdentityManagementAction("AddUserToGroup")

  /** Action for the AttachGroupPolicy operation. */
  case object AttachGroupPolicy extends IdentityManagementAction("AttachGroupPolicy")

  /** Action for the AttachRolePolicy operation. */
  case object AttachRolePolicy extends IdentityManagementAction("AttachRolePolicy")

  /** Action for the AttachUserPolicy operation. */
  case object AttachUserPolicy extends IdentityManagementAction("AttachUserPolicy")

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

  /** Action for the CreateOpenIDConnectProvider operation. */
  case object CreateOpenIDConnectProvider extends IdentityManagementAction("CreateOpenIDConnectProvider")

  /** Action for the CreatePolicy operation. */
  case object CreatePolicy extends IdentityManagementAction("CreatePolicy")

  /** Action for the CreatePolicyVersion operation. */
  case object CreatePolicyVersion extends IdentityManagementAction("CreatePolicyVersion")

  /** Action for the CreateRole operation. */
  case object CreateRole extends IdentityManagementAction("CreateRole")

  /** Action for the CreateSAMLProvider operation. */
  case object CreateSAMLProvider extends IdentityManagementAction("CreateSAMLProvider")

  /** Action for the CreateServiceLinkedRole operation. */
  case object CreateServiceLinkedRole extends IdentityManagementAction("CreateServiceLinkedRole")

  /** Action for the CreateServiceSpecificCredential operation. */
  case object CreateServiceSpecificCredential extends IdentityManagementAction("CreateServiceSpecificCredential")

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

  /** Action for the DeleteOpenIDConnectProvider operation. */
  case object DeleteOpenIDConnectProvider extends IdentityManagementAction("DeleteOpenIDConnectProvider")

  /** Action for the DeletePolicy operation. */
  case object DeletePolicy extends IdentityManagementAction("DeletePolicy")

  /** Action for the DeletePolicyVersion operation. */
  case object DeletePolicyVersion extends IdentityManagementAction("DeletePolicyVersion")

  /** Action for the DeleteRole operation. */
  case object DeleteRole extends IdentityManagementAction("DeleteRole")

  /** Action for the DeleteRolePolicy operation. */
  case object DeleteRolePolicy extends IdentityManagementAction("DeleteRolePolicy")

  /** Action for the DeleteServerCertificate operation. */
  case object DeleteServerCertificate extends IdentityManagementAction("DeleteServerCertificate")

  /** Action for the DeleteServiceLinkedRole operation. */
  case object DeleteServiceLinkedRole extends IdentityManagementAction("DeleteServiceLinkedRole")

  /** Action for the DeleteServiceSpecificCredential operation. */
  case object DeleteServiceSpecificCredential extends IdentityManagementAction("DeleteServiceSpecificCredential")

  /** Action for the DeleteSigningCertificate operation. */
  case object DeleteSigningCertificate extends IdentityManagementAction("DeleteSigningCertificate")

  /** Action for the DeleteSAMLProvider operation. */
  case object DeleteSAMLProvider extends IdentityManagementAction("DeleteSAMLProvider")

  /** Action for the DeleteSSHPublicKey operation. */
  case object DeleteSSHPublicKey extends IdentityManagementAction("DeleteSSHPublicKey")

  /** Action for the DeleteUser operation. */
  case object DeleteUser extends IdentityManagementAction("DeleteUser")

  /** Action for the DeleteUserPolicy operation. */
  case object DeleteUserPolicy extends IdentityManagementAction("DeleteUserPolicy")

  /** Action for the DeleteVirtualMFADevice operation. */
  case object DeleteVirtualMFADevice extends IdentityManagementAction("DeleteVirtualMFADevice")

  /** Action for the DetachGroupPolicy operation. */
  case object DetachGroupPolicy extends IdentityManagementAction("DetachGroupPolicy")

  /** Action for the DetachRolePolicy operation. */
  case object DetachRolePolicy extends IdentityManagementAction("DetachRolePolicy")

  /** Action for the DetachUserPolicy operation. */
  case object DetachUserPolicy extends IdentityManagementAction("DetachUserPolicy")

  /** Action for the EnableMFADevice operation. */
  case object EnableMFADevice extends IdentityManagementAction("EnableMFADevice")

  /** Action for the GenerateCredentialReport operation. */
  case object GenerateCredentialReport extends IdentityManagementAction("GenerateCredentialReport")

  /** Action for the GetAccessKeyLastUsed operation. */
  case object GetAccessKeyLastUsed extends IdentityManagementAction("GetAccessKeyLastUsed")

  /** Action for the GetAccountAuthorizationDetails operation. */
  case object GetAccountAuthorizationDetails extends IdentityManagementAction("GetAccountAuthorizationDetails")

  /** Action for the GetAccountPasswordPolicy operation. */
  case object GetAccountPasswordPolicy extends IdentityManagementAction("GetAccountPasswordPolicy")

  /** Action for the GetAccountSummary operation. */
  case object GetAccountSummary extends IdentityManagementAction("GetAccountSummary")

  /** Action for the GetContextKeysForCustomPolicy operation. */
  case object GetContextKeysForCustomPolicy extends IdentityManagementAction("GetContextKeysForCustomPolicy")

  /** Action for the GetContextKeysForPrincipalPolicy operation. */
  case object GetContextKeysForPrincipalPolicy extends IdentityManagementAction("GetContextKeysForPrincipalPolicy")

  /** Action for the GetCredentialReport operation. */
  case object GetCredentialReport extends IdentityManagementAction("GetCredentialReport")

  /** Action for the GetGroup operation. */
  case object GetGroup extends IdentityManagementAction("GetGroup")

  /** Action for the GetGroupPolicy operation. */
  case object GetGroupPolicy extends IdentityManagementAction("GetGroupPolicy")

  /** Action for the GetInstanceProfile operation. */
  case object GetInstanceProfile extends IdentityManagementAction("GetInstanceProfile")

  /** Action for the GetLoginProfile operation. */
  case object GetLoginProfile extends IdentityManagementAction("GetLoginProfile")

  /** Action for the GetOpenIDConnectProvider operation. */
  case object GetOpenIDConnectProvider extends IdentityManagementAction("GetOpenIDConnectProvider")

  /** Action for the GetPolicy operation. */
  case object GetPolicy extends IdentityManagementAction("GetPolicy")

  /** Action for the GetPolicyVersion operation. */
  case object GetPolicyVersion extends IdentityManagementAction("GetPolicyVersion")

  /** Action for the GetRole operation. */
  case object GetRole extends IdentityManagementAction("GetRole")

  /** Action for the GetRolePolicy operation. */
  case object GetRolePolicy extends IdentityManagementAction("GetRolePolicy")

  /** Action for the GetSAMLProvider operation. */
  case object GetSAMLProvider extends IdentityManagementAction("GetSAMLProvider")

  /** Action for the GetSSHPublicKey operation. */
  case object GetSSHPublicKey extends IdentityManagementAction("GetSSHPublicKey")

  /** Action for the GetServerCertificate operation. */
  case object GetServerCertificate extends IdentityManagementAction("GetServerCertificate")

  /** Action for the GetServiceLinkedRoleDeletionStatus operation. */
  case object GetServiceLinkedRoleDeletionStatus extends IdentityManagementAction("GetServiceLinkedRoleDeletionStatus")

  /** Action for the GetUser operation. */
  case object GetUser extends IdentityManagementAction("GetUser")

  /** Action for the GetUserPolicy operation. */
  case object GetUserPolicy extends IdentityManagementAction("GetUserPolicy")

  /** Action for the ListAccessKeys operation. */
  case object ListAccessKeys extends IdentityManagementAction("ListAccessKeys")

  /** Action for the ListAccountAliases operation. */
  case object ListAccountAliases extends IdentityManagementAction("ListAccountAliases")

  /** Action for the ListAttachedGroupPolicies operation. */
  case object ListAttachedGroupPolicies extends IdentityManagementAction("ListAttachedGroupPolicies")

  /** Action for the ListAttachedRolePolicies operation. */
  case object ListAttachedRolePolicies extends IdentityManagementAction("ListAttachedRolePolicies")

  /** Action for the ListAttachedUserPolicies operation. */
  case object ListAttachedUserPolicies extends IdentityManagementAction("ListAttachedUserPolicies")

  /** Action for the ListEntitiesForPolicy operation. */
  case object ListEntitiesForPolicy extends IdentityManagementAction("ListEntitiesForPolicy")

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

  /** Action for the ListOpenIDConnectProviders operation. */
  case object ListOpenIDConnectProviders extends IdentityManagementAction("ListOpenIDConnectProviders")

  /** Action for the ListPolicies operation. */
  case object ListPolicies extends IdentityManagementAction("ListPolicies")

  /** Action for the ListPolicyVersions operation. */
  case object ListPolicyVersions extends IdentityManagementAction("ListPolicyVersions")

  /** Action for the ListRolePolicies operation. */
  case object ListRolePolicies extends IdentityManagementAction("ListRolePolicies")

  /** Action for the ListRoles operation. */
  case object ListRoles extends IdentityManagementAction("ListRoles")

  /** Action for the ListSAMLProviders operation. */
  case object ListSAMLProviders extends IdentityManagementAction("ListSAMLProviders")

  /** Action for the ListSSHPublicKeys operation. */
  case object ListSSHPublicKeys extends IdentityManagementAction("ListSSHPublicKeys")

  /** Action for the ListServerCertificates operation. */
  case object ListServerCertificates extends IdentityManagementAction("ListServerCertificates")

  /** Action for the ListServiceSpecificCredentials operation. */
  case object ListServiceSpecificCredentials extends IdentityManagementAction("ListServiceSpecificCredentials")

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

  /** Action for the RemoveClientIDFromOpenIDConnectProvider operation. */
  case object RemoveClientIDFromOpenIDConnectProvider extends IdentityManagementAction("RemoveClientIDFromOpenIDConnectProvider")

  /** Action for the RemoveRoleFromInstanceProfile operation. */
  case object RemoveRoleFromInstanceProfile extends IdentityManagementAction("RemoveRoleFromInstanceProfile")

  /** Action for the RemoveUserFromGroup operation. */
  case object RemoveUserFromGroup extends IdentityManagementAction("RemoveUserFromGroup")

  /** Action for the ResetServiceSpecificCredential operation. */
  case object ResetServiceSpecificCredential extends IdentityManagementAction("ResetServiceSpecificCredential")

  /** Action for the ResyncMFADevice operation. */
  case object ResyncMFADevice extends IdentityManagementAction("ResyncMFADevice")

  /** Action for the SetDefaultPolicyVersion operation. */
  case object SetDefaultPolicyVersion extends IdentityManagementAction("SetDefaultPolicyVersion")

  /** Action for the SimulateCustomPolicy operation. */
  case object SimulateCustomPolicy extends IdentityManagementAction("SimulateCustomPolicy")

  /** Action for the SimulatePrincipalPolicy operation. */
  case object SimulatePrincipalPolicy extends IdentityManagementAction("SimulatePrincipalPolicy")

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

  /** Action for the UpdateOpenIDConnectProviderThumbprint operation. */
  case object UpdateOpenIDConnectProviderThumbprint extends IdentityManagementAction("UpdateOpenIDConnectProviderThumbprint")

  /** Action for the UpdateRole operation. */
  case object UpdateRole extends IdentityManagementAction("UpdateRole")

  /** Action for the UpdateRoleDescription operation. */
  case object UpdateRoleDescription extends IdentityManagementAction("UpdateRoleDescription")

  /** Action for the UpdateSAMLProvider operation. */
  case object UpdateSAMLProvider extends IdentityManagementAction("UpdateSAMLProvider")

  /** Action for the UpdateSSHPublicKey operation. */
  case object UpdateSSHPublicKey extends IdentityManagementAction("UpdateSSHPublicKey")

  /** Action for the UpdateServerCertificate operation. */
  case object UpdateServerCertificate extends IdentityManagementAction("UpdateServerCertificate")

  /** Action for the UpdateServiceSpecificCredential operation. */
  case object UpdateServiceSpecificCredential extends IdentityManagementAction("UpdateServiceSpecificCredential")

  /** Action for the UpdateSigningCertificate operation. */
  case object UpdateSigningCertificate extends IdentityManagementAction("UpdateSigningCertificate")

  /** Action for the UploadSSHPublicKey operation. */
  case object UploadSSHPublicKey extends IdentityManagementAction("UploadSSHPublicKey")

  /** Action for the UpdateUser operation. */
  case object UpdateUser extends IdentityManagementAction("UpdateUser")

  /** Action for the UploadServerCertificate operation. */
  case object UploadServerCertificate extends IdentityManagementAction("UploadServerCertificate")

  /** Action for the UploadSigningCertificate operation. */
  case object UploadSigningCertificate extends IdentityManagementAction("UploadSigningCertificate")

  val values: Seq[IdentityManagementAction] = Seq(
    AllIdentityManagementActions, AddClientIDToOpenIDConnectProvider, AddRoleToInstanceProfile, AddUserToGroup,
    AttachGroupPolicy, AttachRolePolicy, AttachUserPolicy, ChangePassword, CreateAccessKey,
    CreateAccountAlias, CreateGroup, CreateInstanceProfile, CreateLoginProfile, CreateOpenIDConnectProvider,
    CreateRole, CreateUser, CreatePolicy, CreatePolicyVersion, CreateSAMLProvider, CreateServiceLinkedRole,
    CreateServiceSpecificCredential, DeleteOpenIDConnectProvider, DeleteSAMLProvider, DeleteSSHPublicKey,
    CreateVirtualMFADevice, DeactivateMFADevice, DeleteAccessKey, DeleteAccountAlias, DeleteAccountPasswordPolicy,
    DeleteGroup, DeleteGroupPolicy, DeleteInstanceProfile, DeleteLoginProfile, DeleteRole, DeleteRolePolicy,
    DeletePolicy, DeletePolicyVersion,
    DeleteServiceLinkedRole, DeleteServiceSpecificCredential, DetachGroupPolicy, DetachRolePolicy, DetachUserPolicy,
    DeleteServerCertificate, DeleteSigningCertificate, DeleteUser, DeleteUserPolicy, DeleteVirtualMFADevice,
    GenerateCredentialReport, GetAccessKeyLastUsed, GetAccountAuthorizationDetails,
    GetContextKeysForCustomPolicy, GetContextKeysForPrincipalPolicy, GetCredentialReport,
    EnableMFADevice, GetAccountPasswordPolicy, GetAccountSummary, GetGroup, GetGroupPolicy, GetInstanceProfile,
    GetOpenIDConnectProvider, GetPolicy, GetPolicyVersion, GetSAMLProvider, GetSSHPublicKey, GetServiceLinkedRoleDeletionStatus,
    GetLoginProfile, GetRole, GetRolePolicy, GetServerCertificate, GetUser, GetUserPolicy, ListAccessKeys,
    ListAccountAliases, ListGroupPolicies, ListGroups, ListGroupsForUser, ListInstanceProfiles,
    ListInstanceProfilesForRole, ListMFADevices, ListRolePolicies, ListRoles, ListServerCertificates,
    ListAttachedGroupPolicies, ListAttachedRolePolicies, ListAttachedUserPolicies, ListEntitiesForPolicy,
    ListOpenIDConnectProviders, ListPolicies, ListPolicyVersions, ListSAMLProviders, ListSSHPublicKeys,
    ListServiceSpecificCredentials,
    ListSigningCertificates, ListUserPolicies, ListUsers, ListVirtualMFADevices, PassRole, PutGroupPolicy,
    PutRolePolicy, PutUserPolicy, RemoveRoleFromInstanceProfile, RemoveUserFromGroup, ResyncMFADevice,
    RemoveClientIDFromOpenIDConnectProvider, ResetServiceSpecificCredential, SetDefaultPolicyVersion,
    SimulateCustomPolicy, SimulatePrincipalPolicy, UpdateOpenIDConnectProviderThumbprint, UpdateRole, UpdateRoleDescription,
    UpdateSAMLProvider, UpdateSSHPublicKey, UpdateServiceSpecificCredential, UploadSSHPublicKey,
    UpdateAccessKey, UpdateAccountPasswordPolicy, UpdateAssumeRolePolicy, UpdateGroup, UpdateLoginProfile,
    UpdateServerCertificate, UpdateSigningCertificate, UpdateUser, UploadServerCertificate, UploadSigningCertificate
  )

  private[awsutil] def registerActions(): Unit =
    Action.registerActions(
      IdentityManagementActions.AllIdentityManagementActions → AllIdentityManagementActions,
      IdentityManagementActions.AddClientIDToOpenIDConnectProvider → AddClientIDToOpenIDConnectProvider,
      IdentityManagementActions.AddRoleToInstanceProfile → AddRoleToInstanceProfile,
      IdentityManagementActions.AddUserToGroup → AddUserToGroup,
      IdentityManagementActions.AttachGroupPolicy → AttachGroupPolicy,
      IdentityManagementActions.AttachRolePolicy → AttachRolePolicy,
      IdentityManagementActions.AttachUserPolicy → AttachUserPolicy,
      IdentityManagementActions.ChangePassword → ChangePassword,
      IdentityManagementActions.CreateAccessKey → CreateAccessKey,
      IdentityManagementActions.CreateAccountAlias → CreateAccountAlias,
      IdentityManagementActions.CreateGroup → CreateGroup,
      IdentityManagementActions.CreateInstanceProfile → CreateInstanceProfile,
      IdentityManagementActions.CreateLoginProfile → CreateLoginProfile,
      IdentityManagementActions.CreateOpenIDConnectProvider → CreateOpenIDConnectProvider,
      IdentityManagementActions.CreatePolicy → CreatePolicy,
      IdentityManagementActions.CreatePolicyVersion → CreatePolicyVersion,
      IdentityManagementActions.CreateRole → CreateRole,
      IdentityManagementActions.CreateSAMLProvider → CreateSAMLProvider,
      IdentityManagementActions.CreateServiceLinkedRole → CreateServiceLinkedRole,
      IdentityManagementActions.CreateServiceSpecificCredential → CreateServiceSpecificCredential,
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
      IdentityManagementActions.DeleteOpenIDConnectProvider → DeleteOpenIDConnectProvider,
      IdentityManagementActions.DeletePolicy → DeletePolicy,
      IdentityManagementActions.DeletePolicyVersion → DeletePolicyVersion,
      IdentityManagementActions.DeleteRole → DeleteRole,
      IdentityManagementActions.DeleteRolePolicy → DeleteRolePolicy,
      IdentityManagementActions.DeleteSAMLProvider → DeleteSAMLProvider,
      IdentityManagementActions.DeleteSSHPublicKey → DeleteSSHPublicKey,
      IdentityManagementActions.DeleteServerCertificate → DeleteServerCertificate,
      IdentityManagementActions.DeleteServiceLinkedRole → DeleteServiceLinkedRole,
      IdentityManagementActions.DeleteServiceSpecificCredential → DeleteServiceSpecificCredential,
      IdentityManagementActions.DeleteSigningCertificate → DeleteSigningCertificate,
      IdentityManagementActions.DeleteUser → DeleteUser,
      IdentityManagementActions.DeleteUserPolicy → DeleteUserPolicy,
      IdentityManagementActions.DeleteVirtualMFADevice → DeleteVirtualMFADevice,
      IdentityManagementActions.DetachGroupPolicy → DetachGroupPolicy,
      IdentityManagementActions.DetachRolePolicy → DetachRolePolicy,
      IdentityManagementActions.DetachUserPolicy → DetachUserPolicy,
      IdentityManagementActions.EnableMFADevice → EnableMFADevice,
      IdentityManagementActions.GenerateCredentialReport → GenerateCredentialReport,
      IdentityManagementActions.GetAccessKeyLastUsed → GetAccessKeyLastUsed,
      IdentityManagementActions.GetAccountAuthorizationDetails → GetAccountAuthorizationDetails,
      IdentityManagementActions.GetAccountPasswordPolicy → GetAccountPasswordPolicy,
      IdentityManagementActions.GetAccountSummary → GetAccountSummary,
      IdentityManagementActions.GetContextKeysForCustomPolicy → GetContextKeysForCustomPolicy,
      IdentityManagementActions.GetContextKeysForPrincipalPolicy → GetContextKeysForPrincipalPolicy,
      IdentityManagementActions.GetCredentialReport → GetCredentialReport,
      IdentityManagementActions.GetGroup → GetGroup,
      IdentityManagementActions.GetGroupPolicy → GetGroupPolicy,
      IdentityManagementActions.GetInstanceProfile → GetInstanceProfile,
      IdentityManagementActions.GetLoginProfile → GetLoginProfile,
      IdentityManagementActions.GetOpenIDConnectProvider → GetOpenIDConnectProvider,
      IdentityManagementActions.GetPolicy → GetPolicy,
      IdentityManagementActions.GetPolicyVersion → GetPolicyVersion,
      IdentityManagementActions.GetRole → GetRole,
      IdentityManagementActions.GetRolePolicy → GetRolePolicy,
      IdentityManagementActions.GetSAMLProvider → GetSAMLProvider,
      IdentityManagementActions.GetServerCertificate → GetServerCertificate,
      IdentityManagementActions.GetSSHPublicKey → GetSSHPublicKey,
      IdentityManagementActions.GetServiceLinkedRoleDeletionStatus → GetServiceLinkedRoleDeletionStatus,
      IdentityManagementActions.GetUser → GetUser,
      IdentityManagementActions.GetUserPolicy → GetUserPolicy,
      IdentityManagementActions.ListAccessKeys → ListAccessKeys,
      IdentityManagementActions.ListAccountAliases → ListAccountAliases,
      IdentityManagementActions.ListAttachedGroupPolicies → ListAttachedGroupPolicies,
      IdentityManagementActions.ListAttachedRolePolicies → ListAttachedRolePolicies,
      IdentityManagementActions.ListAttachedUserPolicies → ListAttachedUserPolicies,
      IdentityManagementActions.ListEntitiesForPolicy → ListEntitiesForPolicy,
      IdentityManagementActions.ListGroupPolicies → ListGroupPolicies,
      IdentityManagementActions.ListGroups → ListGroups,
      IdentityManagementActions.ListGroupsForUser → ListGroupsForUser,
      IdentityManagementActions.ListInstanceProfiles → ListInstanceProfiles,
      IdentityManagementActions.ListInstanceProfilesForRole → ListInstanceProfilesForRole,
      IdentityManagementActions.ListMFADevices → ListMFADevices,
      IdentityManagementActions.ListOpenIDConnectProviders → ListOpenIDConnectProviders,
      IdentityManagementActions.ListPolicies → ListPolicies,
      IdentityManagementActions.ListPolicyVersions → ListPolicyVersions,
      IdentityManagementActions.ListRolePolicies → ListRolePolicies,
      IdentityManagementActions.ListRoles → ListRoles,
      IdentityManagementActions.ListSAMLProviders → ListSAMLProviders,
      IdentityManagementActions.ListSSHPublicKeys → ListSSHPublicKeys,
      IdentityManagementActions.ListServerCertificates → ListServerCertificates,
      IdentityManagementActions.ListServiceSpecificCredentials → ListServiceSpecificCredentials,
      IdentityManagementActions.ListSigningCertificates → ListSigningCertificates,
      IdentityManagementActions.ListUserPolicies → ListUserPolicies,
      IdentityManagementActions.ListUsers → ListUsers,
      IdentityManagementActions.ListVirtualMFADevices → ListVirtualMFADevices,
      IdentityManagementActions.PassRole → PassRole,
      IdentityManagementActions.PutGroupPolicy → PutGroupPolicy,
      IdentityManagementActions.PutRolePolicy → PutRolePolicy,
      IdentityManagementActions.PutUserPolicy → PutUserPolicy,
      IdentityManagementActions.RemoveClientIDFromOpenIDConnectProvider → RemoveClientIDFromOpenIDConnectProvider,
      IdentityManagementActions.RemoveRoleFromInstanceProfile → RemoveRoleFromInstanceProfile,
      IdentityManagementActions.RemoveUserFromGroup → RemoveUserFromGroup,
      IdentityManagementActions.ResetServiceSpecificCredential → ResetServiceSpecificCredential,
      IdentityManagementActions.ResyncMFADevice → ResyncMFADevice,
      IdentityManagementActions.SetDefaultPolicyVersion → SetDefaultPolicyVersion,
      IdentityManagementActions.SimulateCustomPolicy → SimulateCustomPolicy,
      IdentityManagementActions.SimulatePrincipalPolicy → SimulatePrincipalPolicy,
      IdentityManagementActions.UpdateAccessKey → UpdateAccessKey,
      IdentityManagementActions.UpdateAccountPasswordPolicy → UpdateAccountPasswordPolicy,
      IdentityManagementActions.UpdateAssumeRolePolicy → UpdateAssumeRolePolicy,
      IdentityManagementActions.UpdateGroup → UpdateGroup,
      IdentityManagementActions.UpdateLoginProfile → UpdateLoginProfile,
      IdentityManagementActions.UpdateOpenIDConnectProviderThumbprint → UpdateOpenIDConnectProviderThumbprint,
      IdentityManagementActions.UpdateRole → UpdateRole,
      IdentityManagementActions.UpdateRoleDescription → UpdateRoleDescription,
      IdentityManagementActions.UpdateSAMLProvider → UpdateSAMLProvider,
      IdentityManagementActions.UpdateSSHPublicKey → UpdateSSHPublicKey,
      IdentityManagementActions.UpdateServerCertificate → UpdateServerCertificate,
      IdentityManagementActions.UpdateServiceSpecificCredential → UpdateServiceSpecificCredential,
      IdentityManagementActions.UpdateSigningCertificate → UpdateSigningCertificate,
      IdentityManagementActions.UpdateUser → UpdateUser,
      IdentityManagementActions.UploadSSHPublicKey → UploadSSHPublicKey,
      IdentityManagementActions.UploadServerCertificate → UploadServerCertificate,
      IdentityManagementActions.UploadSigningCertificate → UploadSigningCertificate
    )
}
