package com.monsanto.arch.awsutil.auth.policy

import java.util.Date

import com.amazonaws.auth.policy.conditions.{ArnCondition, ConditionFactory, DateCondition}
import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

import scala.collection.JavaConverters._

private[awsutil] case class Condition(key: String, operator: String, values: Seq[String]) {
  def toAws: aws.Condition = {
    val condition = new aws.Condition
    condition.setConditionKey(key)
    condition.setType(operator)
    condition.setValues(values.asJava)
    condition
  }
}

private[awsutil] object Condition {
  def apply(key: String, operator: String, value: String): Condition = Condition(key, operator, Seq(value))

  def apply(key: String, operator: Operator, value: String): Condition = Condition(key, operator.toString, Seq(value))

  def apply(key: String, operator: Operator, values: Seq[String]): Condition = Condition(key, operator.toString, values)

  trait Operator

  object Arn {
    /** Creates a new ARN key which can be used to build a `Condition`. */
    def apply(key: String): Key = Key(key)

    private[policy] sealed abstract class ComparisonType(val toAws: ArnCondition.ArnComparisonType) extends AwsEnumeration[ArnCondition.ArnComparisonType] with Operator

    private[policy] object ComparisonType extends AwsEnumerationCompanion[ComparisonType, ArnCondition.ArnComparisonType] {
      /** Exact matching. */
      case object Equals extends ComparisonType(ArnCondition.ArnComparisonType.ArnEquals)
      /** Loose case-insensitive matching of the ARN. */
      case object Like extends ComparisonType(ArnCondition.ArnComparisonType.ArnLike)
      /** Negated form of [[Equals]]. */
      case object NotEquals extends ComparisonType(ArnCondition.ArnComparisonType.ArnNotEquals)
      /** Negated form of [[Like]]. */
      case object NotLike extends ComparisonType(ArnCondition.ArnComparisonType.ArnNotLike)

      val values: Seq[ComparisonType] = Seq(Equals, Like, NotEquals, NotLike)
    }

    case class Key private[Arn] (key: String) {
      /** Generates a condition for when the ARN is exactly equal to the given value. */
      def is(value: String): Condition = Condition(key, ComparisonType.Equals, value)

      /** Generates a condition for when the ARN is a loose case-insensitive match to the given value. */
      def isLike(value: String): Condition = Condition(key, ComparisonType.Like, value)

      /** Generates a condition for when the ARN is exactly not equal to the given value. */
      def isNot(value: String): Condition = Condition(key, ComparisonType.NotEquals, value)

      /** Generates a condition for when the ARN is not a loose case-insensitive match to the given value. */
      def isNotLike(value: String): Condition = Condition(key, ComparisonType.NotLike, value)
    }

    /** This condition indicates the source resource that is modifying another resource. */
    val SourceArn = Key(ConditionFactory.SOURCE_ARN_CONDITION_KEY)
  }

  object Boolean {
    def apply(key: String): Key = Key(key)

    case class Key private[Boolean] (key: String) {
      def isTrue: Condition = Condition(key, "Bool", "true")
      def isFalse: Condition = Condition(key, "Bool", "false")
    }
  }

  object Date {
    def apply(key: String): Key = Key(key)

    private[policy] sealed abstract class ComparisonType(val toAws: DateCondition.DateComparisonType) extends AwsEnumeration[DateCondition.DateComparisonType] with Operator
    private[policy] object ComparisonType extends AwsEnumerationCompanion[ComparisonType, DateCondition.DateComparisonType] {
      case object Equals extends ComparisonType(DateCondition.DateComparisonType.DateEquals)
      case object GreaterThan extends ComparisonType(DateCondition.DateComparisonType.DateGreaterThan)
      case object GreaterThanEquals extends ComparisonType(DateCondition.DateComparisonType.DateGreaterThanEquals)
      case object LessThan extends ComparisonType(DateCondition.DateComparisonType.DateLessThan)
      case object LessThanEquals extends ComparisonType(DateCondition.DateComparisonType.DateLessThanEquals)
      case object NotEquals extends ComparisonType(DateCondition.DateComparisonType.DateNotEquals)

      override val values: Seq[ComparisonType] = Seq(Equals, GreaterThan, GreaterThanEquals, LessThan, LessThanEquals, NotEquals)
    }

    case class Key private[Date] (key: String) {
      /** Matches a specific date. */
      def is(date: Date): Condition = Condition(key, ComparisonType.Equals, date.toInstant.toString)
      /** Negated matching. */
      def isNot(date: Date): Condition = Condition(key, ComparisonType.NotEquals, date.toInstant.toString)
      /** Matching before a specific date and time. */
      def isBefore(date: Date): Condition = Condition(key, ComparisonType.LessThan, date.toInstant.toString)
      /** Matching at or before a specific date and time. */
      def isAtOrBefore(date: Date): Condition = Condition(key, ComparisonType.LessThanEquals, date.toInstant.toString)
      /** Matching after a specific date and time. */
      def isAfter(date: Date): Condition = Condition(key, ComparisonType.GreaterThan, date.toInstant.toString)
      /** Matching at or after a specific date and time. */
      def isAtOrAfter(date: Date): Condition = Condition(key, ComparisonType.GreaterThanEquals, date.toInstant.toString)
    }

    /** To check for date/time conditions against the current time on the AWS servers. */
    val CurrentTime: Key = Key(ConditionFactory.CURRENT_TIME_CONDITION_KEY)

    /** To check for date/time conditions against a date in epoch or Unix Time. */
    val EpochTime: Key = Key(ConditionFactory.EPOCH_TIME_CONDITION_KEY)

    /** To check the date/time that temporary security credentials were issued. This key is only present in requests
      * that are signed using temporary security credentials.
      */
    val TokenIssueTime: Key = Key("aws:TokenIssueTime")
  }

  /** Constructs a new access control policy that tests if the incoming request was sent over a secure transport
    * (HTTPS).
    */
  def secureTransport: Condition = fromAws(ConditionFactory.newSecureTransportCondition())

  private[awsutil] def fromAws(condition: aws.Condition): Condition =
    Condition(condition.getConditionKey, condition.getType, condition.getValues.asScala.toList)
}
