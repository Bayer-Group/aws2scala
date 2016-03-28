package com.monsanto.arch.awsutil.auth.policy.action

import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class SNSActionSpec extends FreeSpec with AwsEnumerationBehaviours {
  "an SNSAction object" - {
    behave like anAwsEnumeration(SNSAction)
  }
}
