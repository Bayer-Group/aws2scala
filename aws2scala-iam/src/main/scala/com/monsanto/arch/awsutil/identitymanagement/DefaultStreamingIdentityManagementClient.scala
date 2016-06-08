package com.monsanto.arch.awsutil.identitymanagement

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.identitymanagement.{AmazonIdentityManagementAsync, model ⇒ aws}
import com.monsanto.arch.awsutil.converters.IamConverters._
import com.monsanto.arch.awsutil.identitymanagement.model._
import com.monsanto.arch.awsutil.{AWSFlow, AWSFlowAdapter}

import scala.collection.JavaConverters._

private[awsutil] class DefaultStreamingIdentityManagementClient(iam: AmazonIdentityManagementAsync) extends StreamingIdentityManagementClient {
  override val roleLister =
    Flow[ListRolesRequest]
      .map(_.asAws)
      .via[aws.ListRolesResult,NotUsed](AWSFlow.pagedByMarker(iam.listRolesAsync))
      .mapConcat(_.getRoles.asScala.map(_.asScala).toList)
      .named("IAM.roleLister")

  override val roleCreator =
    Flow[CreateRoleRequest]
      .map(_.asAws)
      .via[aws.CreateRoleResult,NotUsed](AWSFlow.simple(iam.createRoleAsync))
      .map(r ⇒ r.getRole.asScala)
      .named("IAM.roleCreator")

  override val roleDeleter =
    Flow[String]
      .map(n ⇒ new aws.DeleteRoleRequest().withRoleName(n))
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput[aws.DeleteRoleRequest,aws.DeleteRoleResult](iam.deleteRoleAsync)))
      .map(_.getRoleName)
      .named("IAM.roleDeleter")

  override val rolePolicyAttacher =
    Flow[AttachRolePolicyRequest]
      .map(_.asAws)
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput[aws.AttachRolePolicyRequest,aws.AttachRolePolicyResult](iam.attachRolePolicyAsync)))
      .map(_.getRoleName)
      .named("IAM.rolePolicyAttacher")

  override val rolePolicyDetacher =
    Flow[DetachRolePolicyRequest]
      .map(_.asAws)
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput[aws.DetachRolePolicyRequest,aws.DetachRolePolicyResult](iam.detachRolePolicyAsync)))
      .map(_.getRoleName)
      .named("IAM.rolePolicyDetacher")

  override val attachedRolePolicyLister =
    Flow[ListAttachedRolePoliciesRequest]
      .map(_.asAws)
      .via[aws.ListAttachedRolePoliciesResult,NotUsed](AWSFlow.pagedByMarker(iam.listAttachedRolePoliciesAsync))
      .mapConcat(_.getAttachedPolicies.asScala.toList)
      .map(_.asScala)
      .named("IAM.attachedRolePolicyLister")

  override val userGetter =
    Flow[GetUserRequest]
      .map(_.asAws)
      .via[aws.GetUserResult,NotUsed](AWSFlow.simple(iam.getUserAsync))
      .map(r ⇒ r.getUser.asScala)
      .named("IAM.userGetter")

  override val policyCreator =
    Flow[CreatePolicyRequest]
      .map(_.asAws)
      .via[aws.CreatePolicyResult,NotUsed](AWSFlow.simple(iam.createPolicyAsync))
      .map(_.getPolicy.asScala)
      .named("IAM.policyCreator")

  override val policyDeleter =
    Flow[PolicyArn]
      .flatMapConcat { arn ⇒
        val request = new aws.DeletePolicyRequest().withPolicyArn(arn.arnString)
        Source.single(request)
          .via[aws.DeletePolicyResult, NotUsed](AWSFlow.simple(iam.deletePolicyAsync))
          .map(_ ⇒ arn)
      }
      .named("IAM.policyDeleter")

  override val policyGetter =
    Flow[PolicyArn]
      .map(arn ⇒ new aws.GetPolicyRequest().withPolicyArn(arn.arnString))
      .via[aws.GetPolicyResult, NotUsed](AWSFlow.simple(iam.getPolicyAsync))
      .map(_.getPolicy.asScala)
      .named("IAM.policyGetter")

  override val policyLister =
    Flow[ListPoliciesRequest]
      .map(_.asAws)
      .via[aws.ListPoliciesResult, NotUsed](AWSFlow.pagedByMarker(iam.listPoliciesAsync))
      .mapConcat(_.getPolicies.asScala.toList.map(_.asScala))
      .named("IAM.policyLister")

  override val policyVersionCreator =
    Flow[CreatePolicyVersionRequest]
      .map(_.asAws)
      .via[aws.CreatePolicyVersionResult,NotUsed](AWSFlow.simple(iam.createPolicyVersionAsync))
      .map(_.getPolicyVersion.asScala)
      .named("IAM.policyVersionCreator")

  override val policyVersionDeleter =
    Flow[DeletePolicyVersionRequest]
      .flatMapConcat { request ⇒
        Source.single(request.asAws)
          .via[aws.DeletePolicyVersionResult,NotUsed](AWSFlow.simple(iam.deletePolicyVersionAsync))
          .map(_ ⇒ request.arn)
      }
      .named("IAM.policyVersionDeleter")
}
