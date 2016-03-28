package com.monsanto.arch.awsutil.auth.policy

import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class StatementSpec extends FreeSpec with AwsEnumerationBehaviours {
  "the Statement.Effect enumeration" - {
    behave like anAwsEnumeration(Statement.Effect)
  }
}
