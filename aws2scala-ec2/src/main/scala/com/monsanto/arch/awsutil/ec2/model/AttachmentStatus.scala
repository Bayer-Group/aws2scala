package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

sealed abstract class AttachmentStatus(val toAws: aws.AttachmentStatus) extends AwsEnumeration[aws.AttachmentStatus]

object AttachmentStatus extends AwsEnumerationCompanion[AttachmentStatus, aws.AttachmentStatus] {
  case object Attached extends AttachmentStatus(aws.AttachmentStatus.Attached)
  case object Attaching extends AttachmentStatus(aws.AttachmentStatus.Attaching)
  case object Detached extends AttachmentStatus(aws.AttachmentStatus.Detached)
  case object Detaching extends AttachmentStatus(aws.AttachmentStatus.Detaching)

  override val values: Seq[AttachmentStatus] = Seq(Attached, Attaching, Detached, Detaching)
}
