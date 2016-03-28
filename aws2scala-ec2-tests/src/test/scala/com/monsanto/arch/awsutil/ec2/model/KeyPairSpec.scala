package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.ec2.model.AwsConverters._
import com.monsanto.arch.awsutil.testkit.Ec2ScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class KeyPairSpec extends FreeSpec {
  "a KeyPair should" - {
    "be constructed from an AWS instance" in {
      forAll { keyPair: KeyPair ⇒
        KeyPair.fromAws(keyPair.toAws) shouldBe keyPair
      }
    }
  }
}
