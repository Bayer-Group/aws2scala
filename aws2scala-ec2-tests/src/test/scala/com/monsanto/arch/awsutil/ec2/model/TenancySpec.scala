package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class TenancySpec extends FreeSpec with AwsEnumerationBehaviours {
  "the Tenancy enumeration" - {
    behave like anAwsEnumeration(Tenancy)
  }
}
