package com.monsanto.arch.awsutil.s3.model

import com.monsanto.arch.awsutil.s3.model.AwsConverters._
import com.monsanto.arch.awsutil.testkit.S3ScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class GrantSpec extends FreeSpec {
  "a Grant can be round-tripped via its AWS equivalent" in {
    forAll { grant: Grant ⇒
      grant.asAws.asScala shouldBe grant
    }
  }
}
