package com.monsanto.arch.awsutil.kms.model

import com.amazonaws.services.kms.model.KeyUsageType
import org.scalatest.{FreeSpec, Matchers}

class KeyUsageSpec extends FreeSpec {
  import Matchers._

  "the EncryptDecrypt key usage should" - {
    "have the same string representation as in AWS" in {
      KeyUsage.EncryptDecrypt.toString shouldBe KeyUsageType.ENCRYPT_DECRYPT.toString
    }

    "be built from a string" in {
      KeyUsage(KeyUsageType.ENCRYPT_DECRYPT.toString) shouldBe KeyUsage.EncryptDecrypt
    }

    "be built from an AWS type" in {
      KeyUsage(KeyUsageType.ENCRYPT_DECRYPT) shouldBe KeyUsage.EncryptDecrypt
    }

    "convert to the correct AWS type" in {
      KeyUsage.EncryptDecrypt.toAws shouldBe KeyUsageType.ENCRYPT_DECRYPT
    }
  }
}
