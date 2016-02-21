package com.monsanto.arch.awsutil.sns.model

/** A basic data type for generating `SetPlatformEndpointAttributesRequest` objects. */
case class SetPlatformEndpointAttributesRequest(platformEndpointArn: String, attributes: Map[String,String])
object SetPlatformEndpointAttributesRequest {
  /** Allows creation of `SetPlatformEndpointAttributesRequest` objects with an explicit name and value. */
  def apply(platformEndpointArn: String, attributeName: String, attributeValue: String): SetPlatformEndpointAttributesRequest =
    SetPlatformEndpointAttributesRequest(platformEndpointArn, Map(attributeName → attributeValue))

  /** Allows creation of `SetPlatformEndpointAttributesRequest` objects with an explicit name and optional value (which gets
    *  converted to an empty string.
    */
  def apply(platformEndpointArn: String, attributeName: String, attributeValue: Option[String]): SetPlatformEndpointAttributesRequest =
    SetPlatformEndpointAttributesRequest(platformEndpointArn, Map(attributeName → attributeValue.getOrElse("")))
}

