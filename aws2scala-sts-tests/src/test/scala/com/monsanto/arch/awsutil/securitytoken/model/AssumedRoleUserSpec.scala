package com.monsanto.arch.awsutil.securitytoken.model

import com.monsanto.arch.awsutil.securitytoken.model.AwsConverters._
import com.monsanto.arch.awsutil.testkit.StsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class AssumedRoleUserSpec extends FreeSpec {
  "a AssumedRoleUser can be round-tripped" - {
    "via its AWS equivalent" in {
      forAll { assumedRoleUser: AssumedRoleUser ⇒
        assumedRoleUser.asAws.asScala shouldBe assumedRoleUser
      }
    }
  }
}
