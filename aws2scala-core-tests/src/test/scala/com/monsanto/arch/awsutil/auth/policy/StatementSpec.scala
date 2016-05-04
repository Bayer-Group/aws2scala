package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.auth.policy.AwsConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class StatementSpec extends FreeSpec with AwsEnumerationBehaviours {
  "Statement object should round-trip via their AWS equivalents" in {
    // register some actions first
    StatementSpec.registerActions()

    forAll { statement: Statement ⇒
      statement.asAws.asScala shouldBe statement
    }
  }

  "the Statement.Effect enumeration" - {
    behave like anAwsEnumeration(
      aws.Statement.Effect.values,
      Statement.Effect.values,
      (_: Statement.Effect).asAws,
      (_: aws.Statement.Effect).asScala)
  }
}

object StatementSpec {
  sealed abstract class TestAction(actionName: String) extends Action with aws.Action {
    override def getActionName: String = actionName
  }

  case object TestAction1 extends TestAction("TestAction1")
  case object TestAction2 extends TestAction("TestAction2")
  case object TestAction3 extends TestAction("TestAction3")
  case object TestAction4 extends TestAction("TestAction4")
  case object TestAction5 extends TestAction("TestAction5")

  def registerActions(): Unit = {
    Action.registerActions(
      StatementSpec.TestAction1 → StatementSpec.TestAction1,
      StatementSpec.TestAction2 → StatementSpec.TestAction2,
      StatementSpec.TestAction3 → StatementSpec.TestAction3,
      StatementSpec.TestAction4 → StatementSpec.TestAction4,
      StatementSpec.TestAction5 → StatementSpec.TestAction5
    )
  }
}
