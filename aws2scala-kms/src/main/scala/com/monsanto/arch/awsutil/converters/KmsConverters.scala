package com.monsanto.arch.awsutil.converters

import com.amazonaws.services.kms.{model ⇒ aws}
import com.monsanto.arch.awsutil.kms.model._

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
        case aws.KeyState.PendingImport   ⇒ KeyState.PendingImport
      }
  }

  implicit class ScalaKeyState(val keyState: KeyState) extends AnyVal {
    def asAws: aws.KeyState =
      keyState match {
        case KeyState.Enabled         ⇒ aws.KeyState.Enabled
        case KeyState.Disabled        ⇒ aws.KeyState.Disabled
        case KeyState.PendingDeletion ⇒ aws.KeyState.PendingDeletion
        case KeyState.PendingImport   ⇒ aws.KeyState.PendingImport
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

  implicit class ScalaCreateKeyRequest(val request: CreateKeyWithAliasRequest) extends AnyVal {
    def asAws: aws.CreateKeyRequest =
      new aws.CreateKeyRequest()
        .withKeyUsage(request.keyUsage.asAws)
        .withPolicy(request.policy.map(_.toJson).orNull)
        .withDescription(request.description.orNull)
        .withBypassPolicyLockoutSafetyCheck(
          request.bypassPolicyLockoutSafetyCheck.map(java.lang.Boolean.valueOf).orNull)
  }
}
