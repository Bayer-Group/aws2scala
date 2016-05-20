package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.IdentityManagementActions
import com.monsanto.arch.awsutil.converters.CoreConverters._
import com.monsanto.arch.awsutil.identitymanagement.IdentityManagement
import com.monsanto.arch.awsutil.test_support.ActionBehaviours
import org.scalatest.FreeSpec

class IdentityManagementActionSpec extends FreeSpec with ActionBehaviours {
  "an IdentityManagementAction object" - {
    // ensure that actions are registered
    IdentityManagement.init()

    behave like anAction(IdentityManagementActions.values, IdentityManagementAction.values,
      (_: IdentityManagementAction).asAws.asInstanceOf[IdentityManagementActions],
      (_: IdentityManagementActions).asScala.asInstanceOf[IdentityManagementAction])
  }
}
