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
}
