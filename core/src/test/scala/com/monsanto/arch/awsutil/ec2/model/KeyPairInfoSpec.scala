package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.model.{KeyPairInfo ⇒ AwsKeyPairInfo}
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class KeyPairInfoSpec extends FreeSpec {
  "a KeyPairInfo should" - {
    "be constructed from an AWS instance" in {
      forAll { args: EC2Gen.KeyPairInfoArgs ⇒
        val aws = new AwsKeyPairInfo()
          .withKeyName(args.name.value)
          .withKeyFingerprint(args.fingerprint.value)

        KeyPairInfo.fromAws(aws) shouldBe args.toKeyPairInfo
      }
    }
  }
}
