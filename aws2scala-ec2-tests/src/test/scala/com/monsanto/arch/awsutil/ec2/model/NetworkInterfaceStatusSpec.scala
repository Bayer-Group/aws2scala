package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class NetworkInterfaceStatusSpec extends FreeSpec with AwsEnumerationBehaviours {
  "the NetworkInterfaceStatus enumeration" - {
    behave like anAwsEnumeration(NetworkInterfaceStatus)
  }
}
