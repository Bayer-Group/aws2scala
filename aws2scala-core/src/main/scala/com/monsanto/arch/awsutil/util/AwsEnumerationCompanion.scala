package com.monsanto.arch.awsutil.util

/** A trait for companion objects of [[com.monsanto.arch.awsutil.util.AwsEnumeration AwsEnumeration]] classes. */
trait AwsEnumerationCompanion[ScalaType <: AwsEnumeration[AwsType], AwsType <: Enum[AwsType]] {
  /** All valid values for the enumeration. */
  def values: Seq[ScalaType]

  /** Given an AWS instance of the enumeration, return its Scala equivalent. */
  def fromAws(aws: AwsType): ScalaType = values.find(_.toAws == aws).get

  /** Given a string, return its enumerated Scala equivalent, if any. */
  def fromString(str: String): Option[ScalaType] = values.find(_.toString == str)

  /** Like [[fromString]], but works as an extractor. */
  def unapply(str: String): Option[ScalaType] = fromString(str)
}
