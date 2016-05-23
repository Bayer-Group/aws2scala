package com.monsanto.arch.awsutil.auth.policy

import com.monsanto.arch.awsutil.converters.CoreConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalactic.Equality
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class PolicySpec extends FreeSpec with AwsEnumerationBehaviours {
  TestAction.registerActions()

  "a Policy can" - {
    "round trip" - {
      "via its AWS equivalent" in {
        forAll { policy: Policy ⇒
          policy.asAws.asScala should equal (policy.copy(version = Some(Policy.Version.`2012-10-17`)))
        }
      }

      "via JSON" in {
        forAll { policy: Policy ⇒
          Policy.fromJson(policy.toJson) shouldBe policy
        }
      }
    }
  }

  "a Policy.Version should" - {
    behave like anAwsEnumeration(
      Array("2008-10-17", "2012-10-17"),
      Policy.Version.values,
      (_: Policy.Version).asAws,
      (_: String).asScalaPolicyVersion)
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
                  rhsStatement.principals == lhsStatement.principals &&
                  rhsStatement.effect == lhsStatement.effect &&
                  rhsStatement.actions == lhsStatement.actions &&
                  rhsStatement.resources == lhsStatement.resources &&
                  rhsStatement.conditions == lhsStatement.conditions
            }
        case _ ⇒ false
      }
    }
  }
}
