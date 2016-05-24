package com.monsanto.arch.awsutil.auth.policy

import scala.util.Try

final case class Policy(version: Option[Policy.Version],
                        id: Option[String],
                        statements: Seq[Statement]) {
  def toJson: String = PolicyJsonSupport.policyToJson(this)
}

object Policy {
  /** Utility to build/extract `Policy` objects from their JSON serialisation. */
  object fromJson {
    /** Parses the given string as JSON and builds a `Policy` from it. */
    def apply(json: String): Policy = PolicyJsonSupport.jsonToPolicy(json)

    /** Extracts a policy given a string. */
    def unapply(json: String): Option[Policy] = Try(apply(json)).toOption
  }

  /** Type for all policy versions. */
  sealed abstract class Version(val id: String)

  object Version {
    /** The current version of the policy language which should be used for all policies. */
    case object `2012-10-17` extends Version("2012-10-17")
    /** An earlier version of the policy language which should not be used for any new policies. */
    case object `2008-10-17` extends Version("2008-10-17")

    val values: Seq[Version] = Seq(`2012-10-17`, `2008-10-17`)

    /** Utility to get/extract a `PolicyVersion` object from its identifiers. */
    object fromId {
      /** Returns the policy version corresponding to the given identifier.
        *
        * @throws java.lang.IllegalArgumentException if no policy version matches the given identifier
        */
      def apply(id: String): Policy.Version =
        unapply(id).getOrElse(throw new IllegalArgumentException(s"‘$id‘ is not a valid policy version"))

      /** Extracts a policy version given its identifier. */
      def unapply(id: String): Option[Policy.Version] = values.find(_.id == id)
    }
  }
}
