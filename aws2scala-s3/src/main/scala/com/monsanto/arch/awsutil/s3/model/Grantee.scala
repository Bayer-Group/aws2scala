package com.monsanto.arch.awsutil.s3.model

sealed trait Grantee {
  def identifier: String
  def typeIdentifier: String
}

object Grantee {
  /** Returns a `Grantee` for the given canonical ID. */
  def canonical(id: String): Grantee = Canonical(id, None)

  /** Returns a `Grantee` for the given e-mail address. */
  def emailAddress(emailAddress: String): Grantee = EmailAddress(emailAddress)

  /** Returns a `Grantee` that will grant anonymous access. */
  val allUsers: Grantee = AllUsers

  /** Returns a `Grantee` that will grant access to anyone with an Amazon account. */
  val authenticatedUsers: Grantee = AuthenticatedUsers

  /** Returns a `Grantee` that grants access to Amazon S3 log delivery. */
  val logDelivery: Grantee = LogDelivery

  /** Represents a grantee identified by their canonical Amazon ID.  The canonical Amazon ID can be thought of as
    * an Amazon-internal ID specific to a user.  For example, Amazon can map a grantee identified by an email address
    * to a canonical ID.
    *
    * Canonical grantees may have an associated display name, which is a human-friendly name that Amazon has linked to
    * the canonical ID (eg. the user’s * login name).
    */
  private[awsutil] case class Canonical(id: String, displayName: Option[String]) extends Grantee {
    override val identifier = id
    override val typeIdentifier = "id"
  }

  /** Represents an e-mail grantee.  An e-mail grantee is a grantee identified by their e-mail address and
    * authenticated by an Amazon system.
    *
    * E-mail grants are internally converted to the canonical user representation when creating the ACL.  If the
    * grantee changes their e-mail address, it will not affect existing Amazon S3 permissions.
    *
    * Adding a grantee by e-mail address only works if exactly one Amazon account corresponds to the specified e-mail
    * address. If multiple Amazon accounts are associated with the e-mail address, an `AmbiguousGrantByEmail` error
    * message is returned. This happens rarely, but usually occurs if a user created an Amazon account in the past,
    * forgotten the password, and created another Amazon account using the same e-mail address. If this occurs, the
    * user should contact Amazon customer service to have the accounts merged.  Alternatively, grant user access
    * specifying the canonical user representation.
    */
  private[awsutil] case class EmailAddress(emailAddress: String) extends Grantee {
    override val identifier = emailAddress
    override val typeIdentifier = "emailAddress"
  }

  /** Specifies groups of Amazon S3 users who can be granted permissions to Amazon S3 buckets and objects. */
  private[awsutil] sealed abstract class GroupGrantee extends Grantee {
    final override val typeIdentifier = "uri"
    final override def identifier = groupUri

    /** A string representation of an Amazon S3 group URI (eg. `http://acs.amazonaws.com/groups/global/AllUsers`). */
    def groupUri: String
  }

  /** Grants anonymous access to any Amazon S3 object or bucket. Any user will be able to access the object by
    * omitting the AWS Key ID and Signature from a request.
    *
    * Amazon highly recommends that users do not grant the `AllUsers` group write access to their buckets. If granted,
    * users will have no control over the objects others can store and their associated charges.
    */
  private[awsutil] case object AllUsers extends GroupGrantee {
    override val groupUri = "http://acs.amazonaws.com/groups/global/AllUsers"
  }

  /** Grants access to buckets or objects to anyone with an Amazon AWS account.  Although this is inherently insecure
    * as any AWS user who is aware of the bucket or object will be able to access it, users may find this
    * authentication method useful.
    */
  private[awsutil] case object AuthenticatedUsers extends GroupGrantee {
    override val groupUri = "http://acs.amazonaws.com/groups/global/AuthenticatedUsers"
  }

  /** Grants access to Amazon S3 log delivery so that an S3 bucket can receive server access logs. Turning on server
    * access logging for an Amazon S3 bucket requires that the bucket receiving the logs is granted permission for the
    * log delivery group to deliver logs.
    */
  private[awsutil] case object LogDelivery extends GroupGrantee {
    override val groupUri = "http://acs.amazonaws.com/groups/s3/LogDelivery"
  }
}
