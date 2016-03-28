package com.monsanto.arch.awsutil.util

/** A utility trait for generating Scala-friendly enumerations from the ones in AWS. */
trait AwsEnumeration[AwsType <: Enum[AwsType]] {
  /** Returns the AWS value of this enumeration value. */
  def toAws: AwsType

  /** Returns the AWS string representation of this value. */
  override def toString = toAws.toString
}
