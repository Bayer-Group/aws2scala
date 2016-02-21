package com.monsanto.arch.awsutil.kms.model

import com.amazonaws.services.kms.model.{KeyState ⇒ AWSKeyState}
import org.scalatest.{FreeSpec, Matchers}

class KeyStateSpec extends FreeSpec {
  import Matchers._

  def scalafiedEnum(scala: KeyState, aws: AWSKeyState): Unit = {
    "should have the same string representation as in AWS" in {
      scala.toString shouldBe aws.toString
    }

    "should be convertible to the correct AWS type" in {
      scala.toAws shouldBe aws
    }

    "should be convertible from an AWS string" in {
      KeyState(aws.toString) shouldBe scala
    }

    "should be convertible from the AWS type" in {
      KeyState(aws) shouldBe scala
    }
  }

  "the Enabled state" - {
    behave like scalafiedEnum(KeyState.Enabled, AWSKeyState.Enabled)
  }

  "the Disabled state" - {
    behave like scalafiedEnum(KeyState.Disabled, AWSKeyState.Disabled)
  }

  "the PendingDeletion state" - {
    behave like scalafiedEnum(KeyState.PendingDeletion, AWSKeyState.PendingDeletion)
  }
}
