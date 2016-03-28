package com.monsanto.arch.awsutil.sns.model

sealed trait MessageAttributeValue

object MessageAttributeValue {
  def apply(value: Array[Byte]): MessageAttributeValue =  BinaryValue(value)
  def apply(value: String): MessageAttributeValue =  StringValue(value)

  private[awsutil] case class BinaryValue(value: Array[Byte]) extends MessageAttributeValue
  private[awsutil] case class StringValue(value: String) extends MessageAttributeValue
}
