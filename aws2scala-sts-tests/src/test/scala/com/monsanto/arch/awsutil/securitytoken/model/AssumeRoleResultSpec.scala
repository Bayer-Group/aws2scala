package com.monsanto.arch.awsutil.securitytoken.model

import com.monsanto.arch.awsutil.securitytoken.model.AwsConverters._
import com.monsanto.arch.awsutil.testkit.StsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class AssumeRoleResultSpec extends FreeSpec {
  "an AssumeRoleResult" - {
    "can be round-tripped" - {
      "via its AWS equivalent" in {
        forAll { result: AssumeRoleResult ⇒
          result.asAws.asScala shouldBe result
        }
      }
    }
  }
}
