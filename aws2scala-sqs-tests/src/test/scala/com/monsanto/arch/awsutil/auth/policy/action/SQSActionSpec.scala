package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.SQSActions
import com.monsanto.arch.awsutil.auth.policy.AwsConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class SQSActionSpec extends FreeSpec with AwsEnumerationBehaviours {
  "an SNSAction object" - {
    // ensure that actions are registered
    SQSAction.registerActions()

    behave like anAwsEnumeration(
      SQSActions.values,
      SQSAction.values,
      (_: SQSAction).asAws.asInstanceOf[SQSActions],
      (_: SQSActions).asScala.asInstanceOf[SQSAction])
  }
}
