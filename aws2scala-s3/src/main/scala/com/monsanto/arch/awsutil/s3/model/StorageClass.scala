package com.monsanto.arch.awsutil.s3.model

import com.amazonaws.services.s3.{model ⇒ aws}

/** Represents an S3 storage class. */
sealed trait StorageClass {
  def toAws: aws.StorageClass
}

object StorageClass {
  case object Glacier extends StorageClass {
    override val toAws = aws.StorageClass.Glacier
  }

  case object ReducedRedundancy extends StorageClass {
    override val toAws = aws.StorageClass.ReducedRedundancy
  }

  case object Standard extends StorageClass {
    override val toAws = aws.StorageClass.Standard
  }

  case object StandardInfrequentAccess extends StorageClass {
    override val toAws = aws.StorageClass.StandardInfrequentAccess
  }
}
