package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.IdentityManagementActions
import com.monsanto.arch.awsutil.auth.policy.AwsConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class IdentityManagementActionSpec extends FreeSpec with AwsEnumerationBehaviours {
  "an IdentityManagementAction object" - {
    // ensure that actions are registered
    IdentityManagementAction.registerActions()
    behave like anAwsEnumeration(IdentityManagementActions.values, IdentityManagementAction.values,
      (_: IdentityManagementAction).asAws.asInstanceOf[IdentityManagementActions],
      (_: IdentityManagementActions).asScala.asInstanceOf[IdentityManagementAction])
  }
}
