package com.monsanto.arch.awsutil.kms.model

import com.amazonaws.services.kms.model.{DataKeySpec ⇒ AWSDataKeySpec}
import org.scalatest.{FreeSpec, Matchers}

class DataKeySpecSpec extends FreeSpec {
  import Matchers._

  def scalafiedEnum(scala: DataKeySpec, aws: AWSDataKeySpec): Unit = {
    "should have the same string representation as in AWS" in {
      scala.toString shouldBe aws.toString
    }

    "should be convertible to the correct AWS type" in {
      scala.toAws shouldBe aws
    }

    "should be convertible from an AWS string" in {
      DataKeySpec(aws.toString) shouldBe scala
    }

    "should be convertible from the AWS type" in {
      DataKeySpec(aws) shouldBe scala
    }
  }

  "the AES 128-bit spec" - {
    behave like scalafiedEnum(DataKeySpec.Aes128, AWSDataKeySpec.AES_128)
  }

  "the AES 256-bit spec" - {
    behave like scalafiedEnum(DataKeySpec.Aes256, AWSDataKeySpec.AES_256)
  }
}
