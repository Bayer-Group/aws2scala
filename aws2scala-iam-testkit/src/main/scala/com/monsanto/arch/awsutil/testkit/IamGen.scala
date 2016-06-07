package com.monsanto.arch.awsutil.testkit

import java.util.Date

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.auth.policy
import com.monsanto.arch.awsutil.auth.policy.action.SecurityTokenServiceAction
import com.monsanto.arch.awsutil.auth.policy.{PolicyDSL, Principal}
import com.monsanto.arch.awsutil.identitymanagement.model._
import com.monsanto.arch.awsutil.securitytoken.SecurityTokenService
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import scala.util.Try

/** Handy generators for ''aws2scala-iam'' objects. */
object IamGen {
  SecurityTokenService.init()

  /** Generates a role from the given name, policy, and path. */
  def role(roleName: String, assumeRolePolicyDocument: policy.Policy, path: Path = Path.empty): Gen[Role] =
    for {
      account ← arbitrary[Account]
      id ← roleId
    } yield {
      val arn = RoleArn(account, roleName, path)
      Role(arn, roleName, path, id, assumeRolePolicyDocument, new Date)
    }

  /** Generates a unique identifier for an instance profile. */
  val instanceProfileId: Gen[String] = id("AIP")

  /** Generates a unique identifier for a role. */
  val roleId: Gen[String] = id("ARO")

  /** Generates a unique identifier for a session access key. */
  val sessionAccessKeyId: Gen[String] = id("ASI")

  /** Generates a unique identifier for a user. */
  val userId: Gen[String] = id("AID")

  /** Generates a simple assume role policy with a random principal. */
  val assumeRolePolicy: Gen[policy.Policy] =
    for {
      principal ← arbitrary[Principal]
    } yield {
      import PolicyDSL._

      PolicyDSL.policy (
        statements (
          allow (
            principals(principal),
            actions(SecurityTokenServiceAction.AssumeRole)
          )
        )
      )
    }

  /** Generates a unique identifier for a managed policy. */
  val policyId: Gen[String] = id("ANP")

  /** Generates a managed policy version ID. */
  val policyVersionId: Gen[String] = Gen.posNum[Int].map(n ⇒ s"v$n").suchThat { v ⇒
    v.length >= 2 && v.charAt(0) == 'v' && Try(v.substring(1).toInt >= 1).isSuccess
  }

  /** Used to generate IAM unique identifiers. */
  private def id(prefix: String): Gen[String] =
    Gen.listOfN(18, UtilGen.base36Char)
      .map(_.mkString(prefix, "", ""))
      .suchThat(_.length == 21)
}
