package com.monsanto.arch.awsutil.kms.model

import java.util.Date

import com.amazonaws.services.kms.model.{KeyMetadata ⇒ AWSKeyMetadata}
import com.monsanto.arch.awsutil.converters.KmsConverters._

case class KeyMetadata(arn: String,
                       accountId: String,
                       creationDate: Date,
                       deletionDate: Option[Date],
                       description: Option[String],
                       enabled: Boolean,
                       id: String,
                       state: KeyState,
                       usage: KeyUsage) {
  def toAws: AWSKeyMetadata = {
    val aws = new AWSKeyMetadata
    aws.setArn(arn)
    aws.setAWSAccountId(accountId)
    aws.setCreationDate(creationDate)
    aws.setDeletionDate(deletionDate.orNull)
    aws.setDescription(description.orNull)
    aws.setEnabled(enabled)
    aws.setKeyId(id)
    aws.setKeyState(state.toAws)
    aws.setKeyUsage(usage.asAws)
    aws
  }
}

object KeyMetadata {
  def apply(aws: AWSKeyMetadata): KeyMetadata =
    KeyMetadata(
      aws.getArn,
      aws.getAWSAccountId,
      aws.getCreationDate,
      Option(aws.getDeletionDate),
      Option(aws.getDescription),
      aws.isEnabled,
      aws.getKeyId,
      KeyState(aws.getKeyState),
      KeyUsage.fromName(aws.getKeyUsage))
}
