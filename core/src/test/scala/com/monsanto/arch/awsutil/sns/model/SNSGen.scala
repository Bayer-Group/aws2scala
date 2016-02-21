package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.AwsGen
import com.monsanto.arch.awsutil.auth.policy.action.SNSAction
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import spray.json._

/** Scalacheck generators for SNS model objects. */
object SNSGen {
  val boolean: Gen[Boolean] = Gen.oneOf(true, false)

  val byteArray = Gen.listOf(Gen.choose(Byte.MinValue, Byte.MaxValue)).map(_.toArray)

  val nonEmptyString: Gen[String] = Gen.alphaStr.suchThat(_.nonEmpty)

  val topicName: Gen[String] = {
    val topicNameChar = Gen.frequency(52 → Gen.alphaChar, 10 → Gen.numChar, 2 → Gen.oneOf('_', '-'))
    for {
      length ← Gen.choose(1, 256)
      chars ← Gen.listOfN(length, topicNameChar)
    } yield chars.mkString
  }

  private val Regions = Set("us-east-1", "us-west-2", "us-west-1", "eu-west-1", "eu-central-1", "ap-southeast-1",
    "ap-northeast-1", "ap-southeast-2", "ap-northeast-2", "sa-east-1")

  val region: Gen[String] = Gen.oneOf(Regions.toSeq)

  val policy: Gen[String] = for(text ← Gen.alphaStr) yield JsObject("text" → JsString(text)).compactPrint

  private val retryPolicy: Gen[JsObject] =
    for {
      numRetries ← Gen.choose(0,100)
      numNoDelayRetries ← Gen.choose(0,numRetries)
      minDelayTarget ← Gen.choose(0,120)
      numMinDelayRetries ← Gen.choose(0,numRetries)
      maxDelayTarget ← Gen.choose(minDelayTarget,3600)
      numMaxDelayRetries ← Gen.choose(0,numRetries)
      backoffFunction ← Gen.oneOf("linear", "arithmetic", "geometric", "exponential")
    } yield JsObject(
      "minDelayTarget" → JsNumber(minDelayTarget),
      "maxDelayTarget" → JsNumber(maxDelayTarget),
      "numRetries" → JsNumber(numRetries),
      "numNoDelayRetries" → JsNumber(numNoDelayRetries),
      "numMinDelayRetries" → JsNumber(numMinDelayRetries),
      "numMaxDelayRetries" → JsNumber(numMaxDelayRetries),
      "backoffFunction" → JsString(backoffFunction))

  private val throttlePolicy: Gen[Option[JsObject]] =
    for (maxReceivesPerSecond ← Gen.oneOf(Gen.const(None), Gen.choose(1,10).map(Some.apply))) yield {
      maxReceivesPerSecond.map(n ⇒ JsObject("maxReceivesPerSecond" → JsNumber(n)))
    }

  val topicDeliveryPolicy: Gen[String] =
    for {
      disableSubscriptionOverrides ← boolean
      maxReceivesPerSecond ← Gen.oneOf(Gen.const(None), Gen.choose(1,10).map(Some.apply))
      defaultHealthyRetryPolicy ← retryPolicy
      defaultThrottlePolicy ← throttlePolicy
    } yield {
      val httpPolicy =
        defaultThrottlePolicy
          .map(p ⇒ Map("defaultThrottlePolicy" → p))
          .getOrElse(Map.empty) ++ Map(
          "defaultHealthyRetryPolicy" → defaultHealthyRetryPolicy,
          "disableSubscriptionOverrides" → JsBoolean(disableSubscriptionOverrides)
        )
      JsObject("http" → JsObject(httpPolicy)).compactPrint
    }

  val maybeTopicDeliveryPolicy: Gen[Option[String]] =
    Gen.oneOf(Gen.const(None), topicDeliveryPolicy.map(Some.apply))

  val subscriptionDeliveryPolicy: Gen[String] =
    for {
      healthyRetryPolicy ← retryPolicy
      throttlePolicy ← throttlePolicy
    } yield
      JsObject(
        "healthyRetryPolicy" → healthyRetryPolicy,
        "throttlePolicy" → throttlePolicy.getOrElse(JsNull)
      ).compactPrint

  val maybeSubscriptionDeliveryPolicy: Gen[Option[String]] =
    Gen.oneOf(Gen.const(None), topicDeliveryPolicy.map(Some.apply))

  val topicArnObj: Gen[TopicArn] =
    for {
      region ← region
      accountId ← arbitrary[AwsGen.Account].map(_.value)
      name ← topicName
    } yield TopicArn(region, accountId, name)

  val topicArn: Gen[String] = topicArnObj.map(_.toString)

  def topicArn(region: String, owner: String) =
    for(name ← topicName) yield TopicArn(region, owner, name).toString

  val snsAction: Gen[SNSAction] = Gen.oneOf(SNSAction.values)

  val topic: Gen[Topic] =
    for {
      topicArn ← topicArnObj
      displayName ← Gen.alphaStr
      subscriptionsPending ← Gen.choose(0, 10)
      subscriptionsConfirmed ← Gen.choose(0, 100)
      subscriptionsDeleted ← Gen.choose(0, 50)
      policy ← SNSGen.policy
      deliveryPolicy ← SNSGen.maybeTopicDeliveryPolicy
      effectiveDeliverPolicy ← SNSGen.policy
    } yield {
      val attributes =
        deliveryPolicy.map(p ⇒ Map("DeliveryPolicy" → p)).getOrElse(Map.empty) ++
          Map(
            "TopicArn" → topicArn.toString,
            "DisplayName" → displayName,
            "Owner" → topicArn.owner,
            "Policy" → policy,
            "SubscriptionsPending" → subscriptionsPending.toString,
            "SubscriptionsConfirmed" → subscriptionsConfirmed.toString,
            "SubscriptionsDeleted" → subscriptionsDeleted.toString,
            "EffectiveDeliveryPolicy" → effectiveDeliverPolicy
          )
      Topic(attributes)
    }

  val protocol: Gen[Protocol] = Gen.oneOf(Protocol.Values)

  val applicationName: Gen[String] = {
    val appNameChar = Gen.frequency(52 → Gen.alphaChar, 10 → Gen.numChar, 3 → Gen.oneOf('_', '-', '.'))
    for {
      n ← Gen.choose(1, 256)
      chars ← Gen.listOfN(n, appNameChar)
    } yield chars.mkString
  }

  private def exampleUrl(protocol: String): Gen[String] =
    for (pathComponents ← Gen.listOf(nonEmptyString))
      yield s"$protocol://example.com/${pathComponents.mkString("/")}"

  val httpEndpoint: Gen[SubscriptionEndpoint.HttpEndpoint] =
    for(url ← exampleUrl("http")) yield Protocol.Http(url)

  val httpsEndpoint: Gen[SubscriptionEndpoint.HttpsEndpoint] =
    for(url ← exampleUrl("https")) yield Protocol.Https(url)

  private val exampleEmail: Gen[String] =
    for (who ← nonEmptyString) yield s"$who@example.com"

  val emailEndpoint: Gen[SubscriptionEndpoint.EmailEndpoint] =
    for(email ← exampleEmail) yield Protocol.Email(email)

  val emailJsonEndpoint: Gen[SubscriptionEndpoint.EmailJsonEndpoint] =
    for(email ← exampleEmail) yield Protocol.EmailJson(email)

  val smsEndpoint: Gen[SubscriptionEndpoint.SMSEndpoint] =
    for (digits ← Gen.listOfN(7, Gen.numChar).map(_.mkString))
      yield Protocol.SMS(s"1555$digits")

  private def withGeneratedRegionAndOwner[T](gen: (String, String) ⇒ Gen[T]) =
    for {
      region ← region
      owner ← arbitrary[AwsGen.Account].map(_.value)
      t ← gen(region, owner)
    } yield t

  def sqsEndpoint(region: String, owner: String): Gen[SubscriptionEndpoint.SQSEndpoint] =
    for (name ← nonEmptyString) yield Protocol.SQS(s"arn:aws:sqs:$region:$owner:$name")

  val sqsEndpoint: Gen[SubscriptionEndpoint.SQSEndpoint] =
    withGeneratedRegionAndOwner(sqsEndpoint(_: String, _: String))

  val platform: Gen[Platform] = Gen.oneOf(Platform.Values)

  private val admCredentials: Gen[PlatformApplicationCredentials] =
    for {
      clientId ← nonEmptyString
      clientSecret ← nonEmptyString
    } yield Platform.ADM(clientId, clientSecret)

  private val apnsDevCredentials: Gen[PlatformApplicationCredentials] =
    for {
      certificate ← nonEmptyString
      key ← nonEmptyString
    } yield Platform.APNSDevelopment(certificate, key)

  private val apnsProdCredentials: Gen[PlatformApplicationCredentials] =
    for {
      certificate ← nonEmptyString
      key ← nonEmptyString
    } yield Platform.APNSDevelopment(certificate, key)

  private val baiduCredentials: Gen[PlatformApplicationCredentials] =
    for {
      apiKey ← nonEmptyString
      secretKey ← nonEmptyString
    } yield Platform.Baidu(apiKey, secretKey)

  private val gcmCredentials: Gen[PlatformApplicationCredentials] =
    for (serverApiKey ← nonEmptyString) yield Platform.GCM(serverApiKey)

  private val mpnsCredentials: Gen[PlatformApplicationCredentials] = {
    val unauthenticatedGen = Gen.const(Platform.MPNS())
    val authenticatedGen = for {
      tlsCertificateChain ← nonEmptyString
      privateKey ← nonEmptyString
    } yield Platform.MPNS(tlsCertificateChain, privateKey)

    Gen.frequency(4 → authenticatedGen, 1 → unauthenticatedGen)
  }

  private val wnsCredentials: Gen[PlatformApplicationCredentials] =
    for {
      packageSecurityIdentifier ← nonEmptyString
      secretKey ← nonEmptyString
    } yield Platform.WNS(packageSecurityIdentifier, secretKey)

  val platformApplicationCredentials: Gen[PlatformApplicationCredentials] =
    Gen.oneOf(admCredentials, apnsDevCredentials, apnsProdCredentials, baiduCredentials, gcmCredentials,
      mpnsCredentials, wnsCredentials)

  def platformApplicationArn(region: String, owner: String, platform: Platform, name: String): String =
    s"arn:aws:sns:$region:$owner:app/${platform.name}/$name"

  def platformApplicationArn(region: String, owner: String): Gen[String] =
    for {
      platform ← platform
      name ← applicationName
    } yield platformApplicationArn(region, owner, platform, name)

  def platformApplicationArn(platform: Platform, name: String): Gen[String] =
    withGeneratedRegionAndOwner(platformApplicationArn(_: String, _: String, platform, name))

  val platformApplicationArn: Gen[String] =
    withGeneratedRegionAndOwner(platformApplicationArn(_: String, _: String))

  def applicationEndpoint(region: String, owner: String): Gen[SubscriptionEndpoint.ApplicationEndpoint] =
    for(arn ← platformApplicationArn(region, owner)) yield Protocol.Application(arn)

  val applicationEndpoint: Gen[SubscriptionEndpoint.ApplicationEndpoint] =
    withGeneratedRegionAndOwner(applicationEndpoint(_: String, _: String))

  private def lambdaEndpoint(region: String, owner: String): Gen[SubscriptionEndpoint.LambdaEndpoint] =
    for (name ← nonEmptyString) yield Protocol.Lambda(s"arn:aws:lambda:$region:$owner:function:$name")

  val lambdaEndpoint: Gen[SubscriptionEndpoint.LambdaEndpoint] =
    withGeneratedRegionAndOwner(lambdaEndpoint(_: String, _: String))

  case class TopicArn(region: String, owner: String, name: String) {
    override def toString = s"arn:aws:sns:$region:$owner:$name"
  }
  object TopicArn {
    val TopicArnRegex = "^arn:aws:sns:([^:]+):([^:]+):([^:]+)$".r
    def apply(arn: String): TopicArn = arn match {
      case TopicArnRegex(r, o, n) ⇒ TopicArn(r, o, n)
    }
  }

  def subscriptionEndpoint(region: String, owner: String): Gen[SubscriptionEndpoint] =
    Gen.oneOf(
      httpEndpoint,
      httpsEndpoint,
      emailEndpoint,
      emailJsonEndpoint,
      smsEndpoint,
      sqsEndpoint(region, owner),
      applicationEndpoint(region, owner),
      lambdaEndpoint(region, owner))

  def subscriptionEndpoint(topicArn: String): Gen[SubscriptionEndpoint] = {
    val TopicArn(region, owner, _) = TopicArn(topicArn)
    subscriptionEndpoint(region, owner)
  }

  val subscriptionEndpoint: Gen[SubscriptionEndpoint] =
    withGeneratedRegionAndOwner(subscriptionEndpoint(_: String, _: String))

  def subscriptionEndpoint(topicArn: TopicArn): Gen[SubscriptionEndpoint] =
    subscriptionEndpoint(topicArn.region, topicArn.owner)

  val hexDigit: Gen[Char] = Gen.oneOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

  val confirmationToken: Gen[String] = for(digits ← Gen.listOfN(64, hexDigit)) yield digits.mkString

  val subscription: Gen[Subscription] =
    for {
      topicArn ← topicArn
      subscription ← subscription(topicArn)
    } yield subscription

  def subscription(topic: Topic): Gen[Subscription] = subscription(topic.arn)

  def subscriptionFrom(summary: SubscriptionSummary): Gen[Option[Subscription]] =
    if (summary.isPending) {
      Gen.const(None)
    } else {
      for {
        rawMessageDelivery ← boolean
        confirmationWasAuthenticated ← boolean
        maybeDeliveryPolicy ← maybeSubscriptionDeliveryPolicy
      } yield {
        val baseAttributes =
          Map(
            "SubscriptionArn" → summary.arn.get,
            "TopicArn" → summary.topicArn,
            "Protocol" → summary.endpoint.protocol.toAws,
            "Endpoint" → summary.endpoint.endpoint,
            "Owner" → summary.owner,
            "RawMessageDelivery" → rawMessageDelivery.toString,
            "ConfirmationWasAuthenticated" → confirmationWasAuthenticated.toString
          )
        val policyAttributes =
          maybeDeliveryPolicy.map(p ⇒ Map("DeliveryPolicy" → p, "EffectiveDeliveryPolicy" → p)).getOrElse(Map.empty)

        Some(Subscription(baseAttributes ++ policyAttributes))
      }
    }

  def subscription(topicArn: String): Gen[Subscription] =
    for {
      arn ← subscriptionArn(topicArn)
      rawMessageDelivery ← boolean
      SubscriptionEndpoint(protocol, endpoint) ← subscriptionEndpoint(topicArn)
      confirmationWasAuthenticated ← boolean
      maybeDeliveryPolicy ← maybeSubscriptionDeliveryPolicy
    } yield {
      val baseAttributes =
        Map(
          "SubscriptionArn" → arn,
          "TopicArn" → topicArn.toString,
          "Protocol" → protocol.toAws,
          "Endpoint" → endpoint,
          "Owner" → TopicArn(topicArn).owner,
          "RawMessageDelivery" → rawMessageDelivery.toString,
          "ConfirmationWasAuthenticated" → confirmationWasAuthenticated.toString
        )
      val policyAttributes =
        maybeDeliveryPolicy.map(p ⇒ Map("DeliveryPolicy" → p, "EffectiveDeliveryPolicy" → p)).getOrElse(Map.empty)

      Subscription(baseAttributes ++ policyAttributes)
    }

  val topicWithSubscription: Gen[(Topic,Subscription)] =
    for {
      topic ← topic
      subscription ← subscription(topic)
    } yield (topic, subscription)

  val subscriptionArn: Gen[String] =
    for {
      topicArn ← topicArn
      subscriptionArn ← subscriptionArn(topicArn)
    } yield subscriptionArn

  def subscriptionArn(topicArn: String): Gen[String] = Gen.uuid.map(uuid ⇒ s"$topicArn:$uuid")

  val subscriptionSummary: Gen[SubscriptionSummary] =
    for {
      topicArn ← topicArn
      summary ← subscriptionSummary(topicArn)
    } yield summary

  def subscriptionSummary(topicArn: String): Gen[SubscriptionSummary] =
    for {
      subscriptionArn ← Gen.frequency(3 → subscriptionArn(topicArn).map(Some.apply), 1 → Gen.const(None))
      endpoint ← subscriptionEndpoint(topicArn)
    } yield SubscriptionSummary(subscriptionArn, topicArn, endpoint, TopicArn(topicArn).owner)

  val platformApplication: Gen[PlatformApplication] =
    for {
      name ← applicationName
      credentials ← platformApplicationCredentials
      platformApplication ← platformApplication(name, credentials)
    } yield platformApplication

  def platformApplication(name: String, credentials: PlatformApplicationCredentials): Gen[PlatformApplication] =
    for {
      region ← region
      owner ← arbitrary[AwsGen.Account].map(_.value)
      enabled ← boolean
      eventEndpointCreated ← Gen.option(topicArn(region, owner))
      eventEndpointDeleted ← Gen.option(topicArn(region, owner))
      eventEndpointUpdated ← Gen.option(topicArn(region, owner))
      eventDeliveryFailure ← Gen.option(topicArn(region, owner))
      failureFeedbackRole ← Gen.option(Gen.const(s"arn:aws:iam::$owner:role/SNSFailureFeedback"))
      successFeedbackRoleAndSampleRate ← Gen.option(Gen.zip(Gen.const(s"arn:aws:iam::$owner:role/SNSSuccessFeedback"), Gen.choose(0, 100)))
    } yield {
      val arn = platformApplicationArn(region, owner, credentials.platform, name)
      val attributes = Map(
        "Enabled" → Some(enabled.toString),
        "EventEndpointCreated" → eventEndpointCreated,
        "EventEndpointDeleted" → eventEndpointDeleted,
        "EventEndpointUpdated" → eventEndpointUpdated,
        "EventDeliveryFailure" → eventDeliveryFailure,
        "SuccessFeedbackRoleArn" → successFeedbackRoleAndSampleRate.map(_._1),
        "SuccessFeedbackSampleRate" → successFeedbackRoleAndSampleRate.map(_._2.toString),
        "FailureFeedbackRoleArn" → failureFeedbackRole
      ).filter(_._2.isDefined).mapValues(_.get)
      PlatformApplication(arn, attributes)
    }

  val platformEndpointArn: Gen[String] =
    for {
      applicationArn ← platformApplicationArn
      endpointArn ← platformEndpointArn(applicationArn)
    } yield endpointArn

  def platformEndpointArn(platformApplicationArn: String): Gen[String] =
    for (id ← Gen.uuid) yield s"$platformApplicationArn/$id".replaceFirst(":app/",":endpoint/")

  val platformEndpointAttributes: Gen[Map[String,String]] =
    for {
      token ← SNSGen.nonEmptyString
      enabled ← SNSGen.boolean
      customUserData ← Gen.option(Gen.alphaStr)
    } yield {
      customUserData.map(x ⇒ Map("CustomUserData" → x)).getOrElse(Map.empty) ++
        Map("Token" → token, "Enabled" → enabled.toString)
    }

  val platformEndpoint: Gen[PlatformEndpoint] =
    for {
      arn ← platformEndpointArn
      attributes ← platformEndpointAttributes
    } yield PlatformEndpoint(arn, attributes)

  def platformEndpoint(platformApplicationArn: String): Gen[PlatformEndpoint] =
    for {
      arn ← platformEndpointArn(platformApplicationArn)
      attributes ← platformEndpointAttributes
    } yield PlatformEndpoint(arn, attributes)

  val messageAttributeValue: Gen[MessageAttributeValue[_]] = {
    val stringValue = for(value ← nonEmptyString) yield MessageAttributeValue(value)
    val byteArrayValue = for(value ← byteArray) yield MessageAttributeValue(value)

    Gen.oneOf(stringValue, byteArrayValue)
  }

  val messageAttributes: Gen[Map[String,MessageAttributeValue[_]]] =
    for {
      size ← Gen.choose(0, 10)
      attributes ← Gen.mapOfN(size, Gen.zip(nonEmptyString, messageAttributeValue))
    } yield attributes

  val jsonMessagePayload: Gen[String] =
    Gen.mapOf(Gen.zip(SNSGen.nonEmptyString, SNSGen.nonEmptyString.map(JsString(_)))).map(JsObject(_).compactPrint)

  val messageMap: Gen[Map[String,String]] = {
    val protocol = Gen.oneOf("default", "http", "https", "email", "email-json", "sms", "sqs", "lambda",
      "ADM", "APNS", "APNS_SANDBOX", "BAIDU", "GCM", "MPNS", "WNS")
    val message = Gen.oneOf(jsonMessagePayload, SNSGen.nonEmptyString)
    for {
      size ← Gen.choose(0,4)
      messageMap ← Gen.mapOfN(size, Gen.zip(protocol, message))
    } yield messageMap
  }

  val messageId: Gen[String] = Gen.uuid.map(_.toString)
}
