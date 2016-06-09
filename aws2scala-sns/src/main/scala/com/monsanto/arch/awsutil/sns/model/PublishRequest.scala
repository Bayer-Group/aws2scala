package com.monsanto.arch.awsutil.sns.model

import java.io.StringWriter

import com.fasterxml.jackson.core.JsonFactory

/** A basic data type for generating `PublishRequest` objects. */
case class PublishRequest(targetArn: String, message: String, subject: Option[String], messageStructure: Option[String],
                          attributes: Map[String,MessageAttributeValue])

object PublishRequest {
  /** Allows creating a `PublishRequest` object with just a target ARN and a message. */
  def apply(targetArn: String, message: String): PublishRequest = PublishRequest(targetArn, message, None, None, Map.empty)

  /** Allows creating a `PublishRequest` object with a target ARN, a message, and a subject. */
  def apply(targetArn: String, message: String, subject: String): PublishRequest =
    PublishRequest(targetArn, message, Some(subject), None, Map.empty)

  /** Allows creating a `PublishRequest` object with a target ARN, a message, and message attributes. */
  def apply(targetArn: String, message: String, attributes: Map[String,MessageAttributeValue]): PublishRequest =
    PublishRequest(targetArn, message, None, None, attributes)

  /** Allows creating a `PublishRequest` object with a target ARN, a message, a subject, and message attributes. */
  def apply(targetArn: String, message: String, subject: String,
            attributes: Map[String,MessageAttributeValue]): PublishRequest =
    PublishRequest(targetArn, message, Some(subject), None, attributes)

  /** Allows creating a `PublishRequest` object with a target ARN and different messages sent to different types of
    * endpoints.  The library will build a JSON object from the messages and escape all values.  Additionally,
    * it will ensure that the message structure is set to `json`.
    */
  def apply(targetArn: String, messages: Map[String,String]): PublishRequest =
    PublishRequest(targetArn, toJsonMessage(messages), None, Some("json"), Map.empty)

  /** Allows creating a `PublishRequest` object with a target ARN, different messages sent to different types of
    * endpoints, and a subject.  The library will build a JSON object from the messages and escape all values.
    * Additionally, it will ensure that the message structure is set to `json`.
    */
  def apply(targetArn: String, messages: Map[String,String], subject: String): PublishRequest =
    PublishRequest(targetArn, toJsonMessage(messages), Some(subject), Some("json"), Map.empty)

  /** Allows creating a `PublishRequest` object with a target ARN, different messages sent to different types of
    * endpoints, and with message attributes.  The library will build a JSON object from the messages and escape
    * all values.  Additionally, it will ensure that the message structure is set to `json`.
    */
  def apply(targetArn: String, messages: Map[String,String],
            attributes: Map[String,MessageAttributeValue]): PublishRequest =
    PublishRequest(targetArn, toJsonMessage(messages), None, Some("json"), attributes)

  /** Allows creating a `PublishRequest` object with a target ARN, different messages sent to different types of
    * endpoints, a subject, and with message attributes.  The library will build a JSON object from the messages
    * and escape all values.  Additionally, it will ensure that the message structure is set to `json`.
    */
  def apply(targetArn: String, messages: Map[String,String], subject: String,
            attributes: Map[String,MessageAttributeValue]): PublishRequest =
    PublishRequest(targetArn, toJsonMessage(messages), Some(subject), Some("json"), attributes)

  /** Allows creating a `PublishRequest` object for a platform endpoint with a JSON payload and some message attributes. */
  def apply(platformEndpoint: PlatformEndpoint, jsonMessage: String): PublishRequest =
    PublishRequest(platformEndpoint.arn, toJsonMessage(Map(platformEndpoint.platform.name → jsonMessage)), None,
      Some("json"), Map.empty)

  /** Allows creating a `PublishRequest` object for a platform endpoint with a JSON payload and some message attributes. */
  def apply(platformEndpoint: PlatformEndpoint, jsonMessage: String,
            attributes: Map[String,MessageAttributeValue]): PublishRequest =
    PublishRequest(platformEndpoint.arn, toJsonMessage(Map(platformEndpoint.platform.name → jsonMessage)), None,
      Some("json"), attributes)

  private val jsonFactory = new JsonFactory()

  private def toJsonMessage(message: Map[String,String]): String = {
    val writer = new StringWriter()
    val generator = jsonFactory.createGenerator(writer)
    try {
      generator.writeStartObject()
      message.foreach { entry ⇒
        generator.writeStringField(entry._1, entry._2)
      }
      generator.writeEndObject()
    } finally {
      generator.close()
      writer.close()
    }
    writer.toString
  }
}
