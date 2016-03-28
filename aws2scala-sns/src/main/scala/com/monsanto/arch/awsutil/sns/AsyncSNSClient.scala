package com.monsanto.arch.awsutil.sns

import akka.Done
import akka.stream.Materializer
import com.monsanto.arch.awsutil.AsyncAwsClient
import com.monsanto.arch.awsutil.auth.policy.action.SNSAction
import com.monsanto.arch.awsutil.sns.model.{MessageAttributeValue, PlatformApplication, PlatformEndpoint, SubscriptionSummary}

import scala.concurrent.Future

trait AsyncSNSClient extends AsyncAwsClient {
  /** Creates a topic to which notification can be published.
    *
    * @param name the name of the topic to create
    * @return the ARN assigned to the created topic
    */
  def createTopic(name: String)(implicit m: Materializer): Future[String]

  /** Deletes a topic and all of its subscriptions.
    *
    * @param topicArn the ARN of the topic to delete
    */
  def deleteTopic(topicArn: String)(implicit m: Materializer): Future[Done]

  /** Lists all available topic ARNs.
    *
    * @return a list of topic ARNs
    */
  def listTopics()(implicit m: Materializer): Future[Seq[String]]

  /** Return a specific attribute of a topic.
    *
    * @param topicArn the ARN of the topic from which to retrieve the attribute
    * @param name the name of the attribute to retrieve
    * @return the value of the attribute, if any
    */
  def getTopicAttribute(topicArn: String, name: String)(implicit m: Materializer): Future[Option[String]]

  /** Return all of the attributes of a topic. */
  def getTopicAttributes(topicArn: String)(implicit m: Materializer): Future[Map[String,String]]

  /** Sets an attribute of a topic to a new value.
    *
    * @param topicArn the ARN of the topic to modify
    * @param attributeName the name of the attribute to set
    * @param attributeValue the new value for the attribute
    */
  def setTopicAttribute(topicArn: String, attributeName: String, attributeValue: String)
                       (implicit m: Materializer): Future[Done]

  /** Sets an attribute of a topic to a new value.
    *
    * @param topicArn the ARN of the topic to modify
    * @param attributeName the name of the attribute to set
    * @param attributeValue the new value for the attribute (if any)
    */
  def setTopicAttribute(topicArn: String, attributeName: String, attributeValue: Option[String])
                       (implicit m: Materializer): Future[Done]

  /** Adds a statement to a topic’s access control policy, granting access for the specified AWS accounts to the
    * specified actions.
    *
    * @param topicArn the ARN of the topic whose access control policy to modify
    * @param label a unique identifier for the new policy statement
    * @param accounts the AWS account IDs of the users (principals) who will be given access to the specified actions
    * @param actions the actions you want to allow for the specified principals
    */
  def addPermission(topicArn: String, label: String, accounts: Seq[String], actions: Seq[SNSAction])
                   (implicit m: Materializer): Future[Done]

  /** Removes a statement from a topic’s access control policy.
    *
    * @param topicArn the ARN of the topic whose access control policy to modify
    * @param label the unique identifier of the statement you want to remove
    */
  def removePermission(topicArn: String, label: String)(implicit m: Materializer): Future[Done]

  /** Returns a list of all of the available subscriptions. */
  def listSubscriptions()(implicit m: Materializer): Future[Seq[SubscriptionSummary]]

  /** Returns a list of all of the available subscriptions to a specific topic. */
  def listSubscriptions(topicArn: String)(implicit m: Materializer): Future[Seq[SubscriptionSummary]]

  /** Prepares to subscribe an endpoint by sending the endpoint a confirmation message.
    *
    * @param topicArn the ARN of the topic to which to subscribe
    * @param protocol the protocol to use for the subscription
    * @param endpoint the endpoint that will receive notifications
    * @return the ARN of the subscription if the service was able to create a subscription immediately.
    */
  def subscribe(topicArn: String, protocol: String, endpoint: String)(implicit m: Materializer): Future[Option[String]]

  /** Verifies an endpoint owner‘s intent to receive messages by validating the token sent to the endpoint by an
    * earlier `Subscribe` action.  If the token is valid, the action creates a new subscription and return its Amazon
    * Resource Name (ARN).
    *
    * @param topicArn the ARN of the topic for which to confirm a subscription
    * @param token a short-lived token sent to an endpoint during the `Subscribe` action
    * @return the ARN of the new subscription
    */
  def confirmSubscription(topicArn: String, token: String)(implicit m: Materializer): Future[String]

  /** Verifies an endpoint owner‘s intent to receive messages by validating the token sent to the endpoint by an
    * earlier `Subscribe` action.  If the token is valid, the action creates a new subscription and return its Amazon
    * Resource Name (ARN).
    *
    * @param topicArn the ARN of the topic for which to confirm a subscription
    * @param token a short-lived token sent to an endpoint during the `Subscribe` action
    * @param authenticateOnUnsubscribe if set, disallows unauthenticated unsubscribes of the subscription
    * @return the ARN of the new subscription
    */
  def confirmSubscription(topicArn: String, token: String, authenticateOnUnsubscribe: Boolean)
                         (implicit m: Materializer): Future[String]

  /** Return a specific property of a subscription.
    *
    * @param subscriptionArn the ARN of the subscription whose property to retrieve
    * @param name the name of the property to retrieve
    * @return the value of the property, if available
    */
  def getSubscriptionAttribute(subscriptionArn: String, name: String)(implicit m: Materializer): Future[Option[String]]

  /** Return all of the properties of a subscription. */
  def getSubscriptionAttributes(subscriptionArn: String)(implicit m: Materializer): Future[Map[String,String]]

  /** Sets an attribute of a subscription to a new value.
    *
    * @param subscriptionArn the ARN of the subscription to modify
    * @param attributeName the name of the attribute to set
    * @param attributeValue the new value for the attribute
    */
  def setSubscriptionAttribute(subscriptionArn: String, attributeName: String, attributeValue: String)
                              (implicit m: Materializer): Future[Done]

  /** Sets an attribute of a subscription to a new value.
    *
    * @param subscriptionArn the ARN of the subscription to modify
    * @param attributeName the name of the attribute to set
    * @param attributeValue the new value for the attribute (if any)
    */
  def setSubscriptionAttribute(subscriptionArn: String, attributeName: String, attributeValue: Option[String])
                              (implicit m: Materializer): Future[Done]

  /** Deletes a subscription. */
  def unsubscribe(subscriptionArn: String)(implicit m: Materializer): Future[Done]

  /** Creates a platform application for one of the supported push notification services, such as APNS and GCM, to
    * which devices and mobile apps may register.
    *
    * @param name the application name, which must be made up of only uppercase and lowercase ASCII letters, number,
    *             underscores, hyphens, and periods, and must be between 1 and 256 characters long
    * @param platform the platform for which to create an application, must be one of `ADM`, `APNS`, `APNS_SANDBOX`,
    *                 `BAIDU`, `GCM`, `MPNS` or `WNS`
    * @param principal the principal from the notification service.  For ADM this is ''client id'', for APNs this is
    *                  a PEM-encoded ''SSL certificate'', for Baidu this is an ''API key'', for CCM this is an empty
    *                  string, for MPNS this is a PEM-encoded ''TLS certificate'', and for WNS this is a ''Package
    *                  Security Identifier''.
    * @param credential the credential from the notification service.  For ADM this is a ''client secret'', for APNs
    *                   this is a PEM-encoded ''private key'', for Baidu this is a ''secret key'', for GCM this is an
    *                   ''API key'', for MPNS this is a PEM-encoded ''private key'', and for WNS this is a ''secret
    *                   key''.
    * @return the ARN of the newly-created platform application
    * @see [[http://docs.aws.amazon.com/sns/latest/dg/SNSMobilePush.html Using SNS Mobile Push Notifications]]
    */
  def createPlatformApplication(name: String, platform: String, principal: String, credential: String)
                               (implicit m: Materializer): Future[String]

  /** Creates a platform application for one of the supported push notification services, such as APNS and GCM, to
    * which devices and mobile apps may register.
    *
    * @param name the application name, which must be made up of only uppercase and lowercase ASCII letters, number,
    *             underscores, hyphens, and periods, and must be between 1 and 256 characters long
    * @param platform the platform for which to create an application, must be one of `ADM`, `APNS`, `APNS_SANDBOX`,
    *                 `BAIDU`, `GCM`, `MPNS` or `WNS`
    * @param principal the principal from the notification service.  For ADM this is ''client id'', for APNs this is
    *                  a PEM-encoded ''SSL certificate'', for Baidu this is an ''API key'', for CCM this is an empty
    *                  string, for MPNS this is a PEM-encoded ''TLS certificate'', and for WNS this is a ''Package
    *                  Security Identifier''.
    * @param credential the credential from the notification service.  For ADM this is a ''client secret'', for APNs
    *                   this is a PEM-encoded ''private key'', for Baidu this is a ''secret key'', for GCM this is an
    *                   ''API key'', for MPNS this is a PEM-encoded ''private key'', and for WNS this is a ''secret
    *                   key''.
    * @param attributes additional attributes to set on the platform application
    * @return the ARN of the newly-created platform application
    * @see [[http://docs.aws.amazon.com/sns/latest/dg/SNSMobilePush.html Using SNS Mobile Push Notifications]]
    */
  def createPlatformApplication(name: String, platform: String, principal: String, credential: String,
                                attributes: Map[String,String])
                               (implicit m: Materializer): Future[String]

  /** Retrieves the attributes of the platform application object.
    *
    * @param arn the ARN of the platform application
    * @return a map containing all of the platform application object’s attributes
    */
  def getPlatformApplicationAttributes(arn: String)(implicit m: Materializer): Future[Map[String,String]]

  /** Retrieves a specific attribute of the platform application object.
    *
    * @param arn the ARN of the platform application
    * @param attributeName the name of the attribute to retrieve
    * @return the attribute value, if any
    */
  def getPlatformApplicationAttribute(arn: String, attributeName: String)
                                     (implicit m: Materializer): Future[Option[String]]

  /** Sets the attribute of a platform application to the given value.
    *
    * @param arn the ARN of the platform application
    * @param attributeName the name of the attribute to modify
    * @param attributeValue the new value for the attribute, note that a null value will be set to the empty string
    */
  def setPlatformApplicationAttribute(arn: String, attributeName: String, attributeValue: String)
                                     (implicit m: Materializer): Future[Done]

  /** Resets the attribute of a platform application to the given value.
    *
    * @param arn the ARN of the platform application
    * @param attributeName the name of the attribute to modify
    * @param attributeValue the new value for the attribute.  If `None`, this results in setting to the empty string.
    */
  def setPlatformApplicationAttribute(arn: String, attributeName: String, attributeValue: Option[String])
                                     (implicit m: Materializer): Future[Done]

  /** Sets the attributes of the platform application object.
    *
    * @param arn the ARN of the platform application
    * @param attributes a map containing updated values for the platform application.  No additional processing is
    *                   performed on the values.
    */
  def setPlatformApplicationAttributes(arn: String, attributes: Map[String,String])
                                      (implicit m: Materializer): Future[Done]

  /** Deletes a platform application object for one of the supported push notification services.
    *
    * @param arn the ARN of the platform application to delete
    */
  def deletePlatformApplication(arn: String)(implicit m: Materializer): Future[Done]

  /** Lists all of the platform applications. */
  def listPlatformApplications()(implicit m: Materializer): Future[Seq[PlatformApplication]]

  /** Creates an endpoint for a device and mobile app on one of the supported push notification services, such as GCM
    * and APNS.  Note that for Baidu it is necessary to include the `ChannelID` and `UserID` attributes using a method
    * that accepts additional attributes.
    *
    * @param platformApplicationArn a ARN for a platform application
    * @param token a unique identifier created by the notification service for an app on a device.  For ADM and GCM
    *              this is a ''registration ID'', for APNs this is a ''device token'', for Baidu this is a
    *              ''platform token'' (which must also be included in the attributes), for MPNS and WNS this is a
    *              ''push notification URI''
    * @return the ARN of the newly created endpoint
    */
  def createPlatformEndpoint(platformApplicationArn: String, token: String)(implicit m: Materializer): Future[String]

  /** Creates an endpoint for a device and mobile app on one of the supported push notification services, such as GCM
    * and APNS.  Note that for Baidu it is necessary to include the `ChannelID` and `UserID` attributes using a method
    * that accepts additional attributes.
    *
    * @param platformApplicationArn a ARN for a platform application
    * @param token a unique identifier created by the notification service for an app on a device.  For ADM and GCM
    *              this is a ''registration ID'', for APNs this is a ''device token'', for Baidu this is a
    *              ''platform token'' (which must also be included in the attributes), for MPNS and WNS this is a
    *              ''push notification URI''
    * @param customUserData arbitrary user data to associate with the endpoint
    * @return the ARN of the newly created endpoint
    */
  def createPlatformEndpoint(platformApplicationArn: String, token: String, customUserData: String)
                            (implicit m: Materializer): Future[String]

  /** Creates an endpoint for a device and mobile app on one of the supported push notification services, such as GCM
    * and APNS.  Note that for Baidu it is necessary to include the `ChannelID` and `UserID` attributes.
    *
    * @param platformApplicationArn a ARN for a platform application
    * @param token a unique identifier created by the notification service for an app on a device.  For ADM and GCM
    *              this is a ''registration ID'', for APNs this is a ''device token'', for Baidu this is a
    *              ''platform token'' (which must also be included in the attributes), for MPNS and WNS this is a
    *              ''push notification URI''
    * @param attributes additional attributes to include in the request
    * @return the ARN of the newly created endpoint
    */
  def createPlatformEndpoint(platformApplicationArn: String, token: String, attributes: Map[String,String])
                            (implicit m: Materializer): Future[String]

  /** Creates an endpoint for a device and mobile app on one of the supported push notification services, such as GCM
    * and APNS.  Note that for Baidu it is necessary to include the `ChannelID` and `UserID` attributes.
    *
    * @param platformApplicationArn a ARN for a platform application
    * @param token a unique identifier created by the notification service for an app on a device.  For ADM and GCM
    *              this is a ''registration ID'', for APNs this is a ''device token'', for Baidu this is a
    *              ''platform token'' (which must also be included in the attributes), for MPNS and WNS this is a
    *              ''push notification URI''
    * @param customUserData arbitrary user data to associate with the endpoint
    * @param attributes additional attributes to include in the request
    * @return the ARN of the newly created endpoint
    */
  def createPlatformEndpoint(platformApplicationArn: String, token: String, customUserData: String,
                             attributes: Map[String,String])
                            (implicit m: Materializer): Future[String]

  /** Retrieve an endpoint attributes for a device on one of the supported push notification services.
    *
    * @param platformEndpointArn the ARN of the platform endpoint
    * @param attributeName the name of the attribute to retrieve
    * @return the value for the given attribute, if any
    */
  def getPlatformEndpointAttribute(platformEndpointArn: String, attributeName: String)
                                  (implicit m: Materializer): Future[Option[String]]

  /** Retrieves the endpoint attributes for a device on one of the supported push notification services.
    *
    * @param platformEndpointArn the ARN of the platform endpoint
    * @return a map containing the platform endpoint attributes
    */
  def getPlatformEndpointAttributes(platformEndpointArn: String)(implicit m: Materializer): Future[Map[String,String]]

  /** Sets tan attribute for an endpoint for a device on one of the supported push notification services.
    *
    * @param platformEndpointArn the ARN of the platform endpoint to update
    * @param attributeName the name of the attribute to empty
    * @param attributeValue the new value for the attribute
    */
  def setPlatformEndpointAttributes(platformEndpointArn: String, attributeName: String, attributeValue: String)
                                   (implicit m: Materializer): Future[Done]

  /** Sets tan attribute for an endpoint for a device on one of the supported push notification services.
    *
    * @param platformEndpointArn the ARN of the platform endpoint to update
    * @param attributeName the name of the attribute to empty
    * @param attributeValue the new value for the attribute.  `None` values will be converted to empty strings.
    */
  def setPlatformEndpointAttributes(platformEndpointArn: String, attributeName: String, attributeValue: Option[String])
                                   (implicit m: Materializer): Future[Done]

  /** Sets the attributes for an endpoint for a device on one of the supported push notification services.
    *
    * @param platformEndpointArn the ARN of the platform endpoint to update
    * @param attributes a map of the endpoint attributes
    */
  def setPlatformEndpointAttributes(platformEndpointArn: String, attributes: Map[String,String])
                                   (implicit m: Materializer): Future[Done]

  /** Deletes the endpoint for a device and mobile app from Amazon SNS.
    *
    * @param platformEndpointArn the ARN of the platform endpoint to delete
    */
  def deletePlatformEndpoint(platformEndpointArn: String)(implicit m: Materializer): Future[Done]

  /** Lists endpoints and endpoint attributes for devices in a supported push notification service.
    *
    * @param platformApplicationArn the platform application for which to list endpoints
    */
  def listPlatformEndpoints(platformApplicationArn: String)(implicit m: Materializer): Future[Seq[PlatformEndpoint]]

  /** Sends a message to a mobile endpoint or to all of a topic’s subscribed endpoints.
    *
    * If you need to send different messages for each transport protocol or a custom JSON payload, use one of the
    * versions of `publish` that take a map for the message.
    *
    * @param targetArn either a topic ARN or a platform endpoint ARN
    * @param message the message to send
    * @return the unique identifier of the published message
    */
  def publish(targetArn: String, message: String)(implicit m: Materializer): Future[String]

  /** Sends a message to a mobile endpoint or to all of a topic’s subscribed endpoints.
    *
    * If you need to send different messages for each transport protocol or a custom JSON payload, use one of the
    * versions of `publish` that take a map for the message.
    *
    * @param targetArn either a topic ARN or a platform endpoint ARN
    * @param message the message to send
    * @param subject a string to be used as the ''Subject'' line when the message is delivered to e-mail endpoints
    * @return the unique identifier of the published message
    */
  def publish(targetArn: String, message: String, subject: String)(implicit m: Materializer): Future[String]

  /** Sends a message to a mobile endpoint or to all of a topic’s subscribed endpoints.
    *
    * If you need to send different messages for each transport protocol or a custom JSON payload, use one of the
    * versions of `publish` that take a map for the message.
    *
    * @param targetArn either a topic ARN or a platform endpoint ARN
    * @param message the message to send
    * @param attributes a map of additional attributes that should be attached to the message
    * @return the unique identifier of the published message
    */
  def publish(targetArn: String, message: String, attributes: Map[String,MessageAttributeValue])
             (implicit m: Materializer): Future[String]

  /** Sends a message to a mobile endpoint or to all of a topic’s subscribed endpoints.
    *
    * If you need to send different messages for each transport protocol or a custom JSON payload, use one of the
    * versions of `publish` that take a map for the message.
    *
    * @param targetArn either a topic ARN or a platform endpoint ARN
    * @param message the message to send
    * @param subject a string to be used as the ''Subject'' line when the message is delivered to e-mail endpoints
    * @param attributes a map of additional attributes that should be attached to the message
    * @return the unique identifier of the published message
    */
  def publish(targetArn: String, message: String, subject: String, attributes: Map[String,MessageAttributeValue])
             (implicit m: Materializer): Future[String]

  /** Sends a message to a mobile endpoint or to all of a topic’s subscribed endpoints.  This variation allows sending
    * different messages to different transport protocols or to be able to send JSON to a mobile endpoint.
    *
    * @param targetArn either a topic ARN or a platform endpoint ARN
    * @param message the message to send, where the keys are the different transport protocols, i.e. `email`, `http`,
    *                `APNS_SANDBOX`, or `GCM`, and the values are strings that will be JSON-escaped
    * @return the unique identifier of the published message
    */
  def publish(targetArn: String, message: Map[String,String])(implicit m: Materializer): Future[String]

  /** Sends a message to a mobile endpoint or to all of a topic’s subscribed endpoints.  This variation allows sending
    * different messages to different transport protocols or to be able to send JSON to a mobile endpoint.
    *
    * @param targetArn either a topic ARN or a platform endpoint ARN
    * @param message the message to send, where the keys are the different transport protocols, i.e. `email`, `http`,
    *                `APNS_SANDBOX`, or `GCM`, and the values are strings that will be JSON-escaped
    * @param subject a string to be used as the ''Subject'' line when the message is delivered to e-mail endpoints
    * @return the unique identifier of the published message
    */
  def publish(targetArn: String, message: Map[String,String], subject: String)
             (implicit m: Materializer): Future[String]

  /** Sends a message to a mobile endpoint or to all of a topic’s subscribed endpoints.  This variation allows sending
    * different messages to different transport protocols or to be able to send JSON to a mobile endpoint.
    *
    * @param targetArn either a topic ARN or a platform endpoint ARN
    * @param message the message to send, where the keys are the different transport protocols, i.e. `email`, `http`,
    *                `APNS_SANDBOX`, or `GCM`, and the values are strings that will be JSON-escaped
    * @param attributes a map of additional attributes that should be attached to the message
    * @return the unique identifier of the published message
    */
  def publish(targetArn: String, message: Map[String,String], attributes: Map[String,MessageAttributeValue])
             (implicit m: Materializer): Future[String]

  /** Sends a message to a mobile endpoint or to all of a topic’s subscribed endpoints.  This variation allows sending
    * different messages to different transport protocols or to be able to send JSON to a mobile endpoint.
    *
    * @param targetArn either a topic ARN or a platform endpoint ARN
    * @param message the message to send, where the keys are the different transport protocols, i.e. `email`, `http`,
    *                `APNS_SANDBOX`, or `GCM`, and the values are strings that will be JSON-escaped
    * @param subject a string to be used as the ''Subject'' line when the message is delivered to e-mail endpoints
    * @param attributes a map of additional attributes that should be attached to the message
    * @return the unique identifier of the published message
    */
  def publish(targetArn: String, message: Map[String,String], subject: String,
              attributes: Map[String,MessageAttributeValue])
             (implicit m: Materializer): Future[String]
}
