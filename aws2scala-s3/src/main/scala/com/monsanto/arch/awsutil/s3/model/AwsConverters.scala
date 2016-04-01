package com.monsanto.arch.awsutil.s3.model

import com.amazonaws.services.s3.{model ⇒ aws}

object AwsConverters {
  implicit class ScalaCreateBucketRequest(val request: CreateBucketRequest) extends AnyVal {
    def asAws: aws.CreateBucketRequest =
      request.region match {
        case Some(r) ⇒ new aws.CreateBucketRequest(request.bucketName, r.toAws)
        case None ⇒ new aws.CreateBucketRequest(request.bucketName)
      }
  }

  implicit class AwsGrant(val grant: aws.Grant) extends AnyVal {
    def asScala: Grant = Grant(grant.getGrantee.asScala, grant.getPermission.asScala)
  }

  implicit class ScalaGrant(val grant: Grant) extends AnyVal {
    def asAws: aws.Grant = new aws.Grant(grant.grantee.asAws, grant.permission.asAws)
  }

  implicit class ScalaGrantee(val grantee: Grantee) extends AnyVal {
    def asAws: aws.Grantee =
      grantee match {
        case Grantee.Canonical(id, None) ⇒
          new aws.CanonicalGrantee(id)
        case Grantee.Canonical(id, Some(displayName)) ⇒
          val g = new aws.CanonicalGrantee(id)
          g.setDisplayName(displayName)
          g
        case Grantee.EmailAddress(emailAddress) ⇒
          new aws.EmailAddressGrantee(emailAddress)
        case Grantee.AllUsers ⇒ aws.GroupGrantee.AllUsers
        case Grantee.AuthenticatedUsers ⇒ aws.GroupGrantee.AuthenticatedUsers
        case Grantee.LogDelivery ⇒ aws.GroupGrantee.LogDelivery
      }
  }

  implicit class AwsGrantee(val grantee: aws.Grantee) extends AnyVal {
    def asScala: Grantee =
      grantee match {
        case c: aws.CanonicalGrantee ⇒ Grantee.Canonical(c.getIdentifier, Option(c.getDisplayName))
        case e: aws.EmailAddressGrantee ⇒ Grantee.EmailAddress(e.getIdentifier)
        case aws.GroupGrantee.AllUsers ⇒ Grantee.AllUsers
        case aws.GroupGrantee.AuthenticatedUsers ⇒ Grantee.AuthenticatedUsers
        case aws.GroupGrantee.LogDelivery ⇒ Grantee.LogDelivery
      }
  }

  implicit class AwsPermission(val permission: aws.Permission) extends AnyVal {
    def asScala: Permission = Permission.fromAws(permission)
  }

  implicit class ScalaPermission(val permission: Permission) extends AnyVal {
    def asAws: aws.Permission = permission.toAws
  }
}
