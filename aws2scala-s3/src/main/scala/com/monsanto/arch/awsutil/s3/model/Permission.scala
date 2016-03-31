package com.monsanto.arch.awsutil.s3.model

import com.amazonaws.services.s3.{model ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

abstract class Permission(val toAws: aws.Permission) extends AwsEnumeration[aws.Permission]

object Permission extends AwsEnumerationCompanion[Permission, aws.Permission] {
  /** Provides READ, WRITE, READ_ACP, and WRITE_ACP permissions.
    *
    * It does not convey additional rights and is provided only for convenience.
    */
  case object FullControl extends Permission(aws.Permission.FullControl)

  /** Grants permission to list the bucket when applied to a bucket; grants permission to read object data and/or
    * metadata when applied to an object.
    */
  case object Read extends Permission(aws.Permission.Read)

  /** Grants permission to create, overwrite, and delete any objects in the bucket.
    *
    * This permission is not supported for objects.
    */
  case object Write extends Permission(aws.Permission.Write)

  /** Grants permission to read the ACL for the applicable bucket or object.
    *
    * The owner of a bucket or object always implicitly has this permission.
    */
  case object ReadAcp extends Permission(aws.Permission.ReadAcp)

  /** Gives permission to overwrite the ACP for the applicable bucket or * object.
    *
    * The owner of a bucket or object always has this permission implicitly.
    *
    * Granting this permission is equivalent to granting `FULL_CONTROL` because the grant recipient can make any
    * changes to the ACP.
    */
  case object WriteAcp extends Permission(aws.Permission.WriteAcp)

  override def values: Seq[Permission] = Seq(FullControl, Read, Write, ReadAcp, WriteAcp)
}
