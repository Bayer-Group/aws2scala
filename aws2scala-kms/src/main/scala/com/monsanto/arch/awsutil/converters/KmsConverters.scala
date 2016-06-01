package com.monsanto.arch.awsutil.converters

import com.amazonaws.services.kms.{model ⇒ aws}
import com.monsanto.arch.awsutil.kms.model.KeyUsage

/** Utility for converting ''aws2scala-kms'' objects to/from their AWS counterparts. */
object KmsConverters {
  implicit class AwsKeyUsage(val keyUsage: aws.KeyUsageType) extends AnyVal {
    def asScala: KeyUsage =
      keyUsage match {
        case aws.KeyUsageType.ENCRYPT_DECRYPT ⇒ KeyUsage.EncryptDecrypt
      }
  }

  implicit class ScalaKeyUsage(val keyUsage: KeyUsage) extends AnyVal {
    def asAws: aws.KeyUsageType =
      keyUsage match {
        case KeyUsage.EncryptDecrypt ⇒ aws.KeyUsageType.ENCRYPT_DECRYPT
      }
  }
}
