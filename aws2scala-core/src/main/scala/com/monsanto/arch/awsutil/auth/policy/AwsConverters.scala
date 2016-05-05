package com.monsanto.arch.awsutil.auth.policy

import java.time.Instant
import java.util
import java.util.{Base64, Date}

import akka.util.ByteString
import com.amazonaws.auth.policy.conditions._
import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.{Account, AccountArn, Arn}

import scala.collection.JavaConverters._
import scala.util.Try

object AwsConverters {
  implicit class AwsPrincipal(val principal: aws.Principal) extends AnyVal {
    def asScala: Principal =
      (principal.getProvider, principal.getId) match {
        case ("*", "*") ⇒
          Principal.all
        case ("AWS", "*") ⇒
          Principal.allUsers
        case ("Service", "*") ⇒
          Principal.allServices
        case ("Service", Principal.Service.ById(id)) ⇒
          Principal.service(id)
        case ("Federated", "*") ⇒
          Principal.allWebProviders
        case ("Federated", Principal.WebIdentityProvider.ById(webIdentityProvider)) ⇒
          Principal.webProvider(webIdentityProvider)
        case ("Federated", SamlProviderArn(account, name)) ⇒
          Principal.SamlProviderPrincipal(account, name)
        case ("AWS", AccountArn.FromString(AccountArn(account))) ⇒
          Principal.AccountPrincipal(account)
        case ("AWS", AccountFromNumber(account)) ⇒
          Principal.AccountPrincipal(account)
        case ("AWS", UserArn(account, name, path)) ⇒
          Principal.IamUserPrincipal(account, name, path)
        case ("AWS", RoleArn(account, name, path)) ⇒
          Principal.IamRolePrincipal(account, name, path)
        case ("AWS", AssumedRoleArn(account, roleName, sessionName)) ⇒
          Principal.IamAssumedRolePrincipal(account, roleName, sessionName)
      }
  }

  private object AccountFromNumber {
    def unapply(str: String): Option[Account] = {
      if (str.matches("^\\d{12}$")) {
        Some(Account(str))
      } else {
        None
      }
    }
  }

  private object SamlProviderArn {
    def unapply(str: String): Option[(Account, String)] =
      str match {
        case Arn(_, Arn.Namespace.IAM, None, Some(account), SamlProviderName(name)) ⇒
          Some((account, name))
        case _ ⇒
          None
      }
    
    private val SamlProviderName = "^saml-provider/(.+)$".r
  }

  private object UserArn {
    def unapply(str: String): Option[(Account, String, Option[String])] = {
      str match {
        case Arn(_, Arn.Namespace.IAM, None, Some(account), UserPathAndName("/", name)) ⇒
          Some((account, name, None))
        case Arn(_, Arn.Namespace.IAM, None, Some(account), UserPathAndName(path, name)) ⇒
          Some((account, name, Some(path)))
        case _ ⇒
          None
      }
    }

    private val UserPathAndName = "^user(/|/.*/)([^/]+)$".r
  }

  private object RoleArn {
    def unapply(str: String): Option[(Account, String, Option[String])] = {
      str match {
        case Arn(_, Arn.Namespace.IAM, None, Some(account), RolePathAndName("/", name)) ⇒
          Some((account, name, None))
        case Arn(_, Arn.Namespace.IAM, None, Some(account), RolePathAndName(path, name)) ⇒
          Some((account, name, Some(path)))
        case _ ⇒
          None
      }
    }

    private val RolePathAndName = "^role(/|/.*/)([^/]+)$".r
  }

  private object AssumedRoleArn {
    def unapply(str: String): Option[(Account, String, String)] = {
      str match {
        case Arn(_, Arn.Namespace.IAM, None, Some(account), RoleAndSessionNames(roleName, sessionName)) ⇒
          Some((account, roleName, sessionName))
        case _ ⇒
          None
      }
    }

    private val RoleAndSessionNames = "^assumed-role/([^/]+)/([^/]+)$".r
  }

  implicit class ScalaPrincipal(val principal: Principal) extends AnyVal {
    def asAws: aws.Principal = {
      principal match {
        case Principal.AllPrincipals ⇒
          aws.Principal.All
        case Principal.AllUsers ⇒
          aws.Principal.AllUsers
        case Principal.ServicePrincipal(Principal.Service.AllServices) ⇒
          aws.Principal.AllServices
        case Principal.WebProviderPrincipal(Principal.WebIdentityProvider.AllProviders) ⇒
          aws.Principal.AllWebProviders
        case _ ⇒
          new aws.Principal(principal.provider, principal.id, false)
      }
    }
  }

  implicit class AwsAction(val action: aws.Action) extends AnyVal {
    def asScala: Action = {
      Action.toScalaConversions.get(action)
        .orElse(Action.stringToScalaConversion.get(action.getActionName))
        .getOrElse(NamedAction(action.getActionName))
    }
  }

  private[policy] case class NamedAction(actionName: String) extends Action

  implicit class ScalaAction(val action: Action) extends AnyVal {
    def asAws: aws.Action = Action.toAwsConversions(action)
  }

  implicit class AwsCondition(val condition: aws.Condition) extends AnyVal {
    def asScala: Condition = {
      val key = condition.getConditionKey
      val values = condition.getValues.asScala.toList
      val (comparisonType, ifExists) =
        if (condition.getType.endsWith("IfExists")) {
          (condition.getType.dropRight(8), true)
        } else {
          (condition.getType, false)
        }

      comparisonType match {
        case MultipleKeyValueConditionPrefix(op, innerType) ⇒
          val innerCondition =
            new aws.Condition()
              .withConditionKey(key)
              .withType(if (ifExists) s"${innerType}IfExists" else innerType)
              .withValues(condition.getValues)
          innerCondition.asScala match {
            case inner: Condition with Condition.MultipleKeyValueSupport ⇒
              Condition.MultipleKeyValueCondition(op, inner)
            case _ ⇒
              throw new IllegalArgumentException(
                s"The condition type $innerType is not supported with the " +
                  s"set operation $op.")
          }
        case ArnComparisonType(arnComparisonType) ⇒
          Condition.ArnCondition(key, arnComparisonType, values, ifExists)
        case "Binary" ⇒
          Condition.BinaryCondition(key, values.map(v ⇒ ByteString(Base64.getDecoder.decode(v))), ifExists)
        case "Bool" ⇒
          values match {
            case value :: Nil ⇒ Condition.BooleanCondition(key, value.toBoolean, ifExists)
            case _            ⇒ throw new IllegalArgumentException("A Bool condition should only have one value.")
          }
        case "Null" ⇒
          values match {
            case value :: Nil ⇒ Condition.NullCondition(key, value.toBoolean)
            case _            ⇒ throw new IllegalArgumentException("A Null condition should only have one value.")
          }
        case DateComparisonType(dateComparisonType) ⇒
          Condition.DateCondition(key, dateComparisonType, values.map(x ⇒ new Date(Instant.parse(x).toEpochMilli)), ifExists)
        case IpAddressComparisonType(ipAddressComparisonType) ⇒
          Condition.IpAddressCondition(key, ipAddressComparisonType, values, ifExists)
        case NumericComparisonType(numericComparisonType) ⇒
          Condition.NumericCondition(key, numericComparisonType, values.map(_.toDouble), ifExists)
        case StringComparisonType(stringComparisonType) ⇒
          Condition.StringCondition(key, stringComparisonType, values, ifExists)
      }
    }
  }

  private object ArnComparisonType {
    def unapply(str: String): Option[Condition.ArnComparisonType] =
      Try(ArnCondition.ArnComparisonType.valueOf(str).asScala).toOption
  }

  private object DateComparisonType {
    def unapply(str: String): Option[Condition.DateComparisonType] =
      Try(DateCondition.DateComparisonType.valueOf(str).asScala).toOption
  }

  private object IpAddressComparisonType {
    def unapply(str: String): Option[Condition.IpAddressComparisonType] =
      Try(IpAddressCondition.IpAddressComparisonType.valueOf(str).asScala).toOption
  }

  private object NumericComparisonType {
    def unapply(str: String): Option[Condition.NumericComparisonType] =
      Try(NumericCondition.NumericComparisonType.valueOf(str).asScala).toOption
  }

  private object StringComparisonType {
    def unapply(str: String): Option[Condition.StringComparisonType] =
      Try(StringCondition.StringComparisonType.valueOf(str).asScala).toOption
  }

  private object MultipleKeyValueConditionPrefix {
    def unapply(str: String): Option[(Condition.SetOperation, String)] = {
      if (str.startsWith("ForAnyValue:")) {
        Some((Condition.SetOperation.ForAnyValue, str.substring(12)))
      } else if (str.startsWith("ForAllValues:")) {
        Some((Condition.SetOperation.ForAllValues, str.substring(13)))
      } else {
        None
      }
    }
  }

  implicit class ScalaCondition(val condition: Condition) extends AnyVal {
    def asAws: aws.Condition = {
      def awsCondition(conditionKey: String, comparisonType: String, comparisonValues: Seq[String], ifExists: Boolean) =
        new aws.Condition()
          .withConditionKey(conditionKey)
          .withType(if (ifExists) s"${comparisonType}IfExists" else comparisonType)
          .withValues(comparisonValues: _*)
      condition match {
        case Condition.ArnCondition(key, comparisonType, values, ifExists) ⇒
          awsCondition(key, comparisonType.asAws.toString, values, ifExists)
        case Condition.BinaryCondition(key, values, ifExists) ⇒
          awsCondition(key, "Binary", values.map(v ⇒ Base64.getEncoder.encodeToString(v.toArray)), ifExists)
        case Condition.BooleanCondition(key, value, ifExists) ⇒
          awsCondition(key, "Bool", Seq(value.toString), ifExists)
        case Condition.DateCondition(key, comparisonType, values, ifExists) ⇒
          awsCondition(key, comparisonType.asAws.toString, values.map(_.toInstant.toString), ifExists)
        case Condition.IpAddressCondition(key, comparisonType, cidrBlocks, ifExists) ⇒
          awsCondition(key, comparisonType.asAws.toString, cidrBlocks, ifExists)
        case Condition.NullCondition(key, value) ⇒
          awsCondition(key, "Null", Seq(value.toString), ifExists = false)
        case Condition.NumericCondition(key, comparisonType, values, ifExists) ⇒
          awsCondition(key, comparisonType.asAws.toString, values.map(_.toString), ifExists)
        case Condition.StringCondition(key, comparisonType, values, ifExists) ⇒
          awsCondition(key, comparisonType.asAws.toString, values, ifExists)
        case Condition.MultipleKeyValueCondition(op, inner) ⇒
          val innerAws = inner.asAws
          innerAws.withType(s"$op:${innerAws.getType}")
      }
    }
  }

  implicit class ScalaArnConditionComparisonType(val comparisonType: Condition.ArnComparisonType) extends AnyVal {
    def asAws: ArnCondition.ArnComparisonType =
      comparisonType match {
        case Condition.ArnComparisonType.Equals ⇒ ArnCondition.ArnComparisonType.ArnEquals
        case Condition.ArnComparisonType.NotEquals ⇒ ArnCondition.ArnComparisonType.ArnNotEquals
        case Condition.ArnComparisonType.Like ⇒ ArnCondition.ArnComparisonType.ArnLike
        case Condition.ArnComparisonType.NotLike ⇒ ArnCondition.ArnComparisonType.ArnNotLike
      }
  }

  implicit class AwsArnConditionComparisonType(val comparisonType: ArnCondition.ArnComparisonType) extends AnyVal {
    def asScala: Condition.ArnComparisonType =
      comparisonType match {
        case ArnCondition.ArnComparisonType.ArnEquals ⇒ Condition.ArnComparisonType.Equals
        case ArnCondition.ArnComparisonType.ArnNotEquals ⇒ Condition.ArnComparisonType.NotEquals
        case ArnCondition.ArnComparisonType.ArnLike ⇒ Condition.ArnComparisonType.Like
        case ArnCondition.ArnComparisonType.ArnNotLike ⇒ Condition.ArnComparisonType.NotLike
      }
  }

  implicit class ScalaDateConditionComparisonType(val comparisonType: Condition.DateComparisonType) extends AnyVal {
    def asAws: DateCondition.DateComparisonType =
      comparisonType match {
        case Condition.DateComparisonType.Equals ⇒ DateCondition.DateComparisonType.DateEquals
        case Condition.DateComparisonType.NotEquals ⇒ DateCondition.DateComparisonType.DateNotEquals
        case Condition.DateComparisonType.Before ⇒ DateCondition.DateComparisonType.DateLessThan
        case Condition.DateComparisonType.AtOrBefore ⇒ DateCondition.DateComparisonType.DateLessThanEquals
        case Condition.DateComparisonType.After ⇒ DateCondition.DateComparisonType.DateGreaterThan
        case Condition.DateComparisonType.AtOrAfter ⇒ DateCondition.DateComparisonType.DateGreaterThanEquals
      }
  }

  implicit class AwsDateConditionComparisonType(val comparisonType: DateCondition.DateComparisonType) extends AnyVal {
    def asScala: Condition.DateComparisonType =
      comparisonType match {
        case DateCondition.DateComparisonType.DateEquals ⇒ Condition.DateComparisonType.Equals
        case DateCondition.DateComparisonType.DateNotEquals ⇒ Condition.DateComparisonType.NotEquals
        case DateCondition.DateComparisonType.DateLessThan ⇒ Condition.DateComparisonType.Before
        case DateCondition.DateComparisonType.DateLessThanEquals ⇒ Condition.DateComparisonType.AtOrBefore
        case DateCondition.DateComparisonType.DateGreaterThan ⇒ Condition.DateComparisonType.After
        case DateCondition.DateComparisonType.DateGreaterThanEquals ⇒ Condition.DateComparisonType.AtOrAfter
      }
  }

  implicit class ScalaIpAddressConditionComparisonType(val comparisonType: Condition.IpAddressComparisonType) extends AnyVal {
    def asAws: IpAddressCondition.IpAddressComparisonType =
      comparisonType match {
        case Condition.IpAddressComparisonType.IsIn ⇒ IpAddressCondition.IpAddressComparisonType.IpAddress
        case Condition.IpAddressComparisonType.IsNotIn ⇒ IpAddressCondition.IpAddressComparisonType.NotIpAddress
      }
  }

  implicit class AwsIpAddressConditionComparisonType(val comparisonType: IpAddressCondition.IpAddressComparisonType) extends AnyVal {
    def asScala: Condition.IpAddressComparisonType =
      comparisonType match {
        case IpAddressCondition.IpAddressComparisonType.IpAddress⇒ Condition.IpAddressComparisonType.IsIn
        case IpAddressCondition.IpAddressComparisonType.NotIpAddress ⇒ Condition.IpAddressComparisonType.IsNotIn
      }
  }

  implicit class ScalaNumericConditionComparisonType(val comparisonType: Condition.NumericComparisonType) extends AnyVal {
    def asAws: NumericCondition.NumericComparisonType =
      comparisonType match {
        case Condition.NumericComparisonType.Equals ⇒ NumericCondition.NumericComparisonType.NumericEquals
        case Condition.NumericComparisonType.GreaterThan ⇒ NumericCondition.NumericComparisonType.NumericGreaterThan
        case Condition.NumericComparisonType.GreaterThanEquals ⇒ NumericCondition.NumericComparisonType.NumericGreaterThanEquals
        case Condition.NumericComparisonType.LessThan ⇒ NumericCondition.NumericComparisonType.NumericLessThan
        case Condition.NumericComparisonType.LessThanEquals ⇒ NumericCondition.NumericComparisonType.NumericLessThanEquals
        case Condition.NumericComparisonType.NotEquals ⇒ NumericCondition.NumericComparisonType.NumericNotEquals
      }
  }

  implicit class AwsNumericConditionComparisonType(val comparisonType: NumericCondition.NumericComparisonType) extends AnyVal {
    def asScala: Condition.NumericComparisonType =
      comparisonType match {
        case NumericCondition.NumericComparisonType.NumericEquals ⇒ Condition.NumericComparisonType.Equals
        case NumericCondition.NumericComparisonType.NumericGreaterThan ⇒ Condition.NumericComparisonType.GreaterThan
        case NumericCondition.NumericComparisonType.NumericGreaterThanEquals ⇒ Condition.NumericComparisonType.GreaterThanEquals
        case NumericCondition.NumericComparisonType.NumericLessThan ⇒ Condition.NumericComparisonType.LessThan
        case NumericCondition.NumericComparisonType.NumericLessThanEquals ⇒ Condition.NumericComparisonType.LessThanEquals
        case NumericCondition.NumericComparisonType.NumericNotEquals ⇒ Condition.NumericComparisonType.NotEquals
      }
  }

  implicit class ScalaStringConditionComparisonType(val comparisonType: Condition.StringComparisonType) extends AnyVal {
    def asAws: StringCondition.StringComparisonType =
      comparisonType match {
        case Condition.StringComparisonType.Equals ⇒ StringCondition.StringComparisonType.StringEquals
        case Condition.StringComparisonType.NotEquals ⇒ StringCondition.StringComparisonType.StringNotEquals
        case Condition.StringComparisonType.EqualsIgnoreCase ⇒ StringCondition.StringComparisonType.StringEqualsIgnoreCase
        case Condition.StringComparisonType.NotEqualsIgnoreCase ⇒ StringCondition.StringComparisonType.StringNotEqualsIgnoreCase
        case Condition.StringComparisonType.Like ⇒ StringCondition.StringComparisonType.StringLike
        case Condition.StringComparisonType.NotLike ⇒ StringCondition.StringComparisonType.StringNotLike
      }
  }

  implicit class AwsStringConditionComparisonType(val comparisonType: StringCondition.StringComparisonType) extends AnyVal {
    def asScala: Condition.StringComparisonType =
      comparisonType match {
        case StringCondition.StringComparisonType.StringEquals ⇒ Condition.StringComparisonType.Equals
        case StringCondition.StringComparisonType.StringNotEquals ⇒ Condition.StringComparisonType.NotEquals
        case StringCondition.StringComparisonType.StringEqualsIgnoreCase ⇒ Condition.StringComparisonType.EqualsIgnoreCase
        case StringCondition.StringComparisonType.StringNotEqualsIgnoreCase ⇒ Condition.StringComparisonType.NotEqualsIgnoreCase
        case StringCondition.StringComparisonType.StringLike ⇒ Condition.StringComparisonType.Like
        case StringCondition.StringComparisonType.StringNotLike ⇒ Condition.StringComparisonType.NotLike
      }
  }

  implicit class AwsResource(val resource: aws.Resource) extends AnyVal {
    def asScala: Resource = Resource(resource.getId)
  }

  implicit class ScalaResource(val resource: Resource) extends AnyVal {
    def asAws: aws.Resource = new aws.Resource(resource.id)
  }

  implicit class AwsStatementEffect(val effect: aws.Statement.Effect) extends AnyVal {
    def asScala: Statement.Effect =
      effect match {
        case aws.Statement.Effect.Allow ⇒ Statement.Effect.Allow
        case aws.Statement.Effect.Deny ⇒ Statement.Effect.Deny
      }
  }

  implicit class ScalaStatementEffect(val effect: Statement.Effect) extends AnyVal {
    def asAws: aws.Statement.Effect =
      effect match {
        case Statement.Effect.Allow ⇒ aws.Statement.Effect.Allow
        case Statement.Effect.Deny ⇒ aws.Statement.Effect.Deny
      }
  }

  implicit class AwsStatement(val statement: aws.Statement) extends AnyVal {
    def asScala: Statement =
      Statement(
        Option(statement.getId),
        asList(statement.getPrincipals).map(_.asScala),
        statement.getEffect.asScala,
        asList(statement.getActions).map(_.asScala),
        asList(statement.getResources).map(_.asScala),
        asList(statement.getConditions).map(_.asScala))
  }

  implicit class ScalaStatement(val statement: Statement) extends AnyVal {
    def asAws: aws.Statement = {
      val awsStatement = new aws.Statement(statement.effect.asAws)
      statement.id.foreach(id ⇒ awsStatement.setId(id))
      awsStatement.setPrincipals(statement.principals.map(_.asAws).asJavaCollection)
      awsStatement.setActions(statement.actions.map(_.asAws).asJavaCollection)
      awsStatement.setResources(statement.resources.map(_.asAws).asJavaCollection)
      awsStatement.setConditions(statement.conditions.map(_.asAws).asJava)
      awsStatement
    }
  }

  implicit class AwsPolicy(val policy: aws.Policy) extends AnyVal {
    def asScala: Policy =
      Policy(
        Option(policy.getId),
        asList(policy.getStatements).map(_.asScala))
  }

  implicit class ScalaPolicy(val policy: Policy) extends AnyVal {
    def asAws: aws.Policy = {
      val awsPolicy = new aws.Policy()
      policy.id.foreach(id ⇒ awsPolicy.setId(id))
      awsPolicy.setStatements(policy.statements.map(_.asAws).asJavaCollection)
      awsPolicy
    }
  }

  private def asList[T](collection: util.Collection[T]): List[T] =
    Option(collection).map(_.asScala.toList).getOrElse(List.empty)
}
