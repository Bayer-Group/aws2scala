package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.converters.CoreConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

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
          Statement(None, principals, Statement.Effect.Allow, Seq.empty, Seq.empty, Seq.empty)
        }
      }
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
