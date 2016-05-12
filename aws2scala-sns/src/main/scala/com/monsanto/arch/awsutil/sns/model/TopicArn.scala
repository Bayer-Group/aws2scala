package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.{Account, Arn}

/** Represents the ARN of an Amazon SNS topic.
  *
  * @param account the owner of the topic
  * @param region the region for the topic
  * @param name the name of the topic
  */
case class TopicArn(account: Account, region: Region, name: String) extends Arn(Arn.Namespace.AmazonSNS, Some(region), account) {
  override val resource = name
}

object TopicArn {
  /** Builds a topic ARN object from the given ARN string. */
  def apply(arnString: String): TopicArn =
    arnString match {
      case Arn(arn: TopicArn) ⇒ arn
      case _ ⇒ throw new IllegalArgumentException(s"‘$arnString’ is not a valid topic ARN.")
    }

  private[sns] val topicArnPF: PartialFunction[Arn.ArnParts, TopicArn] = {
    case (_, Arn.Namespace.AmazonSNS, Some(region), Some(owner), TopicResourceRegex(name)) ⇒
      TopicArn(owner, region, name)
  }

  private val TopicResourceRegex = "^([a-zA-Z0-9_-]{1,256})$".r
}
