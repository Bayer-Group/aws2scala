package com.monsanto.arch.awsutil.testkit

import com.monsanto.arch.awsutil.identitymanagement.model.RoleArn
import com.monsanto.arch.awsutil.securitytoken.model._
import com.monsanto.arch.awsutil.testkit.StsScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

object StsGen {
  val externalId: Gen[String] = {
    val externalIdChars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ "_+=,.@:\\/-".toList
    UtilGen.stringOf(Gen.oneOf(externalIdChars), 2, 1224).suchThat(_.length > 1)
  }

  val packedPolicySize: Gen[Int] = Gen.choose(0, 100)

  val roleSessionName: Gen[String] = UtilGen.stringOf(UtilGen.extendedWordChar, 2, 64).suchThat(_.length > 1)

  def resultFor(request: AssumeRoleRequest): Gen[AssumeRoleResult] =
    for {
      roleId ← IamGen.roleId
      credentials ← arbitrary[Credentials]
      packedPolicySize ← if (request.policy.isDefined) packedPolicySize.map(Some(_)) else Gen.const(None)
    } yield {
      val roleArn = RoleArn(request.roleArn)
      val assumedRoleArn = AssumedRoleArn(roleArn.account, roleArn.name.value, request.roleSessionName)
      val assumedRoleUser = AssumedRoleUser(assumedRoleArn.value, s"$roleId:${request.roleSessionName}")
      AssumeRoleResult(assumedRoleUser, credentials, packedPolicySize)
    }
}
