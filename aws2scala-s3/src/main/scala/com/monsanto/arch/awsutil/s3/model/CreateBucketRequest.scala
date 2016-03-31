package com.monsanto.arch.awsutil.s3.model

sealed abstract class CreateBucketRequest {
  def bucketName: String
  def region: Option[Region]
}

object CreateBucketRequest {
  /** Returns a `CreateBucketRequest` using the default region and no ACL.
    *
    * @param bucketName the name of the bucket to create
    */
  def apply(bucketName: String): CreateBucketRequest = CreateBucketWithNoAcl(bucketName, None)

  /** Returns a `CreateBucketRequest` using the given region and no ACL.
    *
    * @param bucketName the name of the bucket to create
    * @param region the region in which to locate the bucket
    */
  def apply(bucketName: String, region: Region): CreateBucketRequest = CreateBucketWithNoAcl(bucketName, Some(region))

  private[awsutil] case class CreateBucketWithNoAcl(bucketName: String, region: Option[Region]) extends CreateBucketRequest
}

