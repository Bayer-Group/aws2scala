package com.monsanto.arch.awsutil.s3.model

import com.amazonaws.services.s3.{model ⇒ aws}

/** Encapsulates a bucket name and key.  Big surprise. */
case class BucketNameAndKey(bucketName: String, key: String)

object BucketNameAndKey {
  /** Get a [[com.monsanto.arch.awsutil.s3.model.BucketNameAndKey BucketNameAndKey]] from an
    * [[com.amazonaws.services.s3.model.S3ObjectSummary S3ObjectSummary]].
    */
  def fromObjectSummary(summary: aws.S3ObjectSummary): BucketNameAndKey =
    BucketNameAndKey(summary.getBucketName, summary.getKey)

  /** Get a [[com.monsanto.arch.awsutil.s3.model.BucketNameAndKey BucketNameAndKey]] from a tuple. */
  def fromTuple(tuple: (String,String)): BucketNameAndKey = BucketNameAndKey(tuple._1, tuple._2)
}
