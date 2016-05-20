package com.monsanto.arch.awsutil.test_support

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.auth.policy.Action
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.TableDrivenPropertyChecks._

trait ActionBehaviours extends AwsEnumerationBehaviours { this: FreeSpec ⇒
  def anAction[ScalaAction <: Action, AwsAction <: aws.Action](awsActions: Array[AwsAction],
                                                               scalaActions: Seq[ScalaAction],
                                                               asAws: ScalaAction ⇒ AwsAction,
                                                               asScala: AwsAction ⇒ ScalaAction): Unit = {
    val actionsTable = Table("Action", scalaActions: _*)

    "the action name matches its AWS value" in {
      forAll(actionsTable) { action ⇒
        action.name shouldBe asAws(action).getActionName
      }
    }

    "actions can be round-tripped through their name" in {
      forAll(actionsTable) { action ⇒
        Action.fromName.unapply(action.name) shouldBe Some(action)
      }
    }

    anAwsEnumeration(awsActions, scalaActions, asAws, asScala)
  }
}
