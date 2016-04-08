package com.monsanto.arch.awsutil.s3.model

import com.amazonaws.services.s3.{model ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

sealed abstract class CannedAccessControlList(val toAws: aws.CannedAccessControlList) extends AwsEnumeration[aws.CannedAccessControlList]

object CannedAccessControlList extends AwsEnumerationCompanion[CannedAccessControlList, aws.CannedAccessControlList] {
  /** Specifies the owner is granted [[Permission.FullControl]].  No one else has access rights.
    *
    * This is the default access control policy for any new buckets or objects.
    */
  case object Private extends CannedAccessControlList(aws.CannedAccessControlList.Private)

  /** Specifies the owner is granted [[Permission.FullControl]] and the [[Grantee.allUsers]] group grantee is granted
    * [[Permission.Read]] access.
    *
    * If this policy is used on an object, it can be read from a browser without authentication.
    */
  case object PublicRead extends CannedAccessControlList(aws.CannedAccessControlList.PublicRead)

  /** Specifies the owner is granted [[Permission.FullControl]] and the [[Grantee.allUsers]] group grantee is granted
    * [[Permission.Read]] and [[Permission.Write]] access.
    *
    * This access policy is not recommended for general use.
    */
  case object PublicReadWrite extends CannedAccessControlList(aws.CannedAccessControlList.PublicReadWrite)

  /** Specifies the owner is granted [[Permission.FullControl]] and the [[Grantee.authenticatedUsers]] group grantee
    * is granted [[Permission.Read]] access.
    */
  case object AuthenticatedRead extends CannedAccessControlList(aws.CannedAccessControlList.AuthenticatedRead)

  /** Specifies the owner is granted [[Permission.FullControl]] and the [[Grantee.logDelivery]] group grantee is
    * granted [[Permission.Write]] access so that access logs can be delivered.
    *
    * Use this access policy to enable Amazon S3 bucket logging for a bucket.  The destination bucket requires these
    * permissions so that access logs can be delivered.
    */
  case object LogDeliveryWrite extends CannedAccessControlList(aws.CannedAccessControlList.LogDeliveryWrite)

  /** Specifies the owner of the bucket, but not necessarily the same as the owner of the object, is granted
    * [[Permission.Read]].
    *
    * Use this access policy when uploading objects to another owner's bucket.  This access policy grants the bucket
    * owner read access to the object, but does not give read access for all users.
    */
  case object BucketOwnerRead extends CannedAccessControlList(aws.CannedAccessControlList.BucketOwnerRead)

  /** Specifies the owner of the bucket, but not necessarily the same as the owner of the object, is granted
    * [[Permission.FullControl]].
    *
    * Use this access policy to upload objects to another owner's bucket.   This access policy grants the bucket owner
    * full access to the object, but does not give full access to all users.
    */
  case object BucketOwnerFullControl extends CannedAccessControlList(aws.CannedAccessControlList.BucketOwnerFullControl)

  /** Specifies the owner is granted [[Permission.FullControl]].  Amazon EC2 is granted [[Permission.Read]] access to
    * GET an Amazon Machine Image (AMI) bundle from Amazon S3.
    */
  case object AwsExecRead extends CannedAccessControlList(aws.CannedAccessControlList.AwsExecRead)

  override def values: Seq[CannedAccessControlList] =
    Seq(Private, PublicRead, PublicReadWrite, AuthenticatedRead, LogDeliveryWrite, BucketOwnerRead,
      BucketOwnerFullControl, AwsExecRead)
}
