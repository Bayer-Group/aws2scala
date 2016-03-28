package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class AffinitySpec extends FreeSpec with AwsEnumerationBehaviours {
  "the Affinity enumeration" - {
    behave like anAwsEnumeration(Affinity)
  }
}
