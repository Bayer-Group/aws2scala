package com.monsanto.arch.awsutil.kms.model

import com.amazonaws.services.kms.{model ⇒ aws}
import com.monsanto.arch.awsutil.converters.KmsConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.TableDrivenPropertyChecks.{Table, forAll}

class KeyStateSpec extends FreeSpec with AwsEnumerationBehaviours {
  "a KeyState should" - {
    val keyStates = Table("key state", KeyState.values: _*)

    "have an ID that matches AWS string enumeration name" in {
      forAll(keyStates) { keyState ⇒
        keyState.name shouldBe keyState.asAws.name()
      }
    }

    "be buildable from an identifier string" in {
      forAll(keyStates) { keyState ⇒
        KeyState.fromName(keyState.name) shouldBe keyState
      }
    }

    "not be buildable from an invalid identifier string" in {
      an [IllegalArgumentException] shouldBe thrownBy {
        KeyState.fromName("foo")
      }
    }
  }

  behave like anAwsEnumeration(
    aws.KeyState.values(),
    KeyState.values,
    (_: KeyState).asAws,
    (_: aws.KeyState).asScala)
}
