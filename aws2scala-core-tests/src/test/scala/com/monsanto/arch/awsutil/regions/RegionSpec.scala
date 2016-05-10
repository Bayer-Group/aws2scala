package com.monsanto.arch.awsutil.regions

import com.amazonaws.{regions ⇒ aws}
import com.monsanto.arch.awsutil.regions.AwsConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec

class RegionSpec extends FreeSpec with AwsEnumerationBehaviours {
  "the Region enumeration" - {
    behave like anAwsEnumeration(
      aws.Regions.values(),
      Region.values,
      (_: Region).asAws,
      (_: aws.Regions).asScala)
  }
}
