package com.monsanto.arch.awsutil.converters

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.identitymanagement.model._

/** Provides converters between ''aws2scala-iam'' objects and their AWS Java SDK counterparts. */
object IamConverters {
  implicit class AwsAttachRolePolicyRequest(val request: aws.AttachRolePolicyRequest) extends AnyVal {
    def asScala: AttachRolePolicyRequest =
      AttachRolePolicyRequest(request.getRoleName, PolicyArn.fromArnString(request.getPolicyArn))
  }

  implicit class ScalaAttachRolePolicyRequest(val request: AttachRolePolicyRequest) extends AnyVal {
    def asAws: aws.AttachRolePolicyRequest =
      new aws.AttachRolePolicyRequest()
        .withRoleName(request.roleName)
        .withPolicyArn(request.policyArn.arnString)
  }

  implicit class AwsAttachedPolicy(val attachedPolicy: aws.AttachedPolicy) extends AnyVal {
    def asScala: AttachedPolicy =
      AttachedPolicy(PolicyArn.fromArnString(attachedPolicy.getPolicyArn), attachedPolicy.getPolicyName)
  }

  implicit class ScalaAttachedPolicy(val attachedPolicy: AttachedPolicy) extends AnyVal {
    def asAws: aws.AttachedPolicy =
      new aws.AttachedPolicy()
        .withPolicyArn(attachedPolicy.arn.arnString)
        .withPolicyName(attachedPolicy.name)
  }

  implicit class AwsCreateRoleRequest(val request: aws.CreateRoleRequest) extends AnyVal {
    def asScala: CreateRoleRequest =
      CreateRoleRequest(
        request.getRoleName,
        Policy.fromJson(request.getAssumeRolePolicyDocument),
        Option(request.getPath).map(Path.fromPathString(_)))
  }

  implicit class ScalaCreateRoleRequest(val request: CreateRoleRequest) extends AnyVal {
    def asAws: aws.CreateRoleRequest = {
      val awsRequest = new aws.CreateRoleRequest()
        .withRoleName(request.name)
        .withAssumeRolePolicyDocument(request.assumeRolePolicy.toJson)
      request.path.foreach(p ⇒ awsRequest.setPath(p.pathString))
      awsRequest
    }
  }
}
