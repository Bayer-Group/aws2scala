package com.monsanto.arch.awsutil.converters

import com.amazonaws.services.kms.{model ⇒ aws}
import com.monsanto.arch.awsutil.kms.model.{KeyArn, KeyMetadata, KeyState, KeyUsage}

/** Utility for converting ''aws2scala-kms'' objects to/from their AWS counterparts. */
object KmsConverters {
  implicit class AwsKeyMetadata(val metadata: aws.KeyMetadata) extends AnyVal {
    def asScala: KeyMetadata = {
      val arn = KeyArn.fromArnString(metadata.getArn)
      KeyMetadata(
        arn.account,
        metadata.getKeyId,
        arn,
        metadata.getCreationDate,
        metadata.isEnabled.booleanValue(),
        Option(metadata.getDescription),
        KeyUsage.fromName(metadata.getKeyUsage),
        KeyState.fromName(metadata.getKeyState),
        Option(metadata.getDeletionDate))
    }
  }

  implicit class ScalaKeyMetadata(val metadata: KeyMetadata) extends AnyVal {
    def asAws: aws.KeyMetadata =
      new aws.KeyMetadata()
        .withArn(metadata.arn.arnString)
        .withAWSAccountId(metadata.account.id)
        .withCreationDate(metadata.creationDate)
        .withDeletionDate(metadata.deletionDate.orNull)
        .withDescription(metadata.description.orNull)
        .withEnabled(java.lang.Boolean.valueOf(metadata.enabled))
        .withKeyId(metadata.id)
        .withKeyState(metadata.state.asAws)
        .withKeyUsage(metadata.usage.asAws)
  }

  implicit class AwsKeyState(val keyState: aws.KeyState) extends AnyVal {
    def asScala: KeyState =
      keyState match {
        case aws.KeyState.Enabled         ⇒ KeyState.Enabled
        case aws.KeyState.Disabled        ⇒ KeyState.Disabled
        case aws.KeyState.PendingDeletion ⇒ KeyState.PendingDeletion
      }
  }

  implicit class ScalaKeyState(val keyState: KeyState) extends AnyVal {
    def asAws: aws.KeyState =
      keyState match {
        case KeyState.Enabled         ⇒ aws.KeyState.Enabled
        case KeyState.Disabled        ⇒ aws.KeyState.Disabled
        case KeyState.PendingDeletion ⇒ aws.KeyState.PendingDeletion
      }
  }

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
