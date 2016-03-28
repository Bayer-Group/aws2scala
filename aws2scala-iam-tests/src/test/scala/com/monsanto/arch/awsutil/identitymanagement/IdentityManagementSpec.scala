package com.monsanto.arch.awsutil.identitymanagement

import com.monsanto.arch.awsutil.test_support.AwsClientProviderBehaviours
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec

class IdentityManagementSpec extends FreeSpec with MockFactory with AwsClientProviderBehaviours {
  "the IdentityManagement provider should" - {
    behave like anAwsClientProvider(IdentityManagement)
  }
}
