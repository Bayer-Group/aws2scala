package com.monsanto.arch.awsutil.testkit

import java.util.Date

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.identitymanagement.IdentityManagement
import com.monsanto.arch.awsutil.identitymanagement.model._
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalacheck.Arbitrary._
import org.scalacheck.{Arbitrary, Gen, Shrink}

object IamScalaCheckImplicits {
  IdentityManagement.init()

  implicit lazy val arbAttachRolePolicyRequest: Arbitrary[AttachRolePolicyRequest] =
    Arbitrary {
      for {
        roleName ← CoreGen.iamName
        policyArn ← arbitrary[PolicyArn]
      } yield AttachRolePolicyRequest(roleName, policyArn)
    }

  implicit lazy val shrinkAttachRolePolicyRequest: Shrink[AttachRolePolicyRequest] =
    Shrink { request ⇒
      Shrink.shrink(request.policyArn).map(x ⇒ request.copy(policyArn = x)) append
        Shrink.shrink(request.roleName).filter(_.nonEmpty).map(x ⇒ request.copy(roleName = x))
    }

  implicit lazy val arbCreateRoleRequest: Arbitrary[CreateRoleRequest] =
    Arbitrary {
      for {
        name ← CoreGen.iamName
        assumeRolePolicy ← IamGen.assumeRolePolicy
        path ← arbitrary[Option[Path]]
      } yield CreateRoleRequest(name, assumeRolePolicy, path)
    }

  implicit lazy val shrinkCreateRoleRequest: Shrink[CreateRoleRequest] =
    Shrink { request ⇒
      Shrink.shrink(request.name).filter(_.nonEmpty).map(x ⇒ request.copy(name = x)) append
        Shrink.shrink(request.assumeRolePolicy)
          .filterNot(p ⇒ p == request.assumeRolePolicy)
          .map(policy ⇒ request.copy(assumeRolePolicy = policy)) append
        Shrink.shrink(request.path).map(p ⇒ request.copy(path = p))
    }

  implicit lazy val arbDetachRolePolicyRequest: Arbitrary[DetachRolePolicyRequest] =
    Arbitrary {
      for {
        roleName ← CoreGen.iamName
        policyArn ← arbitrary[PolicyArn]
      } yield DetachRolePolicyRequest(roleName, policyArn.arnString)
    }

  implicit lazy val shrinkDetachRolePolicyRequest: Shrink[DetachRolePolicyRequest] =
    Shrink { request ⇒
      Shrink.shrink(request.roleName).filter(_.nonEmpty).map(x ⇒ request.copy(roleName = x)) append
        Shrink.shrink(PolicyArn.fromArnString(request.policyArn)).map(x ⇒ request.copy(policyArn = x.arnString))
    }

  implicit lazy val arbInstanceProfileArn: Arbitrary[InstanceProfileArn] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        name ← CoreGen.iamName
        path ← arbitrary[Path]
      } yield InstanceProfileArn(account, name, path)
    }

  implicit lazy val shrinkInstanceProfileArn: Shrink[InstanceProfileArn] =
    Shrink { arn ⇒
      Shrink.shrink(arn.account).map(x ⇒ arn.copy(account = x)) append
        Shrink.shrink(arn.name).filter(_.nonEmpty).map(x ⇒ arn.copy(name = x)) append
        Shrink.shrink(arn.path).map(x ⇒ arn.copy(path = x))
    }

  implicit lazy val arbPolicyArn: Arbitrary[PolicyArn] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        name ← CoreGen.iamName
        path ← arbitrary[Path]
      } yield PolicyArn(account, name, path)
    }

  implicit lazy val shrinkPolicyArn: Shrink[PolicyArn] =
    Shrink { arn ⇒
      Shrink.shrink(arn.account).map(x ⇒ arn.copy(account = x)) append
        Shrink.shrink(arn.name).filter(_.nonEmpty).map(x ⇒ arn.copy(name = x)) append
        Shrink.shrink(arn.path).map(x ⇒ arn.copy(path = x))
    }

  implicit lazy val arbRole: Arbitrary[Role] = {
    Arbitrary {
      for {
        account ← arbitrary[Account]
        name ← CoreGen.iamName
        path ← arbitrary[Path]
        id ← IamGen.roleId
        policy ← IamGen.assumeRolePolicy
        created ← arbitrary[Date]
      } yield {
        val arn = RoleArn(account, name, path)
        Role(arn, name, path, id, policy, created)
      }
    }
  }

  implicit lazy val shrinkRole: Shrink[Role] = {
    Shrink { role ⇒
      val RoleArn(account, name, path) = role.arn
      val shrunkByName =
        Shrink.shrink(name)
          .filter(_.nonEmpty)
          .map(name ⇒ role.copy(arn = RoleArn(account, name, path), name = name))
      val shrunkByPath =
        Shrink.shrink(path)
          .map(path ⇒ role.copy(arn = RoleArn(account, name, path), path = path))
      val shrunkByPolicy =
        Shrink.shrink(role.assumeRolePolicyDocument)
          .filterNot(p ⇒ p == role.assumeRolePolicyDocument)
          .map(policy ⇒ role.copy(assumeRolePolicyDocument = policy))
      shrunkByName append shrunkByPath append shrunkByPolicy
    }
  }

  implicit lazy val arbUser: Arbitrary[User] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        name ← CoreGen.iamName
        path ← arbitrary[Path]
        id ← arbitrary[UserId]
        created ← arbitrary[Date]
        passwordLastUsed ← arbitrary[Option[Date]]
      } yield {
        val arn = UserArn(account, name, path)
        User(path.pathString, name, id.value, arn.arnString, created, passwordLastUsed)
      }
    }

  implicit lazy val shrinkUser: Shrink[User] =
    Shrink { user ⇒
      val UserArn(account, name, path) = UserArn.fromArnString(user.arn)
      val shrunkByName =
        Shrink.shrink(name)
          .filter(_.nonEmpty)
          .map(name ⇒ user.copy(arn = UserArn(account, name, path).arnString, name = name))
      val shrunkByPath =
        Shrink.shrink(path)
          .map(path ⇒ user.copy(arn = UserArn(account, name, path).arnString, path = path.pathString))
      val shrunkByPasswordLastUsed =
        Shrink.shrink(user.passwordLastUsed)
          .map(d ⇒ user.copy(passwordLastUsed = d))

      shrunkByName append shrunkByPath append shrunkByPasswordLastUsed
    }

  implicit lazy val arbUserId: Arbitrary[UserId] = Arbitrary(IamGen.userId.map(UserId.apply))

  implicit lazy val arbListAttachedRolePoliciesRequest: Arbitrary[ListAttachedRolePoliciesRequest] =
    Arbitrary {
      val noPathPrefixRequest = CoreGen.iamName.map(ListAttachedRolePoliciesRequest(_: String))
      val pathPrefixRequest =
        for {
          roleName ← CoreGen.iamName
          prefix ← arbitrary[Path]
        } yield ListAttachedRolePoliciesRequest(roleName, prefix.pathString)
      Gen.oneOf(noPathPrefixRequest, pathPrefixRequest)
    }

  implicit lazy val shrinkListAttachedRolePoliciesRequest: Shrink[ListAttachedRolePoliciesRequest] =
    Shrink { request ⇒
      Shrink.shrink(request.roleName).filter(_.nonEmpty).map(x ⇒ request.copy(roleName = x)) append
        Shrink.shrink(request.pathPrefix).map(x ⇒ request.copy(pathPrefix = x))
    }

  implicit lazy val arbGetUserRequest: Arbitrary[GetUserRequest] =
    Arbitrary {
      Gen.option(CoreGen.iamName).map {
        case Some(n) ⇒ GetUserRequest.forUserName(n)
        case None    ⇒ GetUserRequest.currentUser
      }
    }

  implicit lazy val shrinkGetUserRequest: Shrink[GetUserRequest] =
    Shrink { request ⇒
      Shrink.shrink(request.userName).filter(_.forall(_.nonEmpty)).map(x ⇒ request.copy(userName = x))
    }

  implicit lazy val arbAttachedPolicy: Arbitrary[AttachedPolicy] =
    Arbitrary {
      for {
        policyArn ← arbitrary[PolicyArn]
      } yield AttachedPolicy(policyArn, policyArn.name)
    }

  implicit lazy val shrinkAttachedPolicy: Shrink[AttachedPolicy] =
    Shrink(ap ⇒ Shrink.shrink(ap.arn).map(x ⇒ AttachedPolicy(x, x.name)))
}
