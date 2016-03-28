package com.monsanto.arch.awsutil.sns.model

import com.amazonaws.services.sns.{model ⇒ aws}

import scala.collection.JavaConverters._

/** Contains all of the information necessary to create an SNS platform application.
  *
  * @param name the name of the application
  * @param platform the identifier for the push notification platform
  * @param attributes all of the attributes needed to set up the platform application
  */
case class CreatePlatformApplicationRequest private (name: String, platform: String, attributes: Map[String,String]) {
  def toAws: aws.CreatePlatformApplicationRequest =
    new aws.CreatePlatformApplicationRequest()
      .withName(name)
      .withPlatform(platform)
      .withAttributes(attributes.asJava)
}

object CreatePlatformApplicationRequest {
  /** Contains all of the information necessary to create an SNS platform application.
    *
    * @param name the name of the application
    * @param platform the identifier for the push notification platform
    * @param platformPrincipal the principal received from the push notification service
    * @param platformCredential the credential received from the push notification service
    */
  def apply(name: String,
            platform: String,
            platformPrincipal: String,
            platformCredential: String): CreatePlatformApplicationRequest =
    apply(name, platform, platformPrincipal, platformCredential, Map.empty)

  /** Contains all of the information necessary to create an SNS platform application.
    *
    * @param name the name of the application
    * @param platform the identifier for the push notification platform
    * @param platformPrincipal the principal received from the push notification service
    * @param platformCredential the credential received from the push notification service
    * @param attributes additional attributes to use to set up the platform application
    */
  def apply(name: String,
            platform: String,
            platformPrincipal: String,
            platformCredential: String,
            attributes: Map[String,String]): CreatePlatformApplicationRequest =
    apply(name, platform, attributes + ("PlatformPrincipal" → platformPrincipal) + ("PlatformCredential" → platformCredential))
}
