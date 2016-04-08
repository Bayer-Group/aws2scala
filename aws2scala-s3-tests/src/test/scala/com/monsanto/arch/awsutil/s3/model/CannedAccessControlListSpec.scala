package com.monsanto.arch.awsutil.s3.model

import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class CannedAccessControlListSpec extends FreeSpec with AwsEnumerationBehaviours {
  "the CannedAccessControlListSpec enumeration" - {
    behave like anAwsEnumeration(CannedAccessControlList)
  }
}
