package com.monsanto.arch.awsutil.sns.model

import com.amazonaws.services.sns.model.{SetPlatformApplicationAttributesRequest ⇒ AwsSetPlatformApplicationAttributesRequest}

import scala.collection.JavaConverters._

/** A basic data type for generating `SetPlatformApplicationAttributesRequest` objects. */
case class SetPlatformApplicationAttributesRequest(platformApplicationArn: String, attributes: Map[String,String]) {
  def toAws: AwsSetPlatformApplicationAttributesRequest =
    new AwsSetPlatformApplicationAttributesRequest()
      .withPlatformApplicationArn(platformApplicationArn)
      .withAttributes(attributes.asJava)
}

object SetPlatformApplicationAttributesRequest {
  /** Allows creation of a `SetPlatformApplicationAttributesRequest` instance with an explicit name and value. */
  def apply(platformApplicationArn: String, attributeName: String, attributeValue: String): SetPlatformApplicationAttributesRequest =
    apply(platformApplicationArn, Map(attributeName → attributeValue))

  /** Allows creation of a `SetPlatformApplicationAttributesRequest` instance with an explicit name and optional value
    * (which gets converted to an empty string).
    */
  def apply(platformApplicationArn: String, attributeName: String, attributeValue: Option[String]): SetPlatformApplicationAttributesRequest =
    apply(platformApplicationArn, Map(attributeName → attributeValue.getOrElse("")))
}
