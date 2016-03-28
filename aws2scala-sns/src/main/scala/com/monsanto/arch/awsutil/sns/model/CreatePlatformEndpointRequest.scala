package com.monsanto.arch.awsutil.sns.model

/** A basic data type for generating `CreatePlatformEndpointRequest` objects. */
case class CreatePlatformEndpointRequest(platformApplicationArn: String, token: String, customUserData: Option[String],
                                         attributes: Map[String,String])

object CreatePlatformEndpointRequest {
  /** Allows creation of `CreatePlatformEndpointRequest` objects using only a platform application ARN and a token. */
  def apply(platformApplicationArn: String, token: String): CreatePlatformEndpointRequest =
    CreatePlatformEndpointRequest(platformApplicationArn, token, None, Map.empty[String,String])

  /** Allows creation of `CreatePlatformEndpointRequest` objects using only a platform application ARN, a token, and
    * some custom user data.
    */
  def apply(platformApplicationArn: String, token: String, customUserData: String): CreatePlatformEndpointRequest =
    CreatePlatformEndpointRequest(platformApplicationArn, token, Some(customUserData), Map.empty[String,String])

  /** Allows creation of `CreatePlatformEndpointRequest` objects using only a platform application ARN, a token, and
    * extra attributes.
    */
  def apply(platformApplicationArn: String, token: String, attributes: Map[String,String]): CreatePlatformEndpointRequest =
    CreatePlatformEndpointRequest(platformApplicationArn, token, None, attributes)

  /** Allows creation of `CreatePlatformEndpointRequest` objects using only a platform application ARN, a token, custom
    * user data (not in an option), and extra attributes.
    */
  def apply(platformApplicationArn: String, token: String, customUserData: String, attributes: Map[String,String]): CreatePlatformEndpointRequest =
    CreatePlatformEndpointRequest(platformApplicationArn, token, Some(customUserData), attributes)
}
