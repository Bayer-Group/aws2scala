package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class PlatformSpec extends FreeSpec with AwsEnumerationBehaviours {
  "the Platform enumeration" - {
    behave like anAwsEnumeration(Platform)
  }
}
