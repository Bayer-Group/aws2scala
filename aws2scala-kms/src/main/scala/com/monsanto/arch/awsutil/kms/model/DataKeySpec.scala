package com.monsanto.arch.awsutil.kms.model

import com.amazonaws.services.kms.model.{DataKeySpec ⇒ AWSDataKeySpec}

sealed trait DataKeySpec extends AnyRef {
  def toAws: AWSDataKeySpec

  override def toString: String = toAws.toString
}

object DataKeySpec {
  def apply(str: String): DataKeySpec = apply(AWSDataKeySpec.fromValue(str))

  def apply(aws: AWSDataKeySpec): DataKeySpec =
    aws match {
      case AWSDataKeySpec.AES_128 ⇒ Aes128
      case AWSDataKeySpec.AES_256 ⇒ Aes256
    }

  case object Aes128 extends DataKeySpec {
    override val toAws = AWSDataKeySpec.AES_128
  }

  case object Aes256 extends DataKeySpec {
    override val toAws = AWSDataKeySpec.AES_256
  }
}
