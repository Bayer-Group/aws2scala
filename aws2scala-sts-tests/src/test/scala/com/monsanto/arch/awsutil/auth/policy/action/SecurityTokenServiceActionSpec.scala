package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.SecurityTokenServiceActions
import com.monsanto.arch.awsutil.auth.policy.AwsConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class SecurityTokenServiceActionSpec extends FreeSpec with AwsEnumerationBehaviours {
  "an SecurityTokenServiceAction object" - {
    // ensure that actions are registered
    SecurityTokenServiceAction.registerActions()
    behave like anAwsEnumeration(SecurityTokenServiceActions.values, SecurityTokenServiceAction.values,
      (_: SecurityTokenServiceAction).asAws.asInstanceOf[SecurityTokenServiceActions],
      (_: SecurityTokenServiceActions).asScala.asInstanceOf[SecurityTokenServiceAction])
  }
}
