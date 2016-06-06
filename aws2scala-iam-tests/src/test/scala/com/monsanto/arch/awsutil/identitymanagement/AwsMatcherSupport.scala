package com.monsanto.arch.awsutil.identitymanagement

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher}

trait AwsMatcherSupport {
  def onlyAttached(expectedValue: Boolean) =
    new HavePropertyMatcher[aws.ListPoliciesRequest, Boolean] {
      override def apply(request: aws.ListPoliciesRequest): HavePropertyMatchResult[Boolean] =
        HavePropertyMatchResult(
          Option(request.getOnlyAttached).contains(java.lang.Boolean.valueOf(expectedValue)),
          "onlyAttached",
          expectedValue,
          request.getOnlyAttached.booleanValue())
    }
}
