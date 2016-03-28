package com.monsanto.arch.awsutil.securitytoken

import com.monsanto.arch.awsutil.test_support.AwsClientProviderBehaviours
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec

class SecurityTokenServiceSpec extends FreeSpec with MockFactory with AwsClientProviderBehaviours {
  "the SecurityTokenService provider should" - {
    behave like anAwsClientProvider(SecurityTokenService)
  }
}
