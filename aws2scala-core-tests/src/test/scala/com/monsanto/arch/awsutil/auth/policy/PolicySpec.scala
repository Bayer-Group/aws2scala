package com.monsanto.arch.awsutil.auth.policy

import com.monsanto.arch.awsutil.auth.policy.PolicyDSL._
import com.monsanto.arch.awsutil.converters.CoreConverters
import com.monsanto.arch.awsutil.converters.CoreConverters._
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.UtilGen
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalactic.Equality
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class PolicySpec extends FreeSpec {
  TestAction.registerActions()

  "a Policy can" - {
    "round trip" - {
      "via its AWS equivalent" in {
        forAll { policy: Policy ⇒
          policy.asAws.asScala should equal (policy)
        }
      }

      "via JSON" in {
        // only get principals that can be process by AWS' fromJson
        val nonBrokenPrincipal = arbitrary[Principal].retryUntil(p ⇒ !p.id.contains("-") && p.id != "*")
        val nonBrokenPolicy =
          for {
          // generate a policy with unique condition types
            policy ← arbitrary[Policy]
            // generate a set of principles that will round-trip
            okPrincipals ← Gen.listOfN(policy.statements.size, UtilGen.listOfSqrtN(nonBrokenPrincipal).map(_.toSet))
          } yield {
            // replace all statement principals with round-trippable ones
            val okStatements = policy.statements.zip(okPrincipals).map {
              case (s, p) ⇒ s.copy(principals = p)
            }
            // create a policy that with the OK statements
            policy.copy(statements = okStatements)
          }
        forAll(nonBrokenPolicy, maxSize(50)) { policy ⇒
          Policy.fromJson(policy.toJson) should equal (policy)
        }
      }
    }

    "handle unknown Actions" in {
      val result = Policy.fromJson("{\"Statement\": [{\"Effect\":\"Allow\",\"Action\":\"foo\"}]}")
      result should equal (
        policy(
          statements(
            allow(
              actions(CoreConverters.NamedAction("foo"))
            )
          )
        )
      )
    }
  }

  /** This is used because AWS will automatically insert statement IDs if not
    * specified in the data structure.  Also, on deserialisation, it is possible
    * that principals and credentials will be reordered.
    */
  private implicit val policyEq = new Equality[Policy] {
    override def areEqual(lhs: Policy, rhs: Any): Boolean = {
      rhs match {
        case Policy(version, id, statements) ⇒
          lhs.version == version &&
            lhs.id == id &&
            lhs.statements.size == statements.size  &&
            lhs.statements.zip(statements).forall {
              case (lhsStatement, rhsStatement) ⇒
                (rhsStatement.id.isEmpty || lhsStatement.id == rhsStatement.id) &&
                  rhsStatement.principals.diff(lhsStatement.principals).isEmpty &&
                  lhsStatement.principals.diff(rhsStatement.principals).isEmpty &&
                  rhsStatement.effect == lhsStatement.effect &&
                  rhsStatement.actions == lhsStatement.actions &&
                  rhsStatement.resources == lhsStatement.resources &&
                  rhsStatement.conditions.diff(lhsStatement.conditions).isEmpty &&
                  lhsStatement.conditions.diff(rhsStatement.conditions).isEmpty
            }
        case _ ⇒ false
      }
    }
  }
}
