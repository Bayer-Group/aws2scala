package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.converters.CoreConverters._

final case class Policy(version: Policy.Version,
                        id: Option[String],
                        statements: Seq[Statement]) {
  def toJson: String = this.asAws.toJson
}

object Policy {
  def fromJson(json: String): Policy = aws.Policy.fromJson(json).asScala

  /** Type for all policy versions. */
  sealed abstract class Version(val id: String)

  object Version {
    /** The current version of the policy language which should be used for all policies. */
    case object `2012-10-17` extends Version("2012-10-17")
    /** An earlier version of the policy language which should not be used for any new policies. */
    case object `2008-10-17` extends Version("2008-10-17")

    val values: Seq[Version] = Seq(`2012-10-17`, `2008-10-17`)

    /** Returns the policy version corresponding to the given ID. */
    def apply(id: String): Policy.Version =
      fromId.unapply(id)
        .getOrElse(throw new IllegalArgumentException(s"‘$id‘ is not a valid policy version"))

    /** Extractor to get a policy version from its ID. */
    object fromId {
      def unapply(id: String): Option[Policy.Version] = values.find(_.id == id)
    }
  }
}
