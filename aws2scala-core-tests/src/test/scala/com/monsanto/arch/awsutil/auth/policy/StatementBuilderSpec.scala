package com.monsanto.arch.awsutil.auth.policy

import com.monsanto.arch.awsutil.testkit.CoreGen
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class StatementBuilderSpec extends FreeSpec {
  TestAction.registerActions()

  "a StatementBuilder should" - {
    "require an effect" in {
      the [IllegalStateException] thrownBy {
        StatementBuilder.newBuilder.result
      } should have message "A statement should have an effect."
    }

    "reject duplicate invocations of" - {
      "withSid" in {
        forAll(
          CoreGen.statementId → "sid1",
          CoreGen.statementId → "sid2"
        ) { (sid1, sid2) ⇒
          the [IllegalStateException] thrownBy {
            StatementBuilder.newBuilder
              .withSid(sid1)
              .withSid(sid2)
          } should have message "A statement may only have one identifier."
        }
      }

      "withPrincipals" in {
        forAll { (principals1: Set[Principal], principals2: Set[Principal]) ⇒
          the [IllegalStateException] thrownBy {
            StatementBuilder.newBuilder
              .withPrincipals(principals1)
              .withPrincipals(principals2)
          } should have message "A statement may only have one set of principals."
        }
      }

      "withEffect" in {
        forAll { (effect1: Statement.Effect, effect2: Statement.Effect) ⇒
          the [IllegalStateException] thrownBy {
            StatementBuilder.newBuilder
              .withEffect(effect1)
              .withEffect(effect2)
          } should have message "A statement may only have one effect."
        }
      }

      "withActions" in {
        forAll { (actions1: Seq[Action], actions2: Seq[Action]) ⇒
          the [IllegalStateException] thrownBy {
            StatementBuilder.newBuilder
              .withActions(actions1)
              .withActions(actions2)
          } should have message "A statement may only have one list of actions."
        }
      }

      "withResources" in {
        forAll { (resources1: Seq[Resource], resources2: Seq[Resource]) ⇒
          the [IllegalStateException] thrownBy {
            StatementBuilder.newBuilder
              .withResources(resources1)
              .withResources(resources2)
          } should have message "A statement may only have one list of resources."
        }
      }
    }

    "build with" - {
      "only an effect" in {
        forAll { effect: Statement.Effect ⇒
          val result = StatementBuilder.newBuilder
            .withEffect(effect)
            .result

          result shouldBe Statement(None, Set.empty, effect, Seq.empty, Seq.empty, Set.empty)
        }
      }

      "an effect and a statement identifier" in {
        forAll(
          CoreGen.statementId → "sid",
          arbitrary[Statement.Effect] → "effect"
        ) { (sid, effect) ⇒
          val result = StatementBuilder.newBuilder
            .withSid(sid)
            .withEffect(effect)
            .result

          result shouldBe Statement(Some(sid), Set.empty, effect, Seq.empty, Seq.empty, Set.empty)
        }
      }

      "an effect and a set of principals" in {
        forAll { (principals: Set[Principal], effect: Statement.Effect) ⇒
          val result = StatementBuilder.newBuilder
            .withPrincipals(principals)
            .withEffect(effect)
            .result

          result shouldBe Statement(None, principals, effect, Seq.empty, Seq.empty, Set.empty)
        }
      }

      "an effect and a list of actions" in {
        forAll { (actions: Seq[Action], effect: Statement.Effect) ⇒
          val result = StatementBuilder.newBuilder
            .withEffect(effect)
            .withActions(actions)
            .result

          result shouldBe Statement(None, Set.empty, effect, actions, Seq.empty, Set.empty)
        }
      }

      "an effect and a list of resources" in {
        forAll { (resources: Seq[Resource], effect: Statement.Effect) ⇒
          val result = StatementBuilder.newBuilder
            .withEffect(effect)
            .withResources(resources)
            .result

          result shouldBe Statement(None, Set.empty, effect, Seq.empty, resources, Set.empty)
        }
      }

      "an effect and a set of conditions" in {
        forAll { (conditions: Set[Condition], effect: Statement.Effect) ⇒
          val result = StatementBuilder.newBuilder
            .withEffect(effect)
            .withConditions(conditions)
            .result

          result shouldBe Statement(None, Set.empty, effect, Seq.empty, Seq.empty, conditions)
        }
      }
    }
  }
}
