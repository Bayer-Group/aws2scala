package com.monsanto.arch.awsutil.testkit

import java.util.Date

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.auth.policy.action.SecurityTokenServiceAction
import com.monsanto.arch.awsutil.identitymanagement.model.{Name, RoleArn}
import com.monsanto.arch.awsutil.securitytoken.model._
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalacheck.Arbitrary._
import org.scalacheck.{Arbitrary, Gen, Shrink}

import scala.concurrent.duration.DurationInt

object StsScalaCheckImplicits {
  SecurityTokenServiceAction.registerActions()

  implicit lazy val arbAssumeRoleRequest: Arbitrary[AssumeRoleRequest] = {
    Arbitrary {
      for {
        roleArn ← arbitrary[RoleArn].map(_.value)
        roleSessionName ← StsGen.roleSessionName
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
      Shrink.shrink(RoleArn(request.roleArn)).map(x ⇒ request.copy(roleArn = x.value)) append
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

  implicit lazy val arbAssumedRoleArn: Arbitrary[AssumedRoleArn] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        roleName ← arbitrary[Name].map(_.value)
        sessionName ← StsGen.roleSessionName
      } yield AssumedRoleArn(account, roleName, sessionName)
    }

  implicit lazy val shrinkAssumedRoleArn: Shrink[AssumedRoleArn] =
    Shrink { arn ⇒
      Shrink.shrink(arn.roleName).filter(_.nonEmpty).map(x ⇒ arn.copy(roleName = x)) append
        Shrink.shrink(arn.sessionName).filter(_.length > 1).map(x ⇒ arn.copy(sessionName = x))
    }

  implicit lazy val arbAssumedRoleUser: Arbitrary[AssumedRoleUser] =
    Arbitrary {
      for {
        roleArn ← arbitrary[AssumedRoleArn]
        roleId ← IamGen.roleId
      }  yield AssumedRoleUser(roleArn.value, s"$roleId:${roleArn.sessionName}")
    }

  implicit lazy val shrinkAssumedRoleUser: Shrink[AssumedRoleUser] =
    Shrink { user ⇒
      val arn = AssumedRoleArn(user.arn)
      val roleId = user.assumedRoleId.substring(0, user.assumedRoleId.lastIndexOf(s":${arn.sessionName}"))
      Shrink.shrink(arn).map(arn ⇒ AssumedRoleUser(arn.value, s"$roleId:${arn.sessionName}"))
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
