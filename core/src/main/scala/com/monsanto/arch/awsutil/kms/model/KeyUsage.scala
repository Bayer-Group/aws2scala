package com.monsanto.arch.awsutil.kms.model

import com.amazonaws.services.kms.{model ⇒ aws}

sealed trait KeyUsage extends AnyRef {
  /** Returns the AWS enumeration value corresponding to the Key usage type. */
  def toAws: aws.KeyUsageType

  /** Ensures that the string representation matches the one AWS uses. */
  override def toString = toAws.toString
}

object KeyUsage {
  case object EncryptDecrypt extends KeyUsage {
    val toAws = aws.KeyUsageType.ENCRYPT_DECRYPT
  }

  def apply(str: String): KeyUsage = apply(aws.KeyUsageType.fromValue(str))

  def apply(awsKeyUsage: aws.KeyUsageType): KeyUsage =
    awsKeyUsage match {
      case aws.KeyUsageType.ENCRYPT_DECRYPT ⇒ EncryptDecrypt
    }
}
