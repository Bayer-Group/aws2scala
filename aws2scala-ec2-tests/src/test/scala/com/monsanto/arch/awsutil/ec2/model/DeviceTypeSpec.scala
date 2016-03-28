package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class DeviceTypeSpec extends FreeSpec with AwsEnumerationBehaviours {
  "the DeviceType enumeration" - {
    behave like anAwsEnumeration(DeviceType)
  }
}
