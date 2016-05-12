package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.{Account, Arn}

/** Represents the ARN of an Amazon SNS subscription.
  *
  * @param account the owner of the SNS subscription
  * @param region the region in which the subscription is located
  * @param topicName the name of the topic to which the subscription is subscribed
  * @param subscriptionId the unique identifier for the subscription
  */
case class SubscriptionArn(account: Account,
                           region: Region,
                           topicName: String,
                           subscriptionId: String) extends Arn(Arn.Namespace.AmazonSNS, Some(region), account) {
  override val resource = s"$topicName:$subscriptionId"
}

object SubscriptionArn {
  /** Builds a subscription ARN object from the given ARN string. */
  def apply(arnString: String): SubscriptionArn = {
    arnString match {
      case Arn(arn: SubscriptionArn) ⇒ arn
      case _ ⇒ throw new IllegalArgumentException(s"’$arnString‘ is not a valid subscription ARN")
    }
  }

  private[sns] val subscriptionArnPF: PartialFunction[Arn.ArnParts, SubscriptionArn] = {
    case (_, Arn.Namespace.AmazonSNS, Some(region), Some(owner), SubscriptionResourceRegex(name, id)) ⇒
      SubscriptionArn(owner, region, name, id)
  }

  private val SubscriptionResourceRegex = "^([a-zA-Z0-9_-]{1,256}):([0-9a-f-]+)$".r
}

