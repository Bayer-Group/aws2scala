package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.converters.CoreConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class StatementSpec extends FreeSpec with AwsEnumerationBehaviours {
  "Statement object should round-trip via their AWS equivalents" - {
    // register some actions first
    TestAction.registerActions()

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
