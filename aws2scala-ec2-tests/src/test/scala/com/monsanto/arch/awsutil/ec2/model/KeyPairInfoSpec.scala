package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.ec2.model.AwsConverters._
import com.monsanto.arch.awsutil.testkit.Ec2ScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class KeyPairInfoSpec extends FreeSpec {
  "a KeyPairInfo should" - {
    "be constructed from an AWS instance" in {
      forAll { keyPairInfo: KeyPairInfo ⇒
        KeyPairInfo.fromAws(keyPairInfo.toAws) shouldBe keyPairInfo
      }
    }
  }
}
