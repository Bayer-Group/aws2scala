package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.{Account, Arn}

private[awsutil] case class TopicArn(owner: Account, region: Region, name: String) extends Arn(Arn.Namespace.AmazonSNS, Some(region), owner) {
  override val resource = name
}

private[awsutil] object TopicArn {
  def apply(arn: String): TopicArn = {
    arn match {
      case Arn(_, Arn.Namespace.AmazonSNS, Some(region), Some(owner), TopicResourceRegex(name)) ⇒
        TopicArn(owner, region, name)
      case _ ⇒ throw new IllegalArgumentException(s"’$arn‘ is not a valid topic ARN")
    }
  }

  private val TopicResourceRegex = "^([a-zA-Z0-9_-]{1,256})$".r
}
