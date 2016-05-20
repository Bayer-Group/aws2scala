package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.SQSActions
import com.monsanto.arch.awsutil.converters.CoreConverters._
import com.monsanto.arch.awsutil.sqs.SQS
import com.monsanto.arch.awsutil.test_support.ActionBehaviours
import org.scalatest.FreeSpec

class SQSActionSpec extends FreeSpec with ActionBehaviours {
  "an SNSAction object" - {
    // ensure that actions are registered
    SQS.init()

    behave like anAction(
      SQSActions.values,
      SQSAction.values,
      (_: SQSAction).asAws.asInstanceOf[SQSActions],
      (_: SQSActions).asScala.asInstanceOf[SQSAction])
  }
}
