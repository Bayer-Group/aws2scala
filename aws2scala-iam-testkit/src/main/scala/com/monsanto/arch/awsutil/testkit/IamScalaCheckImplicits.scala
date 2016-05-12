package com.monsanto.arch.awsutil.testkit

import java.util.Date

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.identitymanagement.IdentityManagement
import com.monsanto.arch.awsutil.identitymanagement.model._
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.{Account, Arn}
import org.scalacheck.Arbitrary._
import org.scalacheck.{Arbitrary, Shrink}

object IamScalaCheckImplicits {
  IdentityManagement.init()

  implicit lazy val arbAttachRolePolicyRequest: Arbitrary[AttachRolePolicyRequest] =
    Arbitrary {
      for {
        roleName ← arbitrary[Name]
        policyArn ← arbitrary[PolicyArn]
      } yield AttachRolePolicyRequest(roleName.value, policyArn.arnString)
    }

  implicit lazy val shrinkAttachRolePolicyRequest: Shrink[AttachRolePolicyRequest] =
    Shrink { request ⇒
      Shrink.shrink(PolicyArn(request.policyArn)).map(x ⇒ request.copy(policyArn = x.arnString)) append
      Shrink.shrink(Name(request.roleName)).map(x ⇒ request.copy(roleName = x.value))
    }

  implicit lazy val arbCreateRoleRequest: Arbitrary[CreateRoleRequest] =
    Arbitrary {
      for {
        name ← arbitrary[Name]
        assumeRolePolicy ← arbitrary[Policy]
        path ← arbitrary[Option[Path]]
      } yield CreateRoleRequest(name.value, assumeRolePolicy.toString, path.map(_.pathString))
    }

  implicit lazy val shrinkCreateRoleRequest: Shrink[CreateRoleRequest] =
    Shrink { request ⇒
      Shrink.shrink(Name(request.name)).map(name ⇒ request.copy(name = name.value)) append
        Shrink.shrink(Policy.fromJson(request.assumeRolePolicy))
          .filterNot(p ⇒ p.toJson == request.assumeRolePolicy)
          .map(policy ⇒ request.copy(assumeRolePolicy = policy.toJson)) append
        Shrink.shrink(request.path.map(Path.fromString.unapply(_).get)).map(path ⇒ request.copy(path = path.map(_.pathString)))
    }

  implicit lazy val arbDetachRolePolicyRequest: Arbitrary[DetachRolePolicyRequest] =
    Arbitrary {
      for {
        roleName ← arbitrary[Name]
        policyArn ← arbitrary[PolicyArn]
      } yield DetachRolePolicyRequest(roleName.value, policyArn.arnString)
    }

  implicit lazy val shrinkDetachRolePolicyRequest: Shrink[DetachRolePolicyRequest] =
    Shrink { request ⇒
      Shrink.shrink(Name(request.roleName)).map(x ⇒ request.copy(roleName = x.value)) append
      Shrink.shrink(PolicyArn(request.policyArn)).map(x ⇒ request.copy(policyArn = x.arnString))
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

  implicit lazy val arbName: Arbitrary[Name] =
    Arbitrary(UtilGen.stringOf(UtilGen.extendedWordChar, 1, 64).map(Name.apply))

  implicit lazy val shrinkName: Shrink[Name] =
    Shrink(name ⇒ Shrink.shrink(name.value).filter(_.nonEmpty).map(Name.apply))

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
        name ← arbitrary[Name]
        path ← arbitrary[Path]
        id ← arbitrary[RoleId]
        policy ← arbitrary[Policy]
        created ← arbitrary[Date]
      } yield {
        val arn = RoleArn(account, name.value, path)
        Role(arn.arnString, name.value, path.pathString, id.value, policy.toString, created)
      }
    }
  }

  implicit lazy val shrinkRole: Shrink[Role] = {
    Shrink { role ⇒
      val RoleArn(account, name, path) = RoleArn(role.arn)
      val shrunkByName =
        Shrink.shrink(Name(name))
          .map(name ⇒ role.copy(arn = RoleArn(account, name.value, path).arnString, name = name.value))
      val shrunkByPath =
        Shrink.shrink(path)
          .map(path ⇒ role.copy(arn = RoleArn(account, name, path).arnString, path = path.toString))
      val shrunkByPolicy =
        Shrink.shrink(Policy.fromJson(role.assumeRolePolicyDocument))
          .filterNot(p ⇒ p.toJson == role.assumeRolePolicyDocument)
          .map(policy ⇒ role.copy(assumeRolePolicyDocument = policy.toJson))
      shrunkByName append shrunkByPath append shrunkByPolicy
    }
  }

  implicit lazy val arbRoleId: Arbitrary[RoleId] = Arbitrary(IamGen.roleId.map(RoleId.apply))

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
      val UserArn(account, name, path) = UserArn(user.arn)
      val shrunkByName =
        Shrink.shrink(Name(name))
          .map(name ⇒ user.copy(arn = UserArn(account, name.value, path).arnString, name = name.value))
      val shrunkByPath =
        Shrink.shrink(path)
          .map(path ⇒ user.copy(arn = UserArn(account, name, path).arnString, path = path.pathString))
      val shrunkByPasswordLastUsed =
        Shrink.shrink(user.passwordLastUsed)
          .map(d ⇒ user.copy(passwordLastUsed = d))

      shrunkByName append shrunkByPath append shrunkByPasswordLastUsed
    }

  implicit lazy val arbUserId: Arbitrary[UserId] = Arbitrary(IamGen.userId.map(UserId.apply))
}
