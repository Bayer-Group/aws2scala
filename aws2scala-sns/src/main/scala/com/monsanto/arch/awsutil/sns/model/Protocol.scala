package com.monsanto.arch.awsutil.sns.model

import java.net.URI

import com.monsanto.arch.awsutil.sns.model.SubscriptionEndpoint._

sealed trait Protocol {
  /** Creates an endpoint using this protocol. */
  def apply(endpoint: String): SubscriptionEndpoint
}

object Protocol {
  case object Http extends Protocol {
    override def apply(endpoint: String) = HttpEndpoint(URI.create(endpoint))
  }
  case object Https extends Protocol {
    override def apply(endpoint: String) = HttpsEndpoint(URI.create(endpoint))
  }
  case object Email extends Protocol {
    override def apply(endpoint: String) = EmailEndpoint(endpoint)
  }
  case object EmailJson extends Protocol {
    override def apply(endpoint: String) = EmailJsonEndpoint(endpoint)
  }
  case object SMS extends Protocol {
    override def apply(endpoint: String) = SMSEndpoint(endpoint)
  }
  case object SQS extends Protocol {
    override def apply(endpoint: String) = SQSEndpoint(endpoint)
  }
  case object Application extends Protocol {
    override def apply(endpoint: String) = ApplicationEndpoint(endpoint)
  }
  case object Lambda extends Protocol {
    override def apply(endpoint: String) = LambdaEndpoint(endpoint)
  }

  /** All of the available protocol values. */
  val values: Seq[Protocol] = Seq(Http, Https, Email, EmailJson, SMS, SQS, Application, Lambda)
}
