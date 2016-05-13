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
  /** Builds a queue ARN object from the given ARN string. */
  def apply(arnString: String): QueueArn =
    arnString match {
      case Arn(arn: QueueArn) ⇒ arn
      case _ ⇒ throw new IllegalArgumentException(s"‘$arnString’ is not a valid queue ARN.")
    }

  private[awsutil] val queueArnPF: PartialFunction[Arn.ArnParts, QueueArn] = {
    case (_, Arn.Namespace.AmazonSQS, Some(region), Some(account), QueueResourceRegex(name)) ⇒
      QueueArn(account, region, name)
  }

  private val QueueResourceRegex = "^([a-zA-Z0-9_-]{1,80})$".r
}
