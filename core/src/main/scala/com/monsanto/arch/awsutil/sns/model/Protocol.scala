package com.monsanto.arch.awsutil.sns.model

import java.net.URI

import com.monsanto.arch.awsutil.sns.model.SubscriptionEndpoint._

sealed trait Protocol {
  /** Returns the string that AWS uses for the protocol. */
  def toAws: String
  /** Creates an endpoint using this protocol. */
  def apply(endpoint: String): SubscriptionEndpoint
}

object Protocol {
  case object Http extends Protocol {
    override val toAws = "http"
    override def apply(endpoint: String) = HttpEndpoint(URI.create(endpoint))
  }
  case object Https extends Protocol {
    override val toAws = "https"
    override def apply(endpoint: String) = HttpsEndpoint(URI.create(endpoint))
  }
  case object Email extends Protocol {
    override val toAws = "email"
    override def apply(endpoint: String) = EmailEndpoint(endpoint)
  }
  case object EmailJson extends Protocol {
    override val toAws = "email-json"
    override def apply(endpoint: String) = EmailJsonEndpoint(endpoint)
  }
  case object SMS extends Protocol {
    override val toAws = "sms"
    override def apply(endpoint: String) = SMSEndpoint(endpoint)
  }
  case object SQS extends Protocol {
    override val toAws = "sqs"
    override def apply(endpoint: String) = SQSEndpoint(endpoint)
  }
  case object Application extends Protocol {
    override val toAws = "application"
    override def apply(endpoint: String) = ApplicationEndpoint(endpoint)
  }
  case object Lambda extends Protocol {
    override val toAws = "lambda"
    override def apply(endpoint: String) = LambdaEndpoint(endpoint)
  }

  /** Returns a Protocol object from the given AWS string. */
  def apply(aws: String): Protocol = {
    Values.find(_.toAws == aws).getOrElse(throw new IllegalArgumentException(s"’$aws‘ is not a valid protocol"))
  }

  /** All of the available protocol values. */
  val Values: Seq[Protocol] = Seq(Http, Https, Email, EmailJson, SMS, SQS, Application, Lambda)
}
