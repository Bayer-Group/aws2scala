package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class ArchitectureSpec extends FreeSpec with AwsEnumerationBehaviours {
  "the Architecture enumeration" - {
    behave like anAwsEnumeration(Architecture)
  }
}
