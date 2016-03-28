package com.monsanto.arch.awsutil.sns.model

import java.nio.ByteBuffer
import java.{util ⇒ ju}

import com.amazonaws.services.sns.{model ⇒ aws}

import scala.collection.JavaConverters._

object AwsConverters {
  implicit class ScalaAddPermissionRequest(val request: AddPermissionRequest) extends AnyVal {
    def asAws: aws.AddPermissionRequest =
      new aws.AddPermissionRequest(request.topicArn, request.label, request.accounts.asJava,
        request.actions.map(_.toString).asJava)
  }

  implicit class ScalaConfirmSubscriptionRequest(val request: ConfirmSubscriptionRequest) extends AnyVal {
    def asAws: aws.ConfirmSubscriptionRequest = {
      request.authenticateOnUnsubscribe match {
        case None ⇒ new aws.ConfirmSubscriptionRequest(request.topicArn, request.token)
        case Some(b) ⇒ new aws.ConfirmSubscriptionRequest(request.topicArn, request.token, b.toString)
      }
    }
  }

  implicit class ScalaMessageAttributeValue(val value: MessageAttributeValue) extends AnyVal {
    def asAws: aws.MessageAttributeValue =
      value match {
        case MessageAttributeValue.StringValue(str) ⇒
          new aws.MessageAttributeValue().withDataType("String").withStringValue(str)
        case MessageAttributeValue.BinaryValue(bytes) ⇒
          new aws.MessageAttributeValue().withDataType("Binary").withBinaryValue(ByteBuffer.wrap(bytes))
      }
  }

  implicit class ScalaMessageAttributesMap(val attributes: Map[String, MessageAttributeValue]) extends AnyVal {
    def asAws: ju.Map[String,aws.MessageAttributeValue] =
      attributes.mapValues(_.asAws).asJava
  }

  implicit class AwsPlatformEndpoint(val endpoint: aws.Endpoint) extends AnyVal {
    def asScala: PlatformEndpoint = PlatformEndpoint(endpoint.getEndpointArn, endpoint.getAttributes.asScala.toMap)
  }

  implicit class ScalaPlatformEndpoint(val endpoint: PlatformEndpoint) extends AnyVal {
    def asAws: aws.Endpoint =
      new aws.Endpoint()
        .withEndpointArn(endpoint.arn)
        .withAttributes(endpoint.attributes.asJava)
  }

  implicit class ScalaProtocol(val protocol: Protocol) extends AnyVal {
    def asAws: String =
      protocol match {
        case Protocol.Application ⇒ "application"
        case Protocol.Email       ⇒ "email"
        case Protocol.EmailJson   ⇒ "email-json"
        case Protocol.Http        ⇒ "http"
        case Protocol.Https       ⇒ "https"
        case Protocol.Lambda      ⇒ "lambda"
        case Protocol.SMS         ⇒ "sms"
        case Protocol.SQS         ⇒ "sqs"
      }
  }

  implicit class AwsProtocol(val str: String) extends AnyVal {
    def asScala: Protocol =
      str match {
        case "application" ⇒ Protocol.Application
        case "email"       ⇒ Protocol.Email
        case "email-json"  ⇒ Protocol.EmailJson
        case "http"        ⇒ Protocol.Http
        case "https"       ⇒ Protocol.Https
        case "lambda"      ⇒ Protocol.Lambda
        case "sms"         ⇒ Protocol.SMS
        case "sqs"         ⇒ Protocol.SQS
        case _             ⇒ throw new IllegalArgumentException(s"’$str‘ is not a valid protocol.")
      }
  }

  implicit class ScalaPublishRequest(val request: PublishRequest) extends AnyVal {
    def asAws: aws.PublishRequest =
      new aws.PublishRequest()
        .withMessage(request.message)
        .withMessageAttributes(request.attributes.asAws)
        .withMessageStructure(request.messageStructure.orNull)
        .withSubject(request.subject.orNull)
        .withTargetArn(request.targetArn)
  }

  implicit class ScalaRemovePermissionRequest(val request: RemovePermissionRequest) extends AnyVal {
    def asAws: aws.RemovePermissionRequest = new aws.RemovePermissionRequest(request.topicArn, request.label)
  }

  implicit class ScalaSubscriptionSummary(val summary: SubscriptionSummary) extends AnyVal {
    def asAws: aws.Subscription =
      new aws.Subscription()
        .withSubscriptionArn(summary.arn.getOrElse("PendingConfirmation"))
        .withTopicArn(summary.topicArn)
        .withProtocol(summary.endpoint.protocol.asAws)
        .withEndpoint(summary.endpoint.endpoint)
        .withOwner(summary.owner)
  }
}
