package com.monsanto.arch.awsutil.identitymanagement

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementAsync
import com.amazonaws.services.identitymanagement.model._
import com.monsanto.arch.awsutil.converters.IamConverters._
import com.monsanto.arch.awsutil.identitymanagement.model.{AttachRolePolicyRequest, CreateRoleRequest, DetachRolePolicyRequest, GetUserRequest, ListAttachedRolePoliciesRequest, ListRolesRequest, Role, User}
import com.monsanto.arch.awsutil.{AWSFlow, AWSFlowAdapter}

import scala.collection.JavaConverters._

private[awsutil] class DefaultStreamingIdentityManagementClient(aws: AmazonIdentityManagementAsync) extends StreamingIdentityManagementClient {
  override val roleLister =
    Flow[ListRolesRequest]
      .map(_.toAws)
      .via[ListRolesResult,NotUsed](AWSFlow.pagedByMarker(aws.listRolesAsync))
      .mapConcat(_.getRoles.asScala.map(Role.fromAws).toList)
      .named("IAM.roleLister")

  override val roleCreator =
    Flow[CreateRoleRequest]
      .map(_.asAws)
      .via[CreateRoleResult,NotUsed](AWSFlow.simple(aws.createRoleAsync))
      .map(r ⇒ Role.fromAws(r.getRole))
      .named("IAM.roleCreator")

  override val roleDeleter =
    Flow[String]
      .map(n ⇒ new DeleteRoleRequest().withRoleName(n))
      .via(AWSFlow.simple(AWSFlowAdapter.devoid(aws.deleteRoleAsync)))
      .map(_.getRoleName)
      .named("IAM.roleDeleter")

  override val rolePolicyAttacher =
    Flow[AttachRolePolicyRequest]
      .map(_.asAws)
      .via(AWSFlow.simple(AWSFlowAdapter.devoid(aws.attachRolePolicyAsync)))
      .map(_.getRoleName)
      .named("IAM.rolePolicyAttacher")

  override val rolePolicyDetacher =
    Flow[DetachRolePolicyRequest]
      .map(_.toAws)
      .via(AWSFlow.simple(AWSFlowAdapter.devoid(aws.detachRolePolicyAsync)))
      .map(_.getRoleName)
      .named("IAM.rolePolicyDetacher")

  override val attachedRolePolicyLister =
    Flow[ListAttachedRolePoliciesRequest]
      .map(_.toAws)
      .via[ListAttachedRolePoliciesResult,NotUsed](AWSFlow.pagedByMarker(aws.listAttachedRolePoliciesAsync))
      .mapConcat(_.getAttachedPolicies.asScala.toList)
      .map(_.asScala)
      .named("IAM.attachedRolePolicyLister")

  override val userGetter =
    Flow[GetUserRequest]
      .map(_.toAws)
      .via[GetUserResult,NotUsed](AWSFlow.simple(aws.getUserAsync))
      .map(r ⇒ User.fromAws(r.getUser))
      .named("IAM.userGetter")
}
