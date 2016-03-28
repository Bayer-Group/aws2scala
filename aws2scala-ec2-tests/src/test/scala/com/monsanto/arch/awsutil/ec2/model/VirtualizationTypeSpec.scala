package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class VirtualizationTypeSpec extends FreeSpec with AwsEnumerationBehaviours {
  "the VirtualizationType enumeration" - {
    behave like anAwsEnumeration(VirtualizationType)
  }
}
