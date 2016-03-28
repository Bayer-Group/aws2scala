package com.monsanto.arch.awsutil.regions

import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class RegionSpec extends FreeSpec with AwsEnumerationBehaviours {
  "the Region enumeration" - {
    behave like anAwsEnumeration(Region)
  }
}
