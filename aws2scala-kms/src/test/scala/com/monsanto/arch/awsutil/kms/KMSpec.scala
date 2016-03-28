package com.monsanto.arch.awsutil.kms

import com.monsanto.arch.awsutil.test_support.AwsClientProviderBehaviours
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec

class KMSpec extends FreeSpec with MockFactory with AwsClientProviderBehaviours {
  "the KMS provider should" - {
    behave like anAwsClientProvider(KMS)
  }
}
