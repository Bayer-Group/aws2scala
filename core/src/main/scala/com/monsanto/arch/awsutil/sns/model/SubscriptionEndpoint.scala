package com.monsanto.arch.awsutil.sns.model

import java.net.URI

sealed trait SubscriptionEndpoint {
  def protocol: Protocol
  def endpoint: String
}

object SubscriptionEndpoint {
  case class HttpEndpoint(uri: URI) extends SubscriptionEndpoint {
    require(uri.getScheme == "http", "HTTP endpoints must have the ’http‘ scheme.")
    override def endpoint = uri.toString
    override def protocol = Protocol.Http
  }

  case class HttpsEndpoint(uri: URI) extends SubscriptionEndpoint {
    require(uri.getScheme == "https", "HTTPS endpoints must have the ’https‘ scheme.")
    override def endpoint = uri.toString
    override def protocol = Protocol.Https
  }

  case class EmailEndpoint(endpoint: String) extends SubscriptionEndpoint {
    override def protocol = Protocol.Email
  }

  case class EmailJsonEndpoint(endpoint: String) extends SubscriptionEndpoint {
    override def protocol = Protocol.EmailJson
  }

  case class SMSEndpoint(endpoint: String) extends SubscriptionEndpoint {
    override def protocol = Protocol.SMS
  }

  case class SQSEndpoint(endpoint: String) extends SubscriptionEndpoint {
    override def protocol = Protocol.SQS
  }

  case class ApplicationEndpoint(endpoint: String) extends SubscriptionEndpoint {
    override def protocol = Protocol.Application
  }

  case class LambdaEndpoint(endpoint: String) extends SubscriptionEndpoint {
    override def protocol = Protocol.Lambda
  }

  def unapply(subscriptionEndpoint: SubscriptionEndpoint): Option[(Protocol, String)] =
    Some((subscriptionEndpoint.protocol, subscriptionEndpoint.endpoint))
}
