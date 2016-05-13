package com.monsanto.arch.awsutil.identitymanagement

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.monsanto.arch.awsutil.identitymanagement.model._

private[awsutil] class DefaultAsyncIdentityManagementClient(streaming: StreamingIdentityManagementClient) extends AsyncIdentityManagementClient {
  override def createRole(roleName: String, assumeRolePolicy: String)(implicit m: Materializer) =
    Source.single(CreateRoleRequest(roleName, assumeRolePolicy, None))
      .via(streaming.roleCreator)
      .runWith(Sink.head)

  override def createRole(roleName: String, assumeRolePolicy: String, path: String)(implicit m: Materializer) =
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

  override def listRoles(pathPrefix: String)(implicit m: Materializer) =
    Source.single(ListRolesRequest.withPathPrefix(pathPrefix))
      .via(streaming.roleLister)
      .runWith(Sink.seq)

  override def attachRolePolicy(roleName: String, policyArn: PolicyArn)(implicit m: Materializer) =
    Source.single(AttachRolePolicyRequest(roleName, policyArn))
      .via(streaming.rolePolicyAttacher)
      .runWith(Sink.ignore)

  override def detachRolePolicy(roleName: String, policyArn: String)(implicit m: Materializer) =
    Source.single(DetachRolePolicyRequest(roleName, policyArn))
      .via(streaming.rolePolicyDetacher)
      .runWith(Sink.ignore)

  override def listAttachedRolePolicies(roleName: String)(implicit m: Materializer) =
    Source.single(ListAttachedRolePoliciesRequest(roleName))
      .via(streaming.attachedRolePolicyLister)
      .runWith(Sink.seq)

  override def listAttachedRolePolicies(roleName: String, pathPrefix: String)(implicit m: Materializer) =
    Source.single(ListAttachedRolePoliciesRequest(roleName, pathPrefix))
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
}
