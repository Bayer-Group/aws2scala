package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.test.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class AttachmentStatusSpec extends FreeSpec with AwsEnumerationBehaviours {
  "the AttachmentStatus enumeration" - {
    behave like anAwsEnumeration(Architecture)
  }
}
