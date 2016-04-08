package com.monsanto.arch.awsutil.s3.model

sealed trait CreateBucketRequest {
  def bucketName: String
  def region: Option[Region]
  def acl: Option[Either[CannedAccessControlList,Seq[Grant]]]
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

  /** Returns a `CreateBucketRequest` using the default region and the given canned ACL.
    *
    * @param bucketName the name of the bucket to create
    * @param cannedAcl the canned access control list to use
    */
  def apply(bucketName: String, cannedAcl: CannedAccessControlList): CreateBucketRequest =
    CreateBucketWithCannedAcl(bucketName, cannedAcl, None)

  /** Returns a `CreateBucketRequest` using the given region and canned ACL.
    *
    * @param bucketName the name of the bucket to create
    * @param cannedAcl the canned access control list to use
    * @param region the region in which to locate the bucket
    */
  def apply(bucketName: String, cannedAcl: CannedAccessControlList, region: Region): CreateBucketRequest =
    CreateBucketWithCannedAcl(bucketName, cannedAcl, Some(region))

  /** Returns a `CreateBucketRequest` using the default region and with an ACL specified by a list of grants.
    *
    * @param bucketName the name of the bucket to create
    * @param grants the list of grants to be used as an access control list
    */
  def apply(bucketName: String, grants: Seq[Grant]): CreateBucketRequest =
    CreateBucketWithGrants(bucketName, grants, None)

  /** Returns a `CreateBucketRequest` using the given region and with an ACL specified by a list of grants.
    *
    * @param bucketName the name of the bucket to create
    * @param grants the list of grants to be used as an access control list
    * @param region the region in which to locate the bucket
    */
  def apply(bucketName: String, grants: Seq[Grant], region: Region): CreateBucketRequest =
    CreateBucketWithGrants(bucketName, grants, Some(region))

  /** A [[com.monsanto.arch.awsutil.s3.model.CreateBucketRequest CreateBucketRequest]] that does not specify an access
    * control list.
    */
  private[awsutil] case class CreateBucketWithNoAcl(bucketName: String, region: Option[Region]) extends CreateBucketRequest {
    override val acl = None
  }

  /** A [[com.monsanto.arch.awsutil.s3.model.CreateBucketRequest CreateBucketRequest]] that specifies a canned access
    * control list.
    */
  private[awsutil] case class CreateBucketWithCannedAcl(bucketName: String,
                                                        cannedAcl: CannedAccessControlList,
                                                        region: Option[Region]) extends CreateBucketRequest {
    override val acl = Some(Left(cannedAcl))
  }

  /** A [[com.monsanto.arch.awsutil.s3.model.CreateBucketRequest CreateBucketRequest]] that specifies an access
    * control list as a list of grants.
    */
  private[awsutil] case class CreateBucketWithGrants(bucketName: String,
                                                     grants: Seq[Grant],
                                                     region: Option[Region]) extends CreateBucketRequest {
    override val acl = Some(Right(grants))
  }
}

