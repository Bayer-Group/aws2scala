package com.monsanto.arch.awsutil.testkit

import java.util.Date

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.identitymanagement.model._
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

object IamGen {
  def role(roleName: Name, assumeRolePolicyDocument: Policy, path: Path = Path(Seq.empty)): Gen[Role] =
    for {
      account ← arbitrary[Account]
      id ← arbitrary[RoleId]
    } yield {
      val arn = RoleArn(account,roleName, path)
      Role(arn.value, roleName.value, path.value, id.value, assumeRolePolicyDocument.toString, new Date)
    }

  val instanceProfileId: Gen[String] = id("AIP")

  val roleId: Gen[String] = id("ARO")

  val sessionAccessKeyId: Gen[String] = id("ASI")

  val userId: Gen[String] = id("AID")

  private def id(prefix: String): Gen[String] =
    Gen.listOfN(18, UtilGen.base36Char)
      .map(_.mkString(prefix, "", ""))
      .suchThat(_.length == 21)
}
