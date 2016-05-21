package com.monsanto.arch.awsutil.auth.policy

import java.nio.ByteBuffer
import java.util.Date

import akka.util.ByteString

/** Conditions specify when a statement in a policy is in effect.  A statement
  * may have zero or more conditions, and all conditions must evaluate to true
  * in order for the statement to take effect.
  *
  * Conditions are generally made up of three parts:
  *
  *  1. A ''condition key'' that defines which value of a request to pull in
  *     and compare against when the policy is evaluated by AWS.
  *  1. A ''comparison type'' that determines how the value obtained by the
  *     request should be compared to the ''comparison values''.
  *  1. One or more ''comparison values'' against which to perform the
  *     operation defined by the ''comparison type''.
  *
  * ==Building a condition==
  *
  * ''aws2scala'' attempts to provide a fluent interface for building
  * conditions.  It should be possible to build any valid condition using this
  * API, as outlined below:
  *
  *  1. Start by specifying a key with a particular type using
  *     [[Condition.arn arn]], [[Condition.binary binary]],
  *     [[Condition.boolean boolean]], [[Condition.date date]],
  *     [[Condition.ipAddress ipAddress]], [[Condition.numeric numeric]], or
  *     [[Condition.string string]].  Alternatively, you can use one of the
  *     existential conditions ([[Condition.isMissing isMissing]] and
  *     [[Condition.isNotNull isNotNull]]) or a key constant such as
  *     [[Condition.sourceArn]].
  *  1. If you want the condition to succeed even if the key is missing, invoke
  *     `ifExists` on the key.  Alternatively, invoke `ifExists` on the
  *     resulting condition.
  *  1. Invoke the corresponding comparison method and provide one or more
  *     comparison values.  This will return a `Condition` instance that can
  *     be used in a [[Statement]].
  *  1. Finally, if you which the condition to work in cases where you want to
  *     test against multiple key values, i.e. as a set operation, then invoke
  *     [[Condition.MultipleKeyValueSupport.forAllValues forAllValues]] or
  *     [[Condition.MultipleKeyValueSupport.forAnyValue forAnyValue]] on the condition.
  *
  * For more information about the different condition types and the
  * comparisons they support, please check the following subsections.
  *
  *
  * ===String conditions===
  *
  * String conditions restrict access baded on comparing a key to a stirng
  * value.   Available comparisons include:
  *
  *  - Case-sensitive exact matching, using the [[Condition.StringKey.is is]]
  *    operator.
  *  - Case-insensitive exact matching, using the
  *    [[Condition.StringKey.ignoringCaseIs ignoringCaseIs]] operator.
  *  - Case-sensitive matching that supports the wildcards that match
  *    multiple characters `*` and single characters `?` using the
  *    [[Condition.StringKey.isLike isLike]] operator.
  *  - Negated forms of the above: [[Condition.StringKey.isNot isNot]],
  *    [[Condition.StringKey.ignoringCaseIsNot ignoringCaseIsNot]],
  *    and [[Condition.StringKey.isNotLike isNotLike]].
  *
  * '''Examples:'''
  * {{{
  *   // the value for the key should be value, case-sensitive
  *   Condition.string("key") is "value"
  *
  *   // the value for the key should be one of the values, case-insensitive
  *   Condition.string("key").ignoringCaseIs("a", "b", "c")
  *
  *   // the value for the key must not match the given pattern
  *   Condition.string("key") isNotLike "foo*"
  * }}}
  *
  *
  * ===Numeric conditions===
  *
  * Numeric conditions restrict access based on comparing a key to a numeric
  * value.  Valid comparisons include:
  *
  *  - Equality using the [[Condition.NumericKey.is is]] operator.
  *  - Inequality using the [[Condition.NumericKey.isLessThan isLessThan]]
  *    [[Condition.NumericKey.isLessThanOrEqualTo isLessThanOrEqualTo]],
  *    [[Condition.NumericKey.isGreaterThan isGreaterThan]],
  *    [[Condition.NumericKey.isGreaterThanOrEqualTo isGreaterThanOrEqualTo]],
  *    [[Condition.NumericKey.isNot isNot]] operators.
  *
  * '''Examples:'''
  * {{{
  *   // the value of the key should be 42
  *   Condition.numeric("key") is 42
  *
  *   // the value of the key must be at least 100
  *   Condition.string("key") isGreaterThanOrEqualTo 100
  * }}}
  *
  *
  * ===Date conditions===
  *
  * Date conditions restrict access based on comparing a key to a date/time
  * value.  Valid comparisons include:
  *
  *  - Matching a specific date/time using the [[Condition.DateKey.is is]]
  *    operator.
  *  - Negated matching of a specific date/time using the
  *    [[Condition.DateKey.isNot isNot]] operator.
  *  - Matching before a specific date/time using
  *    [[Condition.DateKey.isBefore isBefore]] operator.
  *  - Matching at or before a specific date/time using
  *    [[Condition.DateKey.isAtOrBefore isAtOrBefore]] operator.
  *  - Matching after a specific date/time using
  *    [[Condition.DateKey.isAfter isAfter]] operator.
  *  - Matching at or after a specific date/time using
  *    [[Condition.DateKey.isAtOrAfter isAtOrAfter]] operator.
  *
  * For date conditions, you will most likely want to use the
  * [[Condition.currentTime currentTime]] or [[Condition.epochTime epochTime]]
  * keys.
  *
  * '''Examples:'''
  * {{{
  *   val someDate: Date = // some value
  *
  *   // the current (local AWS) time must be after the given date
  *   Condition.currentTime isAfter someDate
  *
  *   // the epoch time must be at or before the given date
  *   Condition.epochTime isAtOrBefore someDate
  * }}}
  *
  *
  * ===Boolean conditions===
  *
  * Boolean conditions restrict access based on comparing a key to a boolean
  * value.  The available comparisons are
  * [[Condition.BooleanKey.isTrue isTrue]] and
  * [[Condition.BooleanKey.isFalse isFalse]].
  *
  * '''Examples:'''
  * {{{
  *   // the key must be true
  *   Condition.boolean("key").isTrue
  *
  *   // the key must be false
  *   Condition.boolean("key").isFalse
  * }}}
  *
  *
  * ===Binary conditions===
  *
  * Binary conditions restrict access based on comparing a key to a binary
  * value.  The only valid operation is equality with
  * [[Condition.BinaryKey.is(value:a* is]].
  *
  * '''Examples:'''
  * {{{
  *   // value may be an array of bytes
  *   val arrayOfBytes: Array[Byte] = // some value
  *   Condition.binary("key") is arrayOfBytes
  *
  *   // value may be a java.nio.ByteBuffer
  *   val byteBuffer: ByteBuffer = // some value
  *   Condition.binary("key") is byteBuffer
  *
  *   // value may be an akka.util.ByteString
  *   val byteString: ByteString = // some value
  *   Condition.binary("key") is byteString
  * }}}
  *
  *
  * ===IP address conditions===
  *
  * IP address conditions restrict access based on comparing a key to an IP
  * address or range of IP addresses.  The two available comparions types are:
  *
  *  1. Testing that an IP address is the given address or within the given
  *     range using the [[Condition.IpAddressKey.isIn isIn]] operator.
  *  1. The negation of the above using the
  *     [[Condition.IpAddressKey.isNotIn isNotIn]] operator.
  *
  * You may wish to use the [[Condition.sourceIp]] constant for building IP
  * address conditions.
  *
  * '''Examples:'''
  * {{{
  *   // tests that the request comes from the IP range 203.0.113.0 to
  *   // 203.0.113.255
  *   Condition.sourceIp isIn "203.0.113.0/24"
  * }}}
  *
  *
  * ===Amazon Resource Name conditions===
  *
  * Amazon Resource Name (ARN) conditions restrict access based on comparing a
  * key to an ARN.   Available comparisons include:
  *
  *  - Exact matching, using the [[Condition.ArnKey.is is]] operator.
  *  - Case-insensitive, loose matching, using the
  *    [[Condition.ArnKey.isLike isLike]] operator.  Each of the size
  *    colon-delimited components of the ARN is checked separately and each
  *    can include a multiple-character match wildcard (`*`) or a
  *    single-character match wildcard (`?`).
  *  - Negated forms of the above: [[Condition.ArnKey.isNot isNot]],
  *    and [[Condition.ArnKey.isNotLike isNotLike]].
  *
  * You may wish to use the [[Condition.sourceArn]] constant for building ARN
  * conditions.
  *
  * '''Examples:'''
  * {{{
  *   // the value for the source ARN must be exactly the given SNS topic
  *   Condition.sourceArn is "arn:aws:sns:us-east-1:123456789012:MyTopic"
  *
  *   // the value for the source ARN may be any SQS queue
  *   Condition.sourceArn isLike "arn:aws:sqs:*:*:*"
  *
  *   // the value for the key should be one of the given roles
  *   Condition.string("role").is(
  *     "arn:aws:iam::123456789012:role/Role1",
  *     "arn:aws:iam::123456789012:role/Role2",
  *     "arn:aws:iam::123456789012:role/Role3"
  *   )
  * }}}
  *
  *
  * ===Existential conditions===
  *
  * It is also possible to check if a key exists at the time of authorisation.
  *
  *  - Use [[Condition.isMissing]] to match cases in which a given key is not
  *    present.
  *  - Use [[Condition.isNotNull]] to match cases in which a given key exists
  *    and the value is not null.
  *
  * '''Example:'''
  * {{{
  *   // The user must not be using temporary credentials
  *   Condition.isMissing("aws:TokenIssueTime")
  * }}}
  *
  *
  * ==`…IfExists` conditions==
  *
  * Except for the existential conditions, all of the above conditions
  * generally require that the key have a value that matches the condition.  In
  * some cases, you may wish to make a condition apply only when a key is
  * present with a value, i.e. the condition will succeed when either the key
  * is missing or is present and has a matching value.
  *
  * To accomplish this, there are two possible options:
  *
  *   1. When you have created the key, call `ifExists` on the key before
  *      applying the operation.
  *   1. Once you have created the condition, call `ifExists` on the condition
  *      itself.
  *
  * '''Examples:'''
  * {{{
  *   // using ifExists on the key
  *   Condition.string("key").ifExists is "value"
  *
  *   // using ifExists on the condition
  *   Condition.string("key").is("value").ifExists
  * }}}
  *
  *
  * ==Conditions that test multiple key values==
  *
  * Finally, some keys may have more than one value.  It is possible to create
  * conditions that test multiple values using a set operator.  There are two
  * such operations:
  *
  *   1. Using [[Condition.MultipleKeyValueSupport.forAnyValue forAnyValue]]
  *      will cause the condition to match if any one of the values in the
  *      request matches any of the condition values in the policy.
  *   1. Using [[Condition.MultipleKeyValueSupport.forAllValues forAllValues]]
  *      will cause the condition to match if every one of the values in the
  *      request matches at least one of the condition values in the policy.
  *
  * '''Examples:'''
  * {{{
  *   // Creates a condition such will match only if all of the requested
  *   // DynamoDB attributes are one of the given values, i.e. every requested
  *   // attribute must be one of PostDateTime, Message, or Tags
  *   //
  *   // This would be useful in the case of a policy that restricts getting
  *   // items to these attributes.
  *   Condition.string("dynamodb:requestedAttributes")
  *     .isLike("PostDateTime", "Message", "Tags")
  *     .forAllValues
  *
  *   // Creates a condition such will match if any of the requested DynamoDB
  *   // attributes are one of the given values, i.e. at least one requested
  *   // attribute must be either ID or PostDateTime
  *   //
  *   // This would be useful in the case of a policy that denies updates to
  *   // these attributes.
  *   Condition.string("dynamodb:requestedAttributes")
  *     .isLike("ID", "PostDateTime")
  *     .forAnyValue
  * }}}
  *
  * @see [[http://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements.html#Condition  IAM Policy Elements Reference: Condition]]
  * @see [[http://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_multi-value-conditions.html Creating a Condition That Tests Multiple Key Values (Set Operations)]]
  */
sealed trait Condition

object Condition {
  /** Allows creation of an ARN condition using the given key. */
  def arn(key: String): ArnKey = new ArnKey(key, ignoreMissing = false)

  /** Creates a condition that matches a binary key value. */
  def binary(key: String): BinaryKey = new BinaryKey(key, ignoreMissing = false)

  /** Allows creation of boolean conditions. */
  def boolean(key: String): BooleanKey =
    new BooleanKey(key, ignoreMissing = false)

  /** Allows creation of date conditions using the give key. */
  def date(key: String): DateKey = new DateKey(key, ignoreMissing = false)

  /** Allows creation of IP address conditions using the given key. */
  def ipAddress(key: String): IpAddressKey =
    new IpAddressKey(key, ignoreMissing = false)

  /** Allows creation of keys supporting numeric comparison conditions. */
  def numeric(key: String): NumericKey =
    new NumericKey(key, ignoreMissing = false)

  /** Allows creation of keys supporting string comparison conditions. */
  def string(key: String): StringKey =
    new StringKey(key, ignoreMissing = false)

  /** Creates a condition that will match when the given key does not exist. */
  def isMissing(key: String): NullCondition = NullCondition(key, value = true)

  /** Creates a condition that checks that the given key exists and its value is
    * not null.
    */
  def isNotNull(key: String): NullCondition = NullCondition(key, value = false)

  /** This condition indicates the source resource that is modifying another
    * resource.
    */
  lazy val sourceArn: ArnKey =
    new ArnKey("aws:SourceArn", ignoreMissing = false)

  /** Allows creation of IP address conditions using the source IP address of
    * the request.
    */
  lazy val sourceIp: IpAddressKey =
    new IpAddressKey("aws:SourceIp", ignoreMissing = false)

  /** Allows creation of date conditions using the current time. */
  lazy val currentTime: DateKey =
    new DateKey("aws:CurrentTime", ignoreMissing = false)

  /** Allows creation of date conditions using the epoch time. */
  lazy val epochTime: DateKey =
    new DateKey("aws:EpochTime", ignoreMissing = false)

  /** Provides a fluent interface for building ARN conditions. */
  class ArnKey private[Condition] (key: String, ignoreMissing: Boolean) {
    /** Generates a condition for when the ARN is exactly equal to the given
      * value.
      */
    def is(values: String*): ArnCondition =
      ArnCondition(key, ArnComparisonType.Equals, values, ignoreMissing)

    /** Generates a condition for when the ARN is a loose, case-insensitive
      * match to the given value.
      */
    def isLike(values: String*): ArnCondition =
      ArnCondition(key, ArnComparisonType.Like, values, ignoreMissing)

    /** Generates a condition for when the ARN is exactly not equal to the
      * given value.
      */
    def isNot(values: String*): ArnCondition =
      ArnCondition(key, ArnComparisonType.NotEquals, values, ignoreMissing)

    /** Generates a condition for when the ARN is not a loose case-insensitive
      * match to the given value.
      */
    def isNotLike(values: String*): ArnCondition =
      ArnCondition(key, ArnComparisonType.NotLike, values, ignoreMissing)

    /** Makes the resulting condition be ignored if the given key is missing. */
    def ifExists: ArnKey = new ArnKey(key, ignoreMissing = true)
  }

  /** Enumeration for all ARN comparison types. */
  sealed abstract class ArnComparisonType(val id: String)
  object ArnComparisonType {
    /** Exact matching. */
    case object Equals extends ArnComparisonType("ArnEquals")
    /** Loose case-insensitive matching of the ARN. */
    case object Like extends ArnComparisonType("ArnLike")
    /** Negated form of [[Equals]]. */
    case object NotEquals extends ArnComparisonType("ArnNotEquals")
    /** Negated form of [[Like]]. */
    case object NotLike extends ArnComparisonType("ArnNotLike")

    val values: Seq[ArnComparisonType] = Seq(Equals, Like, NotEquals, NotLike)

    /** Returns the `ArnComparisonType` value for the given ID. */
    def apply(id: String): ArnComparisonType =
      fromId.unapply(id)
        .getOrElse(throw new IllegalArgumentException(s"‘$id’ is not a valid ARN comparison type ID."))

    /** Extractor for getting the ARN comparison type from its string identifier. */
    object fromId {
      def unapply(id: String): Option[ArnComparisonType] = values.find(_.id == id)
    }
  }

  /** Condition for comparing the value of a key against an ARN value.
    *
    * @param key the name of the value to match from the request
    * @param comparisonType the type of comparison to perform
    * @param values the ARN values against which to compare
    * @param ignoreMissing if true, if the key is missing from the request, the
    *                      condition will succeed.  Otherwise, a missing key
    *                      from the request will result in a failure.
    */
  case class ArnCondition(key: String,
                          comparisonType: ArnComparisonType,
                          values: Seq[String],
                          ignoreMissing: Boolean)
      extends Condition with MultipleKeyValueSupport {
    /** Creates a copy of this condition that will ignore a missing key in a
      * request.
      */
    def ifExists: ArnCondition = copy(ignoreMissing = true)
  }

  /** Provides a fluent interface for building binary conditions. */
  class BinaryKey private[Condition] (key: String, ignoreMissing: Boolean) {
    /** Creates a condition to match the given binary value. */
    def is(value: ByteString): BinaryCondition =
      BinaryCondition(key, Seq(value), ignoreMissing)

    /** Creates a condition to match the given binary value. */
    def is(value: Array[Byte]): BinaryCondition =
      BinaryCondition(key, Seq(ByteString(value)), ignoreMissing)

    /** Creates a condition to match the given binary value. */
    def is(value: ByteBuffer): BinaryCondition =
      BinaryCondition(key, Seq(ByteString(value.duplicate())), ignoreMissing)

    /** Makes the resulting condition be ignored if the given key is missing. */
    def ifExists: BinaryKey = new BinaryKey(key, ignoreMissing = true)
  }

  /** Condition for comparing the value of a key against a binary value.
    *
    * @param key the name of the value to match from the request
    * @param values the binary values against which to compare
    * @param ignoreMissing if true, if the key is missing from the request, the
    *                      condition will succeed.  Otherwise, a missing key
    *                      from the request will result in a failure.
    */
  case class BinaryCondition(key: String,
                             values: Seq[ByteString],
                             ignoreMissing: Boolean)
      extends Condition with MultipleKeyValueSupport{
    /** Creates a copy of this condition that will ignore a missing key in a
      * request.
      */
    def ifExists: BinaryCondition = copy(ignoreMissing = true)
  }

  /** Provides a fluent interface for building boolean conditions. */
  case class BooleanKey private[Condition] (key: String, ignoreMissing: Boolean) {
    /** Creates a condition that checks if the given key is true. */
    def isTrue: BooleanCondition = BooleanCondition(key, value = true, ignoreMissing)

    /** Creates a condition that checks if the given key is false. */
    def isFalse: BooleanCondition = BooleanCondition(key, value = false, ignoreMissing)

    /** Makes the resulting condition be ignored if the given key is missing. */
    def ifExists: BooleanKey = copy(ignoreMissing = true)
  }

  /** Condition for comparing the value of a key against a boolean value.
    *
    * @param key the name of the value to match from the request
    * @param value the desired boolean value
    * @param ignoreMissing if true, if the key is missing from the request, the
    *                      condition will succeed.  Otherwise, a missing key
    *                      from the request will result in a failure.
    */
  case class BooleanCondition(key: String,
                              value: Boolean,
                              ignoreMissing: Boolean)
      extends Condition with MultipleKeyValueSupport {
    /** Creates a copy of this condition that will ignore a missing key in a
      * request.
      */
    def ifExists: BooleanCondition = copy(ignoreMissing = true)
  }

  /** Enumeration of all of the date comparison types. */
  sealed abstract class DateComparisonType(val id: String)
  object DateComparisonType {
    case object Equals extends DateComparisonType("DateEquals")
    case object After extends DateComparisonType("DateGreaterThan")
    case object AtOrAfter extends DateComparisonType("DateGreaterThanEquals")
    case object Before extends DateComparisonType("DateLessThan")
    case object AtOrBefore extends DateComparisonType("DateLessThanEquals")
    case object NotEquals extends DateComparisonType("DateNotEquals")

    val values: Seq[DateComparisonType] = Seq(Equals, After, AtOrAfter, Before, AtOrBefore, NotEquals)

    /** Returns the `DateComparisonType` value for the given ID. */
    def apply(id: String): DateComparisonType =
      fromId.unapply(id)
        .getOrElse(throw new IllegalArgumentException(s"‘$id’ is not a valid date comparison type ID."))

    /** Extractor for getting the date comparison type from its string identifier. */
    object fromId {
      def unapply(id: String): Option[DateComparisonType] = values.find(_.id == id)
    }
  }

  /** Condition for comparing the value of a key against a date/time value.
    *
    * @param key the name of the value to match from the request
    * @param comparisonType the type of comparison to perform
    * @param values the date/time values against which to compare
    * @param ignoreMissing if true, if the key is missing from the request, the
    *                      condition will succeed.  Otherwise, a missing key
    *                      from the request will result in a failure.
    */
  case class DateCondition(key: String,
                           comparisonType: DateComparisonType,
                           values: Seq[Date],
                           ignoreMissing: Boolean)
      extends Condition with MultipleKeyValueSupport {
    /** Creates a copy of this condition that will ignore a missing key in a
      * request.
      */
    def ifExists: DateCondition = copy(ignoreMissing = true)
  }

  /** Provides a fluent interface for building date/time conditions. */
  class DateKey private[Condition] (key: String, ignoreMissing: Boolean) {
    /** Matches a specific date. */
    def is(dates: Date*): DateCondition =
      DateCondition(key, DateComparisonType.Equals, dates, ignoreMissing)

    /** Negated matching. */
    def isNot(dates: Date*): DateCondition =
      DateCondition(key, DateComparisonType.NotEquals, dates, ignoreMissing)

    /** Matching before a specific date and time. */
    def isBefore(dates: Date*): DateCondition =
      DateCondition(key, DateComparisonType.Before, dates, ignoreMissing)

    /** Matching at or before a specific date and time. */
    def isAtOrBefore(dates: Date*): DateCondition =
      DateCondition(key, DateComparisonType.AtOrBefore, dates, ignoreMissing)

    /** Matching after a specific date and time. */
    def isAfter(dates: Date*): DateCondition =
      DateCondition(key, DateComparisonType.After, dates, ignoreMissing)

    /** Matching at or after a specific date and time. */
    def isAtOrAfter(dates: Date*): DateCondition =
      DateCondition(key, DateComparisonType.AtOrAfter, dates, ignoreMissing)

    /** Makes the resulting condition be ignored if the given key is missing. */
    def ifExists: DateKey = new DateKey(key, ignoreMissing = true)
  }

  /** Enumeration of all of the IP address comparison types. */
  sealed abstract class IpAddressComparisonType(val id: String)
  object IpAddressComparisonType {
    /** Matches an IP address against a CIDR IP range, evaluating to true if the IP address being tested is in the
      * conditions‘s specified CIDR IP range.
      */
    case object IsIn extends IpAddressComparisonType("IpAddress")
    /** Negated form of [[IsIn]]. */
    case object IsNotIn extends IpAddressComparisonType("NotIpAddress")

    val values: Seq[IpAddressComparisonType] = Seq(IsIn, IsNotIn)

    /** Returns the `IpAddressComparisonType` value for the given ID. */
    def apply(id: String): IpAddressComparisonType =
      fromId.unapply(id)
        .getOrElse(throw new IllegalArgumentException(s"‘$id’ is not a valid IP address comparison type ID."))

    /** Extractor for getting the IP address comparison type from its string identifier. */
    object fromId {
      def unapply(id: String): Option[IpAddressComparisonType] = values.find(_.id == id)
    }
  }

  /** Provides a fluent interface for building date/time conditions. */
  case class IpAddressKey private[Condition] (key: String, ignoreMissing: Boolean) {
    /** Creates a condition that will match if the tested address is within one of the given ranges. */
    def isIn(cidrBlocks: String*): IpAddressCondition = IpAddressCondition(key, IpAddressComparisonType.IsIn, cidrBlocks, ignoreMissing)

    /** Creates a condition that will match if the tested address is not within one of the given ranges. */
    def isNotIn(cidrBlocks: String*): IpAddressCondition = IpAddressCondition(key, IpAddressComparisonType.IsNotIn, cidrBlocks, ignoreMissing)

    /** Makes the resulting condition be ignored if the given key is missing. */
    def ifExists: IpAddressKey = copy(ignoreMissing = true)
  }

  /** Condition for comparing the value of a key against a CIDR block.
    *
    * @param key the name of the value to match from the request
    * @param cidrBlocks the CIDR blocks against which to compare
    * @param ignoreMissing if true, if the key is missing from the request, the
    *                      condition will succeed.  Otherwise, a missing key
    *                      from the request will result in a failure.
    */
  case class IpAddressCondition(key: String,
                                comparisonType: IpAddressComparisonType,
                                cidrBlocks: Seq[String],
                                ignoreMissing: Boolean)
      extends Condition with MultipleKeyValueSupport {
    /** Creates a copy of this condition that will ignore a missing key in a request. */
    def ifExists: IpAddressCondition = copy(ignoreMissing = true)
  }

  /** The enumeration of all valid numeric comparison types. */
  sealed trait NumericComparisonType
  object NumericComparisonType {
    case object Equals extends NumericComparisonType
    case object GreaterThan extends NumericComparisonType
    case object GreaterThanEquals extends NumericComparisonType
    case object LessThan extends NumericComparisonType
    case object LessThanEquals extends NumericComparisonType
    case object NotEquals extends NumericComparisonType

    val values: Seq[NumericComparisonType] =
      Seq(Equals, GreaterThan, GreaterThanEquals, LessThan, LessThanEquals,
        NotEquals)
  }

  /** Condition for comparing the value of a key against a numeric value.
    *
    * @param key the name of the value to match from the request
    * @param comparisonType the type of comparison to perform
    * @param values the numeric values against which to compare
    * @param ignoreMissing if true, if the key is missing from the request, the
    *                      condition will succeed.  Otherwise, a missing key
    *                      from the request will result in a failure.
    */
  case class NumericCondition(key: String,
                              comparisonType: NumericComparisonType,
                              values: Seq[Double],
                              ignoreMissing: Boolean)
      extends Condition with MultipleKeyValueSupport {
    /** Creates a copy of this condition that will ignore a missing key in a
      * request.
      */
    def ifExists: NumericCondition = copy(ignoreMissing = true)
  }

  /** Provides a fluent interface for building numeric conditions. */
  class NumericKey private[Condition] (key: String, ignoreMissing: Boolean) {
    /** Matches if it is any of the given values. */
    def is(values: Double*): NumericCondition =
      NumericCondition(key, NumericComparisonType.Equals, values, ignoreMissing)

    /** Matches if it is less than any of the given values. */
    def isLessThan(values: Double*): NumericCondition =
      NumericCondition(key, NumericComparisonType.LessThan, values, ignoreMissing)

    /** Matches if it is less than or equal to any of the given values. */
    def isLessThanOrEqualTo(values: Double*): NumericCondition =
      NumericCondition(key, NumericComparisonType.LessThanEquals, values, ignoreMissing)

    /** Matches if it is greater than any of the given values. */
    def isGreaterThan(values: Double*): NumericCondition =
      NumericCondition(key, NumericComparisonType.GreaterThan, values, ignoreMissing)

    /** Matches if it is greater than or equal to any of the given values. */
    def isGreaterThanOrEqualTo(values: Double*): NumericCondition =
      NumericCondition(key, NumericComparisonType.GreaterThanEquals, values, ignoreMissing)

    /** Matches if it is not any of the given values. */
    def isNot(values: Double*): NumericCondition =
      NumericCondition(key, NumericComparisonType.NotEquals, values, ignoreMissing)

    /** Makes the resulting condition be ignored if the given key is missing. */
    def ifExists: NumericKey = new NumericKey(key, ignoreMissing = true)
  }

  /** The enumeration of all string comparison types. */
  trait StringComparisonType
  object StringComparisonType {
    /** Case-sensitive exact string matching. */
    case object Equals extends StringComparisonType

    /** Negated form of [[Equals]]. */
    case object NotEquals extends StringComparisonType

    /** Case-insensitive string matching. */
    case object EqualsIgnoreCase extends StringComparisonType

    /** Negated form of [[NotEquals]]. */
    case object NotEqualsIgnoreCase extends StringComparisonType

    /** Loose case-insensitive matching.  The values can include a
      * multi-character match wildcard (`*`) or a single-character match
      * wildcard (?) anywhere in the string.
      */
    case object Like extends StringComparisonType

    /** Negated form of [[Like]]. */
    case object NotLike extends StringComparisonType

    val values: Seq[StringComparisonType] =
      Seq(Equals, NotEquals, EqualsIgnoreCase, NotEqualsIgnoreCase, Like, NotLike)
  }

  /** Condition for comparing the value of a key with a string.
    *
    * @param key the name of the value to match from the request
    * @param comparisonType the type of comparison to perform
    * @param values the string values against which to compare
    * @param ignoreMissing if true, if the key is missing from the request, the
    *                      condition will succeed.  Otherwise, a missing key
    *                      from the request will result in a failure.
    */
  case class StringCondition(key: String,
                             comparisonType: StringComparisonType,
                             values: Seq[String],
                             ignoreMissing: Boolean)
      extends Condition with MultipleKeyValueSupport {
    /** Creates a copy of this condition that will ignore a missing key in a
      * request.
      */
    def ifExists: StringCondition = copy(ignoreMissing = true)
  }

  /** Provides a fluent interface for building string conditions. */
  class StringKey private[Condition] (key: String, ignoreMissing: Boolean) {
    /** Exactly matches any of the given strings, case-sensitive. */
    def is(values: String*): StringCondition =
      StringCondition(key, StringComparisonType.Equals, values, ignoreMissing)

    /** Negated matching. */
    def isNot(values: String*): StringCondition =
      StringCondition(key, StringComparisonType.NotEquals, values,
        ignoreMissing)

    /** Exactly matches any of the given strings, case-insensitive. */
    def ignoringCaseIs(values: String*): StringCondition =
      StringCondition(key, StringComparisonType.EqualsIgnoreCase, values,
        ignoreMissing)

    /** Negated case-insensitive matching. */
    def ignoringCaseIsNot(values: String*): StringCondition =
      StringCondition(key, StringComparisonType.NotEqualsIgnoreCase, values,
        ignoreMissing)

    /** Loosely matches any of the given strings, case-sensitive.  The values
      * can include a multi-character match wildcard `*` or a single-character
      * match wildcard `?` anywhere in the string.
      */
    def isLike(values: String*): StringCondition =
      StringCondition(key, StringComparisonType.Like, values, ignoreMissing)

    /** Negated loose case-sensitive matching. */
    def isNotLike(values: String*): StringCondition =
      StringCondition(key, StringComparisonType.NotLike, values, ignoreMissing)

    /** Makes the resulting condition be ignored if the given key is missing. */
    def ifExists: StringKey = new StringKey(key, ignoreMissing = true)
  }

  /** Condition for checking whether a condition key is present at the time of
    * authorisation.
    *
    * @param key the name of the value to match from the request
    * @param value If `true`, the condition will match when the key does not
    *              exist.  If `false`, the condition will match when the key is
    *              present and contains a non-null value.
    */
  case class NullCondition private[Condition] (key: String, value: Boolean)
    extends Condition with MultipleKeyValueSupport

  /** An enumeration of all set operation types. */
  sealed trait SetOperation
  object SetOperation {
    /** Matches when every key value matches the operation. */
    case object ForAllValues extends SetOperation
    /** Matches when any key value matches the operation. */
    case object ForAnyValue extends SetOperation

    val values: Seq[SetOperation] = Seq(ForAllValues, ForAnyValue)
  }

  /** Qualifies a condition so that it performs a set operation against multiple
    * key values.
    *
    * @param op the operation to apply
    * @param condition the inner condition to apply to each key value
    */
  case class MultipleKeyValueCondition private[Condition](op: SetOperation,
                                                          condition: Condition with MultipleKeyValueSupport) extends Condition

  /** Adds support to a condition so that set operations may be applied to
    * it.
    */
  sealed trait MultipleKeyValueSupport { this: Condition ⇒
    /** Converts this condition into a condition that supports keys that contain
      * multiple values by enforcing that all values meet the condition.
      */
    def forAllValues: Condition.MultipleKeyValueCondition =
      Condition.MultipleKeyValueCondition(Condition.SetOperation.ForAllValues,
        this)

    /** Converts this condition into a condition that supports keys that contain
      * multiple values by enforcing that at least one value meets the condition.
      */
    def forAnyValue: Condition.MultipleKeyValueCondition =
      Condition.MultipleKeyValueCondition(Condition.SetOperation.ForAnyValue,
        this)
  }
}
