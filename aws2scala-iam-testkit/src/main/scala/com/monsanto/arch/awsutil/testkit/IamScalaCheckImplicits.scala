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
      } yield DetachRolePolicyRequest(roleName, policyArn)
    }

  implicit lazy val shrinkDetachRolePolicyRequest: Shrink[DetachRolePolicyRequest] =
    Shrink { request ⇒
      Shrink.shrink(request.roleName).filter(_.nonEmpty).map(x ⇒ request.copy(roleName = x)) append
        Shrink.shrink(request.policyArn).map(x ⇒ request.copy(policyArn = x))
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
        arn ← arbitrary[UserArn]
        id ← IamGen.userId
        created ← arbitrary[Date]
        passwordLastUsed ← arbitrary[Option[Date]]
      } yield User(arn.path, arn.name, id, arn, created, passwordLastUsed)
    }

  implicit lazy val shrinkUser: Shrink[User] =
    Shrink { user ⇒
      val shrunkByName =
        Shrink.shrink(user.name)
          .filter(_.nonEmpty)
          .map(n ⇒ user.copy(arn = user.arn.copy(name = n), name = n))
      val shrunkByPath =
        Shrink.shrink(user.path)
          .map(p ⇒ user.copy(arn = user.arn.copy(path = p), path = p))
      val shrunkByPasswordLastUsed =
        Shrink.shrink(user.passwordLastUsed)
          .map(d ⇒ user.copy(passwordLastUsed = d))

      shrunkByName append shrunkByPath append shrunkByPasswordLastUsed
    }

  implicit lazy val arbListAttachedRolePoliciesRequest: Arbitrary[ListAttachedRolePoliciesRequest] =
    Arbitrary {
      val noPathPrefixRequest = CoreGen.iamName.map(ListAttachedRolePoliciesRequest(_: String))
      val pathPrefixRequest =
        for {
          roleName ← CoreGen.iamName
          prefix ← arbitrary[Path]
        } yield ListAttachedRolePoliciesRequest(roleName, prefix)
      Gen.oneOf(noPathPrefixRequest, pathPrefixRequest)
    }

  implicit lazy val shrinkListAttachedRolePoliciesRequest: Shrink[ListAttachedRolePoliciesRequest] =
    Shrink { request ⇒
      Shrink.shrink(request.roleName).filter(_.nonEmpty).map(x ⇒ request.copy(roleName = x)) append
        Shrink.shrink(request.prefix).map(p ⇒ request.copy(prefix = p))
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

  implicit lazy val arbCreatePolicyRequest: Arbitrary[CreatePolicyRequest] =
    Arbitrary {
      for {
        name ← CoreGen.iamName
        policy ← arbitrary[Policy]
        path ← arbitrary[Path]
        description ← arbitrary[Option[String]].suchThat(_.forall(_.length < 1000))
      } yield CreatePolicyRequest(name, policy, description, path)
    }

  implicit lazy val shrinkCreatePolicyRequest: Shrink[CreatePolicyRequest] =
    Shrink { request ⇒
      Shrink.shrink(request.name).filter(_.nonEmpty).map(n ⇒ request.copy(name = n)) append
        Shrink.shrink(request.document).map(d ⇒ request.copy(document = d)) append
        Shrink.shrink(request.path).map(p ⇒ request.copy(path = p)) append
        Shrink.shrink(request.description).map(d ⇒ request.copy(description = d))
    }

  implicit lazy val arbManagedPolicy: Arbitrary[ManagedPolicy] =
    Arbitrary {
      for {
        arn ← arbitrary[PolicyArn]
        id ← IamGen.policyId
        defaultVersionId ← Gen.posNum[Int].map(n ⇒ s"v$n")
        attachmentCount ← Gen.choose(0, 1024)
        attachable ← arbitrary[Boolean]
        description ← arbitrary[Option[String]]
        created ← arbitrary[Date]
        updated ← arbitrary[Date].suchThat(u ⇒ u.equals(created) || u.after(created))
      } yield ManagedPolicy(arn.name, id, arn, arn.path, defaultVersionId, attachmentCount, attachable, description,
        created, updated)
    }

  implicit lazy val shrinkManagedPolicy: Shrink[ManagedPolicy] =
    Shrink { policy ⇒
      Shrink.shrink(policy.arn).map(a ⇒ policy.copy(name = a.name, arn = a, path = a.path)) append
        Shrink.shrink(policy.attachmentCount).filter(_ >= 0).map(x ⇒ policy.copy(attachmentCount = x)) append
        Shrink.shrink(policy.description).map(x ⇒ policy.copy(description = x))
    }

  implicit lazy val arbListPoliciesRequest: Arbitrary[ListPoliciesRequest] =
    Arbitrary(Gen.resultOf(ListPoliciesRequest(_: Boolean, _: Path, _: ListPoliciesRequest.Scope)))

  implicit lazy val shrinkListPoliciesRequest: Shrink[ListPoliciesRequest] =
    Shrink { request ⇒
      Shrink.shrink(request.prefix).map(p ⇒ request.copy(prefix = p))
    }

  implicit lazy val arbListPoliciesRequestScope: Arbitrary[ListPoliciesRequest.Scope] =
    Arbitrary(Gen.oneOf(ListPoliciesRequest.Scope.values))

  implicit lazy val arbManagedPolicyVersion: Arbitrary[ManagedPolicyVersion] =
    Arbitrary {
      for {
        document ← arbitrary[Policy]
        versionId ← Gen.posNum[Int].map(n ⇒ s"v$n")
        isDefaultVersion ← arbitrary[Boolean]
        created ← arbitrary[Date]
      } yield ManagedPolicyVersion(document, versionId, isDefaultVersion, created)
    }

  implicit lazy val shrinkManagedPolicyVersion: Shrink[ManagedPolicyVersion] =
    Shrink { policyVersion ⇒
      Shrink.shrink(policyVersion.document).map(x ⇒ policyVersion.copy(document = x))
    }
}
