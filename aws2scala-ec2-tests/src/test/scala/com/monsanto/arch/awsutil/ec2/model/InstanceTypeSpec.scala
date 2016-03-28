package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class InstanceTypeSpec extends FreeSpec with AwsEnumerationBehaviours {
  "the InstanceType enumeration" - {
    behave like anAwsEnumeration(InstanceType)
  }
}
