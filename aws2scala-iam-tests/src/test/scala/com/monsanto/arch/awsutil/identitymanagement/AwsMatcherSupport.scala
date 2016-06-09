package com.monsanto.arch.awsutil.identitymanagement

import java.lang.{Boolean ⇒ JBoolean}

import com.amazonaws.services.identitymanagement.{model ⇒ aws}
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher}

trait AwsMatcherSupport {
  def onlyAttached(expectedValue: Boolean) =
    jBooleanMatcher("onlyAttached", expectedValue, (_: aws.ListPoliciesRequest).getOnlyAttached)

  def setAsDefault(expectedValue: Boolean) =
    jBooleanMatcher("setAsDefault", expectedValue, (_: aws.CreatePolicyVersionRequest).getSetAsDefault)

  private def jBooleanMatcher[T](name: String, expectedValue: Boolean, f: T ⇒ JBoolean) =
    new HavePropertyMatcher[T, JBoolean] {
      val expected = JBoolean.valueOf(expectedValue)

      override def apply(t: T): HavePropertyMatchResult[JBoolean] =
        HavePropertyMatchResult(f(t) == expected, name, expected, f(t))
    }
}
