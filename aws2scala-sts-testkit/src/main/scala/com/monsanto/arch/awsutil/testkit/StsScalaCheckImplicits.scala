package com.monsanto.arch.awsutil.testkit

import java.util.Date

import com.monsanto.arch.awsutil.Arn
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.identitymanagement.model.RoleArn
import com.monsanto.arch.awsutil.securitytoken.SecurityTokenService
import com.monsanto.arch.awsutil.securitytoken.model._
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalacheck.Arbitrary._
import org.scalacheck.{Arbitrary, Gen, Shrink}

import scala.concurrent.duration.DurationInt

object StsScalaCheckImplicits {
  SecurityTokenService.init()

  implicit lazy val arbAssumeRoleRequest: Arbitrary[AssumeRoleRequest] = {
    Arbitrary {
      for {
        roleArn ← arbitrary[RoleArn].map(_.arnString)
        roleSessionName ← CoreGen.assumedRoleSessionName
        duration ← Gen.option(Gen.choose(900, 3600).map(_.seconds))
        externalId ← Gen.option(StsGen.externalId)
        policy ← arbitrary[Option[Policy]]
        mfa ← arbitrary[Option[AssumeRoleRequest.MFA]]
      } yield AssumeRoleRequest(roleArn, roleSessionName, duration, externalId, policy.map(_.toJson), mfa)
    }
  }

  implicit lazy val shrinkAssumeRoleRequest: Shrink[AssumeRoleRequest] =
    Shrink { request ⇒
      val policy = request.policy.map(p ⇒ Policy.fromJson(p))
      val roleArn = Arn.unapply(request.roleArn).get.asInstanceOf[RoleArn]
      Shrink.shrink(roleArn).map(x ⇒ request.copy(roleArn = x.arnString)) append
        Shrink.shrink(request.roleSessionName).filter(_.length > 1).map(x ⇒ request.copy(roleSessionName = x)) append
        Shrink.shrink(request.duration).map(x ⇒ request.copy(duration = x)) append
        Shrink.shrink(request.externalId).filter(_.forall(_.length > 1)).map(x ⇒ request.copy(externalId = x)) append
        Shrink.shrink(policy).map(p ⇒ request.copy(policy = p.map(_.toJson))) append
        Shrink.shrink(request.mfa).map(m ⇒ request.copy(mfa = m))
    }

  implicit lazy val arbAssumeRoleRequestMfa: Arbitrary[AssumeRoleRequest.MFA] =
    Arbitrary {
      for {
        serial ← Gen.listOfN(20, Gen.alphaNumChar).map(_.mkString)
        token ← Gen.listOfN(6, Gen.numChar).map(_.mkString)
      } yield AssumeRoleRequest.MFA(serial, token)
    }

  implicit lazy val arbAssumeRoleResult: Arbitrary[AssumeRoleResult] =
    Arbitrary {
      for {
        packedPolicySize ← Gen.option(Gen.choose(0, 100))
        result ← Gen.resultOf(AssumeRoleResult(_: AssumedRoleUser, _: Credentials, packedPolicySize))
      } yield result
    }

  implicit lazy val shrinkAssumeRoleResult: Shrink[AssumeRoleResult] =
    Shrink { result ⇒
      Shrink.shrink(result.assumedRoleUser).map(x ⇒ result.copy(assumedRoleUser = x)) append
        Shrink.shrink(result.packedPolicySize)
          .filter(_.forall(n ⇒ n >= 0 && n <= 100))
          .map(x ⇒ result.copy(packedPolicySize = x))
    }

  implicit lazy val arbAssumedRoleUser: Arbitrary[AssumedRoleUser] =
    Arbitrary {
      for {
        roleArn ← arbitrary[AssumedRoleArn]
        roleId ← IamGen.roleId
      }  yield AssumedRoleUser(roleArn.arnString, s"$roleId:${roleArn.sessionName}")
    }

  implicit lazy val shrinkAssumedRoleUser: Shrink[AssumedRoleUser] =
    Shrink { user ⇒
      val arn = Arn.unapply(user.arn).get.asInstanceOf[AssumedRoleArn]
      val roleId = user.assumedRoleId.substring(0, user.assumedRoleId.lastIndexOf(s":${arn.sessionName}"))
      Shrink.shrink(arn).map(arn ⇒ AssumedRoleUser(arn.arnString, s"$roleId:${arn.sessionName}"))
    }

  implicit lazy val arbCredentials: Arbitrary[Credentials] =
    Arbitrary {
      for {
        accessKeyId ← IamGen.sessionAccessKeyId
        secretAccessKey ← UtilGen.stringOf(UtilGen.extendedWordChar, 32, 48)
        sessionToken ← UtilGen.stringOf(UtilGen.extendedWordChar, 64, 1024)
        expiration ← arbitrary[Date]
      } yield Credentials(accessKeyId, secretAccessKey, sessionToken, expiration)
    }
}
