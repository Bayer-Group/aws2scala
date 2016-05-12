package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.SNSActions
import com.monsanto.arch.awsutil.converters.CoreConverters._
import com.monsanto.arch.awsutil.sns.SNS
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class SNSActionSpec extends FreeSpec with AwsEnumerationBehaviours {
  "an SNSAction object" - {
    // ensure that actions are registered
    SNS.init()

    behave like anAwsEnumeration(SNSActions.values, SNSAction.values, (_: SNSAction).asAws.asInstanceOf[SNSActions],
      (_: SNSActions).asScala.asInstanceOf[SNSAction])
  }
}
