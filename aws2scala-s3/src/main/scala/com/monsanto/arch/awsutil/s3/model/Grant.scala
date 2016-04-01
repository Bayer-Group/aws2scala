package com.monsanto.arch.awsutil.s3.model

/** Specifies a grant in a bucket or object ACL.
  *
  * @param grantee the grantee being ranted a permission by this grant
  * @param permission the permission being granted to the grantee by this grant
  */
case class Grant(grantee: Grantee, permission: Permission)
