package com.monsanto.arch.awsutil.identitymanagement

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.identitymanagement.model._

private[awsutil] class DefaultAsyncIdentityManagementClient(streaming: StreamingIdentityManagementClient) extends AsyncIdentityManagementClient {
  override def createRole(roleName: String, assumeRolePolicy: Policy)(implicit m: Materializer) =
    Source.single(CreateRoleRequest(roleName, assumeRolePolicy, None))
      .via(streaming.roleCreator)
      .runWith(Sink.head)

  override def createRole(roleName: String, assumeRolePolicy: Policy, path: Path)(implicit m: Materializer) =
    Source.single(CreateRoleRequest(roleName, assumeRolePolicy, Some(path)))
      .via(streaming.roleCreator)
      .runWith(Sink.head)

  override def deleteRole(roleName: String)(implicit m: Materializer) =
    Source.single(roleName)
      .via(streaming.roleDeleter)
      .runWith(Sink.ignore)

  override def getRole(roleName: String)(implicit m: Materializer) =
    Source.single(roleName)
      .via(streaming.roleGetter)
      .runWith(Sink.head)

  override def listRoles()(implicit m: Materializer) =
    Source.single(ListRolesRequest.allRoles)
      .via(streaming.roleLister)
      .runWith(Sink.seq)

  override def listRoles(prefix: Path)(implicit m: Materializer) =
    Source.single(ListRolesRequest.withPrefix(prefix))
      .via(streaming.roleLister)
      .runWith(Sink.seq)

  override def attachRolePolicy(roleName: String, policyArn: PolicyArn)(implicit m: Materializer) =
    Source.single(AttachRolePolicyRequest(roleName, policyArn))
      .via(streaming.rolePolicyAttacher)
      .runWith(Sink.ignore)

  override def detachRolePolicy(roleName: String, policyArn: PolicyArn)(implicit m: Materializer) =
    Source.single(DetachRolePolicyRequest(roleName, policyArn))
      .via(streaming.rolePolicyDetacher)
      .runWith(Sink.ignore)

  override def listAttachedRolePolicies(roleName: String)(implicit m: Materializer) =
    Source.single(ListAttachedRolePoliciesRequest(roleName))
      .via(streaming.attachedRolePolicyLister)
      .runWith(Sink.seq)

  override def listAttachedRolePolicies(roleName: String, prefix: Path)(implicit m: Materializer) =
    Source.single(ListAttachedRolePoliciesRequest(roleName, prefix))
      .via(streaming.attachedRolePolicyLister)
      .runWith(Sink.seq)

  override def getCurrentUser()(implicit m: Materializer) =
    Source.single(GetUserRequest.currentUser)
      .via(streaming.userGetter)
      .runWith(Sink.head)

  override def getUser(name: String)(implicit m: Materializer) =
    Source.single(GetUserRequest.forUserName(name))
      .via(streaming.userGetter)
      .runWith(Sink.head)

  override def createPolicy(name: String, document: Policy)(implicit m: Materializer) =
    Source.single(CreatePolicyRequest(name, document, None, Path.empty))
      .via(streaming.policyCreator)
      .runWith(Sink.head)

  override def createPolicy(name: String, document: Policy, description: String)(implicit m: Materializer) =
    Source.single(CreatePolicyRequest(name, document, Some(description), Path.empty))
      .via(streaming.policyCreator)
      .runWith(Sink.head)

  override def createPolicy(name: String, document: Policy, description: String, path: Path)
                           (implicit m: Materializer) =
    Source.single(CreatePolicyRequest(name, document, Some(description), path))
      .via(streaming.policyCreator)
      .runWith(Sink.head)

  override def deletePolicy(policyArn: PolicyArn)(implicit m: Materializer) =
    Source.single(policyArn)
      .via(streaming.policyDeleter)
      .runWith(Sink.ignore)

  override def getPolicy(policyArn: PolicyArn)(implicit m: Materializer) =
    Source.single(policyArn)
      .via(streaming.policyGetter)
      .runWith(Sink.head)

  override def listPolicies()(implicit m: Materializer) =
    listPolicies(ListPoliciesRequest.allPolicies)

  override def listPolicies(prefix: Path)(implicit m: Materializer) =
    listPolicies(ListPoliciesRequest.withPrefix(prefix))

  override def listPolicies(request: ListPoliciesRequest)(implicit m: Materializer) =
    Source.single(request)
      .via(streaming.policyLister)
      .runWith(Sink.seq)

  override def listLocalPolicies()(implicit m: Materializer) =
    listPolicies(ListPoliciesRequest.localPolicies)

  override def createPolicyVersion(arn: PolicyArn,
                                   document: Policy,
                                   setAsDefault: Boolean)
                                  (implicit m: Materializer) =
    Source.single(CreatePolicyVersionRequest(arn, document, setAsDefault))
      .via(streaming.policyVersionCreator)
      .runWith(Sink.head)

  override def deletePolicyVersion(arn: PolicyArn, versionId: String)(implicit m: Materializer) =
    Source.single(DeletePolicyVersionRequest(arn, versionId))
      .via(streaming.policyVersionDeleter)
      .runWith(Sink.ignore)

  override def getPolicyVersion(arn: PolicyArn, versionId: String)(implicit m: Materializer) =
    Source.single(GetPolicyVersionRequest(arn, versionId))
      .via(streaming.policyVersionGetter)
      .runWith(Sink.head)

  override def listPolicyVersions(arn: PolicyArn)(implicit m: Materializer) =
    Source.single(arn)
      .via(streaming.policyVersionLister)
      .runWith(Sink.seq)

  override def setDefaultPolicyVersion(arn: PolicyArn, versionId: String)(implicit m: Materializer) =
    Source.single(SetDefaultPolicyVersionRequest(arn, versionId))
      .via(streaming.defaultPolicyVersionSetter)
      .runWith(Sink.ignore)
}
