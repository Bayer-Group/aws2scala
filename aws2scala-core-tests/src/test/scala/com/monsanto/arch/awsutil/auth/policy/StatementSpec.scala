package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.auth.policy.AwsConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class StatementSpec extends FreeSpec with AwsEnumerationBehaviours {
  "the Statement.Effect enumeration" - {
    behave like anAwsEnumeration(
      aws.Statement.Effect.values,
      Statement.Effect.values,
      (_: Statement.Effect).asAws,
      (_: aws.Statement.Effect).asScala)
  }
}
