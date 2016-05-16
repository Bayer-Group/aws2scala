package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}

sealed abstract class TestAction(actionName: String) extends Action with aws.Action {
  override def getActionName: String = actionName
}

object TestAction {
  case object TestAction1 extends TestAction("TestAction1")
  case object TestAction2 extends TestAction("TestAction2")
  case object TestAction3 extends TestAction("TestAction3")
  case object TestAction4 extends TestAction("TestAction4")
  case object TestAction5 extends TestAction("TestAction5")

  def registerActions(): Unit = {
    Action.registerActions(
      TestAction1 → TestAction1,
      TestAction2 → TestAction2,
      TestAction3 → TestAction3,
      TestAction4 → TestAction4,
      TestAction5 → TestAction5
    )
  }
}

