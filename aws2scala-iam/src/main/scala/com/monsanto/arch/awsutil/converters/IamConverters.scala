package com.monsanto.arch.awsutil.converters

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import com.monsanto.arch.awsutil.identitymanagement.model.{AttachRolePolicyRequest, AttachedPolicy, PolicyArn}

/** Provides converters between ''aws2scala-iam'' objects and their AWS Java SDK counterparts. */
object IamConverters {
  implicit class AwsAttachRolePolicyRequest(val request: aws.AttachRolePolicyRequest) extends AnyVal {
    def asScala: AttachRolePolicyRequest =
      AttachRolePolicyRequest(request.getRoleName, PolicyArn(request.getPolicyArn))
  }

  implicit class ScalaAttachRolePolicyRequest(val request: AttachRolePolicyRequest) extends AnyVal {
    def asAws: aws.AttachRolePolicyRequest =
      new aws.AttachRolePolicyRequest()
        .withRoleName(request.roleName)
        .withPolicyArn(request.policyArn.arnString)
  }

  implicit class AwsAttachedPolicy(val attachedPolicy: aws.AttachedPolicy) extends AnyVal {
    def asScala: AttachedPolicy =
      AttachedPolicy(PolicyArn(attachedPolicy.getPolicyArn), attachedPolicy.getPolicyName)
  }

  implicit class ScalaAttachedPolicy(val attachedPolicy: AttachedPolicy) extends AnyVal {
    def asAws: aws.AttachedPolicy =
      new aws.AttachedPolicy()
        .withPolicyArn(attachedPolicy.arn.arnString)
        .withPolicyName(attachedPolicy.name)
  }
}
