package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.SecurityTokenServiceActions
import com.monsanto.arch.awsutil.converters.CoreConverters._
import com.monsanto.arch.awsutil.securitytoken.SecurityTokenService
import com.monsanto.arch.awsutil.test_support.ActionBehaviours
import org.scalatest.FreeSpec

class SecurityTokenServiceActionSpec extends FreeSpec with ActionBehaviours {
  "an SecurityTokenServiceAction object" - {
    // ensure that actions are registered
    SecurityTokenService.init()
    behave like anAction(SecurityTokenServiceActions.values, SecurityTokenServiceAction.values,
      (_: SecurityTokenServiceAction).asAws.asInstanceOf[SecurityTokenServiceActions],
      (_: SecurityTokenServiceActions).asScala.asInstanceOf[SecurityTokenServiceAction])
  }
}
