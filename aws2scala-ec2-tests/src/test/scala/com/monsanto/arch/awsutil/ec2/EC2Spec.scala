package com.monsanto.arch.awsutil.ec2

import com.monsanto.arch.awsutil.test_support.AwsClientProviderBehaviours
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec

class EC2Spec extends FreeSpec with MockFactory with AwsClientProviderBehaviours {
  "the EC2 provider should" - {
    behave like anAwsClientProvider(EC2)
  }
}
