package com.monsanto.arch.awsutil.sqs.model

import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.{Account, Arn}

/** Represents the ARN of of an Amazon SQS queue.
  *
  * @param account the account to which the queue belongs
  * @param region the region in which the queue is located
  * @param name the unique name of the queue
  */
case class QueueArn(account: Account,
                    region: Region,
                    name: String) extends Arn(Arn.Namespace.AmazonSQS, Some(region), account) {
  override val resource = name
}

object QueueArn {
  /** Utility to build/extract `QueueArn` instances from strings containing ARNs. */
  object fromArnString {
    /** Builds a `QueueArn` object from the given ARN string. */
    def apply(arnString: String): QueueArn =
      unapply(arnString).getOrElse(throw new IllegalArgumentException(s"‘$arnString’ is not a valid queue ARN."))

    /** Extracts a `QueueArn` object from the given ARN string. */
    def unapply(arnString: String): Option[QueueArn] =
      arnString match {
        case Arn.fromArnString(arn: QueueArn) ⇒ Some(arn)
        case _                                ⇒ None
      }
  }

  private[awsutil] val queueArnPF: PartialFunction[Arn.ArnParts, QueueArn] = {
    case (_, Arn.Namespace.AmazonSQS, Some(region), Some(account), QueueResourceRegex(name)) ⇒
      QueueArn(account, region, name)
  }

  private val QueueResourceRegex = "^([a-zA-Z0-9_-]{1,80})$".r
}
