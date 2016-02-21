package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.model.{KeyPair ⇒ AwsKeyPair}
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class KeyPairSpec extends FreeSpec {
  "a KeyPair should" - {
    "be constructed from an AWS instance" in {
      forAll { args: EC2Gen.KeyPairArgs ⇒
        val aws = new AwsKeyPair()
          .withKeyName(args.name.value)
          .withKeyFingerprint(args.fingerprint.value)
          .withKeyMaterial(args.key.value)

        KeyPair.fromAws(aws) shouldBe args.toKeyPair
      }
    }
  }
}
