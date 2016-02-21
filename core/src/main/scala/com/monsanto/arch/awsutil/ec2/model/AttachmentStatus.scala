package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.model.{AttachmentStatus ⇒ AwsAttachmentStatus}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

sealed abstract class AttachmentStatus(val toAws: AwsAttachmentStatus) extends AwsEnumeration[AwsAttachmentStatus]

object AttachmentStatus extends AwsEnumerationCompanion[AttachmentStatus] {
  case object Attached extends AttachmentStatus(AwsAttachmentStatus.Attached)
  case object Attaching extends AttachmentStatus(AwsAttachmentStatus.Attaching)
  case object Detached extends AttachmentStatus(AwsAttachmentStatus.Detached)
  case object Detaching extends AttachmentStatus(AwsAttachmentStatus.Detaching)

  override val values: Seq[AttachmentStatus] = Seq(Attached, Attaching, Detached, Detaching)
}
