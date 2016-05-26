package com.monsanto.arch.awsutil.testkit

import com.monsanto.arch.awsutil.Arn
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

  def resultFor(request: AssumeRoleRequest): Gen[AssumeRoleResult] =
    for {
      roleId ← IamGen.roleId
      credentials ← arbitrary[Credentials]
      packedPolicySize ← if (request.policy.isDefined) packedPolicySize.map(Some(_)) else Gen.const(None)
    } yield {
      val roleArn = RoleArn.fromArnString(request.roleArn)
      val assumedRoleArn = AssumedRoleArn(roleArn.account, roleArn.name, request.roleSessionName)
      val assumedRoleUser = AssumedRoleUser(assumedRoleArn.arnString, s"$roleId:${request.roleSessionName}")
      AssumeRoleResult(assumedRoleUser, credentials, packedPolicySize)
    }
}
