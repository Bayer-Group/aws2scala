package com.monsanto.arch.awsutil.auth.policy

import com.monsanto.arch.awsutil.testkit.CoreGen
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class PolicyBuilderSpec extends FreeSpec {
  TestAction.registerActions()

  "a PolicyBuilder should" - {
    "require statements" in {
      the [IllegalStateException] thrownBy {
        PolicyBuilder.newBuilder.result
      } should have message "A policy should have a list of statements."
    }

    "require non-empty statements" in {
      the [IllegalArgumentException] thrownBy {
        PolicyBuilder.newBuilder.withStatements(Seq.empty)
      } should have message "The policy’s statement list may not be empty."
    }

    "reject duplicate invocations of" - {
      "withId" in {
        forAll(
          CoreGen.statementId → "id1",
          CoreGen.statementId → "id2"
        ) { (id1, id2) ⇒
          the [IllegalStateException] thrownBy {
            PolicyBuilder.newBuilder
              .withId(id1)
              .withId(id2)
          } should have message "A policy may only have one identifier."
        }
      }

      "withVersion" in {
        forAll { (version1: Policy.Version, version2: Policy.Version) ⇒
          the [IllegalStateException] thrownBy {
            PolicyBuilder.newBuilder
              .withVersion(version1)
              .withVersion(version2)
          } should have message "A policy may only have one version."
        }
      }

      "withStatements" in {
        forAll { (statements1: Seq[Statement], statements2: Seq[Statement]) ⇒
          the [IllegalStateException] thrownBy {
            PolicyBuilder.newBuilder
              .withStatements(statements1)
              .withStatements(statements2)
          } should have message "A policy may only have one list of statements."
        }
      }
    }

    "build with" - {
      "just some statements" in {
        forAll { statements: Seq[Statement] ⇒
          PolicyBuilder.newBuilder.withStatements(statements).result shouldBe Policy(None, None, statements)
        }
      }

      "with statements and statement ID" in {
        forAll(
          CoreGen.statementId → "sid",
          arbitrary[Seq[Statement]] → "statements"
        ) { (sid, statements) ⇒
          val result = PolicyBuilder.newBuilder
            .withStatements(statements)
            .withId(sid)
            .result
          result shouldBe Policy(None, Some(sid), statements)
        }
      }

      "with statements and a version" in {
        forAll(
          arbitrary[Policy.Version] → "version",
          arbitrary[Seq[Statement]] → "statements"
        ) { (version, statements) ⇒
          val result = PolicyBuilder.newBuilder
            .withStatements(statements)
            .withVersion(version)
            .result
          result shouldBe Policy(Some(version), None, statements)
        }
      }

      "with statements, a version, and a statement ID" in {
        forAll(
          arbitrary[Policy.Version] → "version",
          CoreGen.statementId → "sid",
          arbitrary[Seq[Statement]] → "statements"
        ) { (version, sid, statements) ⇒
          val result = PolicyBuilder.newBuilder
            .withStatements(statements)
            .withVersion(version)
            .withId(sid)
            .result
          result shouldBe Policy(Some(version), Some(sid), statements)
        }
      }
    }
  }
}
