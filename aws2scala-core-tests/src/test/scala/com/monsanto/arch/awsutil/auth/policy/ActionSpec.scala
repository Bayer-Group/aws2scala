package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.converters.CoreConverters._
import com.monsanto.arch.awsutil.test_support.ActionBehaviours
import org.scalatest.FreeSpec
import org.scalatest.Matchers._

class ActionSpec extends FreeSpec with ActionBehaviours {
  "there is an AllActions that" - {
    behave like anAction(
      Array(Action.AllActions),
      Seq(Action.AllActions),
      (_: Action).asAws.asInstanceOf[Action.AllActions.type],
      (_: aws.Action).asScala.asInstanceOf[Action.AllActions.type])
  }

  "it is not possible to get an action from a bad name" in {
    an [IllegalArgumentException] shouldBe thrownBy {
      Action("this is not a valid action")
    }
  }
}
