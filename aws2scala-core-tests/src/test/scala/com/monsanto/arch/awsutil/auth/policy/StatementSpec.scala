package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.converters.CoreConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalatest.prop.TableDrivenPropertyChecks.{Table, forAll ⇒ forAllIn}

class StatementSpec extends FreeSpec with AwsEnumerationBehaviours {
  "Statement object should" - {
    // register some actions first
    TestAction.registerActions()

    "round-trip via their AWS equivalents" in {
      forAll { statement: Statement ⇒
        statement.asAws.asScala shouldBe statement
      }
    }

    "reject using AllPrincipals with something else" in {
      forAll { principal: Principal ⇒
        an [IllegalArgumentException] shouldBe thrownBy {
          val principals = Set(principal, Principal.AllPrincipals)
          Statement(None, principals, Statement.Effect.Allow, Seq.empty, Seq.empty, Set.empty)
        }
      }
    }

    "reject using AllActions with something else" in {
      forAll { action: Action ⇒
        an [IllegalArgumentException] shouldBe thrownBy {
          val actions = Seq(action, Action.AllActions)
          Statement(None, Set.empty, Statement.Effect.Allow, actions, Seq.empty, Set.empty)
        }
      }
    }

    "reject using AllResources with something else" in {
      forAll { resource: Resource ⇒
        an [IllegalArgumentException] shouldBe thrownBy {
          val resources = Seq(resource, Resource.AllResources)
          Statement(None, Set.empty, Statement.Effect.Allow, Seq.empty, resources, Set.empty)
        }
      }
    }
  }

  "the Statement.Effect enumeration" - {
    val effects = Table("effect", Statement.Effect.values: _*)

    behave like anAwsEnumeration(
      aws.Statement.Effect.values,
      Statement.Effect.values,
      (_: Statement.Effect).asAws,
      (_: aws.Statement.Effect).asScala)

    "has the same name values as their AWS equivalents" in {
      forAllIn(effects) { effect ⇒
        effect.name shouldBe effect.asAws.name()
      }
    }

    "can extract Effect objects from strings" in {
      forAllIn(effects) { effect ⇒
        Statement.Effect.fromName(effect.name) shouldBe effect
      }
    }
  }
}
