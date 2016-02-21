package com.monsanto.arch.awsutil.sns.model

import java.nio.ByteBuffer

import com.amazonaws.services.sns.model.{MessageAttributeValue ⇒ AWSMessageAttributeValue}
import com.monsanto.arch.awsutil.sns.model.MessageAttributeValue.AwsAdapter

/** Contains a message attribute value. */
case class MessageAttributeValue[T: AwsAdapter](value: T) {
  /** Converts an arbitrary value to an SNS-compatible message attribute value. */
  private[sns] def toAws = implicitly[AwsAdapter[T]].toAws(value)
}

object MessageAttributeValue {
  /** Type class for converting a particular value to an AWS `MessageAttributeValue` instance. */
  trait AwsAdapter[T] {
    def toAws(value: T): AWSMessageAttributeValue
  }

  object AwsAdapter {
    /** Supports string values. */
    implicit object StringAwsAdapter extends AwsAdapter[String] {
      override def toAws(value: String) = new AWSMessageAttributeValue().withDataType("String").withStringValue(value)
    }

    /** Supports byte array values. */
    implicit object ByteArrayAwsAdapter extends AwsAdapter[Array[Byte]] {
      override def toAws(value: Array[Byte]) =
        new AWSMessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(value))
    }
  }
}
