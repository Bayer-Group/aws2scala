package com.monsanto.arch.awsutil.testkit

import java.util.Date

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.auth.policy.action.IdentityManagementAction
import com.monsanto.arch.awsutil.identitymanagement.model._
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import org.scalacheck.Arbitrary._
import org.scalacheck.{Arbitrary, Gen, Shrink}

object IamScalaCheckImplicits {
  IdentityManagementAction.registerActions()

  implicit lazy val arbAttachRolePolicyRequest: Arbitrary[AttachRolePolicyRequest] =
    Arbitrary {
      for {
        roleName ← arbitrary[Name]
        policyArn ← arbitrary[PolicyArn]
      } yield AttachRolePolicyRequest(roleName.value, policyArn.value)
    }

  implicit lazy val shrinkAttachRolePolicyRequest: Shrink[AttachRolePolicyRequest] =
    Shrink { request ⇒
      Shrink.shrink(PolicyArn(request.policyArn)).map(x ⇒ request.copy(policyArn = x.value)) append
      Shrink.shrink(Name(request.roleName)).map(x ⇒ request.copy(roleName = x.value))
    }

  implicit lazy val arbCreateRoleRequest: Arbitrary[CreateRoleRequest] =
    Arbitrary {
      for {
        name ← arbitrary[Name]
        assumeRolePolicy ← arbitrary[Policy]
        path ← arbitrary[Option[Path]]
      } yield CreateRoleRequest(name.value, assumeRolePolicy.toString, path.map(_.value))
    }

  implicit lazy val shrinkCreateRoleRequest: Shrink[CreateRoleRequest] =
    Shrink { request ⇒
      Shrink.shrink(Name(request.name)).map(name ⇒ request.copy(name = name.value)) append
        Shrink.shrink(Policy.fromJson(request.assumeRolePolicy))
          .filterNot(p ⇒ p.toJson == request.assumeRolePolicy)
          .map(policy ⇒ request.copy(assumeRolePolicy = policy.toJson)) append
        Shrink.shrink(request.path.map(Path.apply)).map(path ⇒ request.copy(path = path.map(_.value)))
    }

  implicit lazy val arbDetachRolePolicyRequest: Arbitrary[DetachRolePolicyRequest] =
    Arbitrary {
      for {
        roleName ← arbitrary[Name]
        policyArn ← arbitrary[PolicyArn]
      } yield DetachRolePolicyRequest(roleName.value, policyArn.value)
    }

  implicit lazy val shrinkDetachRolePolicyRequest: Shrink[DetachRolePolicyRequest] =
    Shrink { request ⇒
      Shrink.shrink(Name(request.roleName)).map(x ⇒ request.copy(roleName = x.value)) append
      Shrink.shrink(PolicyArn(request.policyArn)).map(x ⇒ request.copy(policyArn = x.value))
    }

  implicit lazy val arbInstanceProfileArn: Arbitrary[InstanceProfileArn] =
    Arbitrary(Gen.resultOf(InstanceProfileArn.apply _))

  implicit lazy val shrinkInstanceProfileArn: Shrink[InstanceProfileArn] =
    Shrink.xmap((InstanceProfileArn.apply _).tupled, InstanceProfileArn.unapply(_).get)

  implicit lazy val arbName: Arbitrary[Name] =
    Arbitrary(UtilGen.stringOf(UtilGen.extendedWordChar, 1, 64).map(Name.apply))

  implicit lazy val shrinkName: Shrink[Name] =
    Shrink(name ⇒ Shrink.shrink(name.value).filter(_.nonEmpty).map(Name.apply))

  implicit lazy val arbPath: Arbitrary[Path] = {
    val elementChar: Gen[Char] = Gen.oneOf(((0x21 to 0x2e) ++ (0x30 to 0x7f)).map(_.toChar))
    val element = UtilGen.stringOf(elementChar, 1, 512)
    Arbitrary {
      Gen.sized { n ⇒
        val max = Math.sqrt(n).toInt
        for {
          n ← Gen.choose(0, max)
          elements ← Gen.listOfN(n, element)
        } yield Path(elements)
      }
    }
  }

  implicit lazy val shrinkPath: Shrink[Path] =
    Shrink { path ⇒
      Shrink.shrink(path.elements).filter(_.forall(_.nonEmpty)).map(Path.apply)
    }

  implicit lazy val arbPolicyArn: Arbitrary[PolicyArn] = Arbitrary(Gen.resultOf(PolicyArn.apply(_: Account, _: Name, _: Path)))

  implicit lazy val shrinkPolicyArn: Shrink[PolicyArn] =
    Shrink.xmap((PolicyArn.apply(_: Account, _: Name, _: Path)).tupled, PolicyArn.unapply(_).get)

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
        val arn = RoleArn(account, name, path)
        Role(arn.toString, name.value, path.value, id.value, policy.toString, created)
      }
    }
  }

  implicit lazy val shrinkRole: Shrink[Role] = {
    Shrink { role ⇒
      val RoleArn(account, name, path) = RoleArn(role.arn)
      val shrunkByName =
        Shrink.shrink(name)
          .map(name ⇒ role.copy(arn = RoleArn(account, name, path).value, name = name.value))
      val shrunkByPath =
        Shrink.shrink(path)
          .map(path ⇒ role.copy(arn = RoleArn(account, name, path).value, path = path.toString))
      val shrunkByPolicy =
        Shrink.shrink(Policy.fromJson(role.assumeRolePolicyDocument))
          .filterNot(p ⇒ p.toJson == role.assumeRolePolicyDocument)
          .map(policy ⇒ role.copy(assumeRolePolicyDocument = policy.toJson))
      shrunkByName append shrunkByPath append shrunkByPolicy
    }
  }

  implicit lazy val arbRoleArn: Arbitrary[RoleArn] = Arbitrary(Gen.resultOf(RoleArn.apply(_: Account, _: Name, _: Path)))

  implicit lazy val shrinkRoleArn: Shrink[RoleArn] =
    Shrink.xmap((RoleArn.apply(_: Account, _: Name, _: Path)).tupled, RoleArn.unapply(_).get)

  implicit lazy val arbRoleId: Arbitrary[RoleId] = Arbitrary(IamGen.roleId.map(RoleId.apply))

  implicit lazy val arbUser: Arbitrary[User] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        name ← arbitrary[Name]
        path ← arbitrary[Path]
        id ← arbitrary[UserId]
        created ← arbitrary[Date]
        passwordLastUsed ← arbitrary[Option[Date]]
      } yield {
        val arn = UserArn(account, name, path)
        User(path.value, name.value, id.value, arn.value, created, passwordLastUsed)
      }
    }

  implicit lazy val shrinkUser: Shrink[User] =
    Shrink { user ⇒
      val UserArn(account, name, path) = UserArn(user.arn)
      val shrunkByName =
        Shrink.shrink(name)
          .map(name ⇒ user.copy(arn = UserArn(account, name, path).value, name = name.value))
      val shrunkByPath =
        Shrink.shrink(path)
          .map(path ⇒ user.copy(arn = UserArn(account, name, path).value, path = path.value))
      val shrunkByPasswordLastUsed =
        Shrink.shrink(user.passwordLastUsed)
          .map(d ⇒ user.copy(passwordLastUsed = d))

      shrunkByName append shrunkByPath append shrunkByPasswordLastUsed
    }

  implicit lazy val arbUserArn: Arbitrary[UserArn] = Arbitrary(Gen.resultOf(UserArn.apply(_: Account, _: Name, _: Path)))

  implicit lazy val shrinkUserArn: Shrink[UserArn] =
    Shrink.xmap((UserArn.apply(_: Account, _: Name, _: Path)).tupled, UserArn.unapply(_).get)

  implicit lazy val arbUserId: Arbitrary[UserId] = Arbitrary(IamGen.userId.map(UserId.apply))
}
