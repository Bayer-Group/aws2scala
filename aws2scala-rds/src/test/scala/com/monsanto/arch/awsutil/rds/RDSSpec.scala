package com.monsanto.arch.awsutil.rds

import com.monsanto.arch.awsutil.test_support.AwsClientProviderBehaviours
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec

class RDSSpec extends FreeSpec with MockFactory with AwsClientProviderBehaviours {
  "the RDS provider should" - {
    behave like anAwsClientProvider(RDS)
  }
}
