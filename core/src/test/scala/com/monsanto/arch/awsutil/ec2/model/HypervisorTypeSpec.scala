package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.test.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class HypervisorTypeSpec extends FreeSpec with AwsEnumerationBehaviours {
  "the HypervisorType enumeration" - {
    behave like anAwsEnumeration(HypervisorType)
  }
}
