package com.monsanto.arch.awsutil.s3.model

import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class PermissionSpec extends FreeSpec with AwsEnumerationBehaviours {
  "the Permission enumeration" - {
    behave like anAwsEnumeration(Permission)
  }
}
