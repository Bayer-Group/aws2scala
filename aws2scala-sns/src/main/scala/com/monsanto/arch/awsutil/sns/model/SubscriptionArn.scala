package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.{Account, Arn}

private[awsutil] case class SubscriptionArn(owner: Account, region: Region, applicationName: String, subscriptionId: String) extends Arn(Arn.Namespace.AmazonSNS, Some(region), owner) {
  override val resource = s"$applicationName:$subscriptionId"
}

private[awsutil] object SubscriptionArn {
  def apply(arn: String): SubscriptionArn = {
    arn match {
      case Arn(_, Arn.Namespace.AmazonSNS, Some(region), Some(owner), SubscriptionResourceRegex(name, id)) ⇒
        SubscriptionArn(owner, region, name, id)
      case _ ⇒ throw new IllegalArgumentException(s"’$arn‘ is not a valid subscription ARN")
    }
  }

  private val SubscriptionResourceRegex = "^([a-zA-Z0-9_-]{1,256}):([0-9a-f-]+)$".r
}

