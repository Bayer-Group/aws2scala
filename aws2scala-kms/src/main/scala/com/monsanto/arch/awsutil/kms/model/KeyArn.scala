package com.monsanto.arch.awsutil.kms.model

import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.{Account, Arn}

/** Represents th ARN of a KMS key.
  *
  * @param account the account to which the key belongs
  * @param region the region in which the key resides
  * @param id the globally unique key identifier
  */
case class KeyArn(account: Account,
                  region: Region,
                  id: String) extends Arn(Arn.Namespace.AwsKMS, Some(region), account) {
  override val resource: String =  s"key/$id"
}

object KeyArn {
  /** Utility to build/extract `KeyArn` instances from strings containing ARNs. */
  object fromArnString {
    /** Builds a `KeyArn` object from the given ARN string. */
    def apply(arnString: String): KeyArn =
      unapply(arnString).getOrElse(throw new IllegalArgumentException(s"‘$arnString’ is not a valid key ARN."))

    /** Extracts a `KeyArn` object from the given ARN string. */
    def unapply(arnString: String): Option[KeyArn] =
      arnString match {
        case Arn.fromArnString(arn: KeyArn) ⇒ Some(arn)
        case _                              ⇒ None
      }
  }

  private[awsutil] val keyArnPF: PartialFunction[Arn.ArnParts, KeyArn] = {
    case (_, Arn.Namespace.AwsKMS, Some(region), Some(account), KeyResourceRegex(id)) ⇒
      KeyArn(account, region, id)
  }

  private val KeyResourceRegex = "^key/(.+)$".r
}
