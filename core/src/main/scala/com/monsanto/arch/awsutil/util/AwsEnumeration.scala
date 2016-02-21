package com.monsanto.arch.awsutil.util

/** A utility trait for generating Scala-friendly enumerations from the ones in AWS. */
trait AwsEnumeration[T <: Enum[T]] {
  /** The type of the AWS enumeration. */
  type AwsType = T

  /** Returns the AWS value of this enumeration value. */
  def toAws: AwsType

  /** Returns the AWS string representation of this value. */
  override def toString = toAws.toString
}

trait AwsEnumerationCompanion[T <: AwsEnumeration[_]] {
  /** The type of the Scala enumeration. */
  type ScalaType = T
  /** The type of the AWS enumeration. */
  type AwsType = ScalaType#AwsType

  /** All valid values for the enumeration. */
  def values: Seq[ScalaType]

  /** Given an AWS instance of the enumeration, return its Scala equivalent. */
  def fromAws(aws: AwsType): ScalaType = values.find(_.toAws == aws).get

  /** Given a string, return its enumerated Scala equivalent, if any. */
  def fromString(str: String): Option[ScalaType] = values.find(_.toString == str)
}
