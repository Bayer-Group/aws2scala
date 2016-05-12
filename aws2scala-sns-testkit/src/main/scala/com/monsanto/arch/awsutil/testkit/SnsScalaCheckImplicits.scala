package com.monsanto.arch.awsutil.testkit

import com.monsanto.arch.awsutil.auth.policy.action.SNSAction
import com.monsanto.arch.awsutil.identitymanagement.model.RoleArn
import com.monsanto.arch.awsutil.sns.SNS
import com.monsanto.arch.awsutil.sns.model.{SNS ⇒ _, _}
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.{Account, Arn}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen, Shrink}

import scala.util.Try

object SnsScalaCheckImplicits {
  SNS.init()

  implicit lazy val arbAddPermissionRequest: Arbitrary[AddPermissionRequest] =
    Arbitrary {
      for {
        topicArn ← arbitrary[TopicArn]
        label ← CoreGen.statementId
        accounts ← UtilGen.nonEmptyListOfSqrtN(arbitrary[Account]).map(_.map(_.id))
        actions ← UtilGen.nonEmptyListOfSqrtN(arbitrary[SNSAction])
      } yield AddPermissionRequest(topicArn.arnString, label, accounts, actions)
    }

  implicit lazy val shrinkAddPermissionRequest: Shrink[AddPermissionRequest] =
    Shrink { request ⇒
      val arn = TopicArn(request.topicArn)
      val accounts = request.accounts.map(id ⇒ Account(id, arn.partition))
      Shrink.shrink(arn).map(x ⇒ request.copy(topicArn = x.arnString)) append
        Shrink.shrink(request.label).filter(_.nonEmpty).map(x ⇒ request.copy(label = x)) append
        Shrink.shrink(accounts).filter(_.nonEmpty).map(x ⇒ request.copy(accounts = x.map(_.id))) append
        Shrink.shrink(request.actions).filter(_.nonEmpty).map(x ⇒ request.copy(actions = x))
    }

  implicit lazy val arbMessageAttributeValue: Arbitrary[MessageAttributeValue] =
    Arbitrary {
      Gen.oneOf(
        Gen.resultOf(MessageAttributeValue(_: String)),
        Gen.resultOf(MessageAttributeValue(_: Array[Byte])) )
    }


  implicit lazy val shrinkMessageAttributeValue: Shrink[MessageAttributeValue] =
    Shrink {
      case MessageAttributeValue.BinaryValue(bytes) ⇒ Shrink.shrink(bytes).map(MessageAttributeValue.apply)
      case MessageAttributeValue.StringValue(str) ⇒ Shrink.shrink(str).map(MessageAttributeValue.apply)
    }

  implicit lazy val arbMessageAttributeValueMap: Arbitrary[Map[String,MessageAttributeValue]] =
    Arbitrary {
      for {
        n ← Gen.choose(0, 10)
        map ← Gen.mapOfN(n, Gen.zip(UtilGen.nonEmptyString, arbitrary[MessageAttributeValue]))
      } yield map
    }

  implicit lazy val shrinkMessageAttributeValueMap: Shrink[Map[String,MessageAttributeValue]] =
    Shrink { attrs ⇒
      Shrink.shrinkContainer2[Map,String,MessageAttributeValue].shrink(attrs).filter(_.keys.forall(_.nonEmpty))
    }

  implicit lazy val arbPlatform: Arbitrary[Platform] = Arbitrary(Gen.oneOf(Platform.values))

  implicit lazy val arbPlatformApplication: Arbitrary[PlatformApplication] =
    Arbitrary {
      for {
        arn ← arbitrary[PlatformApplicationArn]
        attributes ← SnsGen.platformApplicationAttributes(arn)
      } yield PlatformApplication(arn.arnString, attributes)
    }

  implicit lazy val shrinkPlatformApplication: Shrink[PlatformApplication] =
    Shrink { application ⇒
      val applicationArn = PlatformApplicationArn(application.arn)

      def shrinkArn[T <: Arn: Shrink](key: String, maybeArn: Option[T]): Stream[PlatformApplication] = {
        Shrink.shrink(maybeArn)
          .collect {
            case None ⇒ application.attributes - key
            case Some(arn) ⇒ application.attributes.updated(key, arn.arnString)
          }
          .map(attrs ⇒ application.copy(attributes = attrs))
      }

      Shrink.shrink(applicationArn).map(x ⇒ application.copy(arn = x.arnString)) append
        shrinkArn("EventEndpointCreated", application.eventEndpointCreated.map(TopicArn(_))) append
        shrinkArn("EventEndpointDeleted", application.eventEndpointDeleted.map(TopicArn(_))) append
        shrinkArn("EventEndpointUpdated", application.eventEndpointUpdated.map(TopicArn(_))) append
        shrinkArn("EventDeliveryFailure", application.eventDeliveryFailure.map(TopicArn(_))) append
        shrinkArn("SuccessFeedbackRoleArn", application.successFeedbackRoleArn.map(RoleArn(_))) append
        shrinkArn("FailureFeedbackRoleArn", application.failureFeedbackRoleArn.map(RoleArn(_))) append
        Shrink.shrink(application.successFeedbackSampleRate)
          .filter(_.forall(i ⇒ (i >= 0) && (i <= 100)))
          .collect {
            case None ⇒ application.attributes - "SuccessFeedbackSampleRate"
            case Some(i) ⇒ application.attributes.updated("SuccessFeedbackSampleRate", i.toString)
          }
          .map(attrs ⇒ application.copy(attributes = attrs))
    }

  implicit lazy val arbPlatformApplicationArn: Arbitrary[PlatformApplicationArn] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        region ← CoreGen.regionFor(account)
        platform ← arbitrary[Platform]
        name ← SnsGen.platformApplicationName
      } yield PlatformApplicationArn(account, region, platform, name)
    }

  implicit lazy val shrinkPlatformApplicationArn: Shrink[PlatformApplicationArn] =
    Shrink { arn ⇒
      Shrink.shrink(arn.name).filter(_.nonEmpty).map(n ⇒ arn.copy(name = n))
    }

  implicit lazy val arbPlatformEndpoint: Arbitrary[PlatformEndpoint] =
    Arbitrary {
      for {
        arn ← arbitrary[PlatformEndpointArn]
        attributes ← SnsGen.platformEndpointAttributes
      } yield PlatformEndpoint(arn.arnString, attributes)
    }

  implicit lazy val shrinkPlatformEndpoint: Shrink[PlatformEndpoint] =
    Shrink { endpoint ⇒
      Shrink.shrink(PlatformEndpointArn(endpoint.arn)).map(arn ⇒ endpoint.copy(arn = arn.arnString))
    }

  implicit lazy val arbPlatformEndpointArn: Arbitrary[PlatformEndpointArn] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        region ← CoreGen.regionFor(account)
        platform ← arbitrary[Platform]
        applicationName ← SnsGen.platformApplicationName
        endpointId ← Gen.uuid.map(_.toString)
      } yield PlatformEndpointArn(account, region, platform, applicationName, endpointId)
    }

  implicit lazy val shrinkPlatformEndpointArn: Shrink[PlatformEndpointArn] =
    Shrink { arn ⇒
      Shrink.shrink(arn.applicationName).filter(_.nonEmpty).map(n ⇒ arn.copy(applicationName = n))
    }

  implicit lazy val arbProtocol: Arbitrary[Protocol] = Arbitrary(Gen.oneOf(Protocol.values))

  implicit lazy val arbPublishRequest: Arbitrary[PublishRequest] =
    Arbitrary {
      for {
        target ← SnsGen.targetArn
        message ← UtilGen.nonEmptyString
        attributes ← arbitrary[Map[String,MessageAttributeValue]]
        subject ← Gen.option(UtilGen.nonEmptyString)
        structure ← Gen.option(Gen.const("json"))
      } yield PublishRequest(target.arnString, message, subject, structure, attributes)
    }

  implicit lazy val shrinkPublishRequest: Shrink[PublishRequest] =
    Shrink { request ⇒
      val shrunkArn = {
        val arn = Try(TopicArn(request.targetArn)).recoverWith {
          case _: IllegalArgumentException ⇒ Try(PlatformEndpointArn(request.targetArn))
        }.get
        arn match {
          case arn: TopicArn ⇒ Shrink.shrink(arn).map(x ⇒ request.copy(targetArn = x.arnString))
          case arn: PlatformEndpointArn ⇒ Shrink.shrink(arn).map(x ⇒ request.copy(targetArn = x.arnString))
        }
      }
      val shrunkMessage = Shrink.shrink(request.message).filter(_.nonEmpty).map(x ⇒ request.copy(message = x))
      val shrunkAttributes = Shrink.shrink(request.attributes).map(x ⇒ request.copy(attributes = x))
      val shrunkStructure =
        if (request.messageStructure.isDefined) {
          request.copy(messageStructure = None) #:: Stream.empty
        } else {
          Stream.empty
        }

      shrunkArn append shrunkMessage append shrunkAttributes append shrunkStructure
    }

  implicit lazy val arbRemovePermissionRequest: Arbitrary[RemovePermissionRequest] =
    Arbitrary {
      for {
        topicArn ← arbitrary[TopicArn]
        label ← CoreGen.statementId
      } yield RemovePermissionRequest(topicArn.arnString, label)
    }

  implicit lazy val shrinkRemovePermissionRequest: Shrink[RemovePermissionRequest] =
    Shrink { request ⇒
      val arn = TopicArn(request.topicArn)
      Shrink.shrink(arn).map(x ⇒ request.copy(topicArn = x.arnString)) append
        Shrink.shrink(request.label).filter(_.nonEmpty).map(x ⇒ request.copy(label = x))
    }

  implicit lazy val arbSNSAction: Arbitrary[SNSAction] = Arbitrary(Gen.oneOf(SNSAction.values))

  implicit lazy val arbSubscription: Arbitrary[Subscription] =
    Arbitrary {
      for {
        attrs ← arbitrary[SubscriptionAttributes]
      } yield Subscription(attrs.asMap)
    }

  implicit lazy val shrinkSubscription: Shrink[Subscription] =
    Shrink { sub ⇒
      val attrs = SubscriptionAttributes(sub.attributes)
      Shrink.shrink(attrs)
        .filter(_ != attrs)
        .map(a ⇒ Subscription(a.asMap))
    }

  implicit lazy val arbSubscriptionArn: Arbitrary[SubscriptionArn] =
    Arbitrary {
      for {
        owner ← arbitrary[Account]
        region ← CoreGen.regionFor(owner)
        topicName ← SnsGen.topicName
        subscriptionId ← Gen.uuid.map(_.toString)
      } yield SubscriptionArn(owner, region, topicName, subscriptionId)
    }

  implicit lazy val shrinkSubscriptionArn: Shrink[SubscriptionArn] =
    Shrink { arn ⇒
      Shrink.shrink(arn.topicName).filter(_.nonEmpty).map(x ⇒ arn.copy(topicName = x))
    }

  implicit lazy val arbSubscriptionEndpointApplication: Arbitrary[SubscriptionEndpoint.ApplicationEndpoint] =
    Arbitrary {
      for {
        arn ← arbitrary[PlatformEndpointArn]
      } yield Protocol.Application(arn.arnString)
    }

  implicit lazy val arbSubscriptionEndpointEmail: Arbitrary[SubscriptionEndpoint.EmailEndpoint] =
    Arbitrary {
      Gen.identifier.map(who ⇒ Protocol.Email(s"$who@example.com"))
    }

  implicit lazy val arbSubscriptionEndpointEmailJson: Arbitrary[SubscriptionEndpoint.EmailJsonEndpoint] =
    Arbitrary {
      Gen.identifier.map(who ⇒ Protocol.EmailJson(s"$who@example.com"))
    }

  implicit lazy val arbSubscriptionEndpointHttp: Arbitrary[SubscriptionEndpoint.HttpEndpoint] =
    Arbitrary {
      Gen.const(Protocol.Http("http://example.com"))
    }

  implicit lazy val arbSubscriptionEndpointHttps: Arbitrary[SubscriptionEndpoint.HttpsEndpoint] =
    Arbitrary {
      Gen.const(Protocol.Https("https://example.com"))
    }

  implicit lazy val arbSubscriptionEndpointLambda: Arbitrary[SubscriptionEndpoint.LambdaEndpoint] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        region ← CoreGen.regionFor(account)
        name ← Gen.identifier
      } yield {
        val arn = new Arn(Arn.Namespace.Lambda, Some(region), account) {
          override val resource = s"function:$name"
        }
        Protocol.Lambda(arn.arnString)
      }
    }

  implicit lazy val arbSubscriptionEndpointSms: Arbitrary[SubscriptionEndpoint.SMSEndpoint] =
    Arbitrary {
      for {
        digits ← Gen.listOfN(7, Gen.numChar).map(_.mkString)
      } yield Protocol.SMS(s"1555$digits")
    }

  implicit lazy val arbSubscriptionEndpointSqs: Arbitrary[SubscriptionEndpoint.SQSEndpoint] =
    Arbitrary {
      for {
        account ← arbitrary[Account]
        region ← CoreGen.regionFor(account)
        name ← Gen.identifier
      } yield {
        val arn = new Arn(Arn.Namespace.AmazonSQS, Some(region), account) {
          override val resource = name
        }
        Protocol.SQS(arn.arnString)
      }
    }

  implicit lazy val arbSubscriptionEndpoint: Arbitrary[SubscriptionEndpoint] =
    Arbitrary {
      Gen.oneOf(
        arbitrary[SubscriptionEndpoint.ApplicationEndpoint],
        arbitrary[SubscriptionEndpoint.EmailEndpoint],
        arbitrary[SubscriptionEndpoint.EmailJsonEndpoint],
        arbitrary[SubscriptionEndpoint.HttpEndpoint],
        arbitrary[SubscriptionEndpoint.HttpsEndpoint],
        arbitrary[SubscriptionEndpoint.LambdaEndpoint],
        arbitrary[SubscriptionEndpoint.SMSEndpoint],
        arbitrary[SubscriptionEndpoint.SQSEndpoint]
      )
    }

  implicit lazy val arbSubscriptionSummary: Arbitrary[SubscriptionSummary] =
    Arbitrary {
      val pending =
        for {
          topicArn ← arbitrary[TopicArn]
          endpoint ← arbitrary[SubscriptionEndpoint]
        } yield SubscriptionSummary(None, topicArn.arnString, endpoint, topicArn.account.id)
      val confirmed =
        for {
          topicArn ← arbitrary[TopicArn]
          subscriptionId ← Gen.uuid.map(_.toString)
          endpoint ← arbitrary[SubscriptionEndpoint]
        } yield {
          val arn = SubscriptionArn(topicArn.account, topicArn.region, topicArn.name, subscriptionId).arnString
          SubscriptionSummary(Some(arn), topicArn.arnString, endpoint, topicArn.account.id)
        }

      Gen.frequency(
        9 → confirmed,
        1 → pending
      )
    }

  implicit lazy val arbTopic: Arbitrary[Topic] =
    Arbitrary {
      arbitrary[TopicAttributes].map(x ⇒ Topic(x.asMap))
    }

  implicit lazy val shrinkTopic: Shrink[Topic] =
    Shrink { topic ⇒
      Shrink.shrink(TopicAttributes(topic.attributes))
        .filter(_.asMap != topic.attributes)
        .map(a ⇒ Topic(a.asMap))
    }

  implicit lazy val arbTopicArn: Arbitrary[TopicArn] =
    Arbitrary {
      for {
        owner ← arbitrary[Account]
        region ← CoreGen.regionFor(owner)
        topicName ← SnsGen.topicName
      } yield TopicArn(owner, region, topicName)
    }

  implicit lazy val shrinkTopicArn: Shrink[TopicArn] =
    Shrink { arn ⇒
      Shrink.shrink(arn.name).filter(_.nonEmpty).map(x ⇒ arn.copy(name = x))
    }

  implicit lazy val arbPlatformApplicationCredentials: Arbitrary[PlatformApplicationCredentials] =
    Arbitrary(SnsGen.platformApplicationCredentials)


  //object SNSGen {
  //
  //  val region: Gen[String] = Gen.oneOf(Regions.toSeq)
  //
  //  val policy: Gen[String] = for(text ← Gen.alphaStr) yield JsObject("text" → JsString(text)).compactPrint
  //
  //  private val retryPolicy: Gen[JsObject] =
  //    for {
  //      numRetries ← Gen.choose(0,100)
  //      numNoDelayRetries ← Gen.choose(0,numRetries)
  //      minDelayTarget ← Gen.choose(0,120)
  //      numMinDelayRetries ← Gen.choose(0,numRetries)
  //      maxDelayTarget ← Gen.choose(minDelayTarget,3600)
  //      numMaxDelayRetries ← Gen.choose(0,numRetries)
  //      backoffFunction ← Gen.oneOf("linear", "arithmetic", "geometric", "exponential")
  //    } yield JsObject(
  //      "minDelayTarget" → JsNumber(minDelayTarget),
  //      "maxDelayTarget" → JsNumber(maxDelayTarget),
  //      "numRetries" → JsNumber(numRetries),
  //      "numNoDelayRetries" → JsNumber(numNoDelayRetries),
  //      "numMinDelayRetries" → JsNumber(numMinDelayRetries),
  //      "numMaxDelayRetries" → JsNumber(numMaxDelayRetries),
  //      "backoffFunction" → JsString(backoffFunction))
  //
  //  private val throttlePolicy: Gen[Option[JsObject]] =
  //    for (maxReceivesPerSecond ← Gen.oneOf(Gen.const(None), Gen.choose(1,10).map(Some.apply))) yield {
  //      maxReceivesPerSecond.map(n ⇒ JsObject("maxReceivesPerSecond" → JsNumber(n)))
  //    }
  //
  //  val topicDeliveryPolicy: Gen[String] =
  //    for {
  //      disableSubscriptionOverrides ← boolean
  //      maxReceivesPerSecond ← Gen.oneOf(Gen.const(None), Gen.choose(1,10).map(Some.apply))
  //      defaultHealthyRetryPolicy ← retryPolicy
  //      defaultThrottlePolicy ← throttlePolicy
  //    } yield {
  //      val httpPolicy =
  //        defaultThrottlePolicy
  //          .map(p ⇒ Map("defaultThrottlePolicy" → p))
  //          .getOrElse(Map.empty) ++ Map(
  //          "defaultHealthyRetryPolicy" → defaultHealthyRetryPolicy,
  //          "disableSubscriptionOverrides" → JsBoolean(disableSubscriptionOverrides)
  //        )
  //      JsObject("http" → JsObject(httpPolicy)).compactPrint
  //    }
  //
  //  val maybeTopicDeliveryPolicy: Gen[Option[String]] =
  //    Gen.oneOf(Gen.const(None), topicDeliveryPolicy.map(Some.apply))
  //
  //  val subscriptionDeliveryPolicy: Gen[String] =
  //    for {
  //      healthyRetryPolicy ← retryPolicy
  //      throttlePolicy ← throttlePolicy
  //    } yield
  //      JsObject(
  //        "healthyRetryPolicy" → healthyRetryPolicy,
  //        "throttlePolicy" → throttlePolicy.getOrElse(JsNull)
  //      ).compactPrint
  //
  //  val maybeSubscriptionDeliveryPolicy: Gen[Option[String]] =
  //    Gen.oneOf(Gen.const(None), topicDeliveryPolicy.map(Some.apply))
  //
  //  val topicArnObj: Gen[TopicArn] =
  //    for {
  //      region ← region
  //      accountId ← arbitrary[AwsGen.Account].map(_.value)
  //      name ← topicName
  //    } yield TopicArn(region, accountId, name)
  //
  //  val topicArn: Gen[String] = topicArnObj.map(_.toString)
  //
  //  def topicArn(region: String, owner: String) =
  //    for(name ← topicName) yield TopicArn(region, owner, name).toString
  //
  //  val snsAction: Gen[SNSAction] = Gen.oneOf(SNSAction.values)
  //
  //  val topic: Gen[Topic] =
  //    for {
  //      topicArn ← topicArnObj
  //      displayName ← Gen.alphaStr
  //      subscriptionsPending ← Gen.choose(0, 10)
  //      subscriptionsConfirmed ← Gen.choose(0, 100)
  //      subscriptionsDeleted ← Gen.choose(0, 50)
  //      policy ← SNSGen.policy
  //      deliveryPolicy ← SNSGen.maybeTopicDeliveryPolicy
  //      effectiveDeliverPolicy ← SNSGen.policy
  //    } yield {
  //      val attributes =
  //        deliveryPolicy.map(p ⇒ Map("DeliveryPolicy" → p)).getOrElse(Map.empty) ++
  //          Map(
  //            "TopicArn" → topicArn.toString,
  //            "DisplayName" → displayName,
  //            "Owner" → topicArn.owner,
  //            "Policy" → policy,
  //            "SubscriptionsPending" → subscriptionsPending.toString,
  //            "SubscriptionsConfirmed" → subscriptionsConfirmed.toString,
  //            "SubscriptionsDeleted" → subscriptionsDeleted.toString,
  //            "EffectiveDeliveryPolicy" → effectiveDeliverPolicy
  //          )
  //      Topic(attributes)
  //    }
  //
  //  private def withGeneratedRegionAndOwner[T](gen: (String, String) ⇒ Gen[T]) =
  //    for {
  //      region ← region
  //      owner ← arbitrary[AwsGen.Account].map(_.value)
  //      t ← gen(region, owner)
  //    } yield t
  //
  //  def sqsEndpoint(region: String, owner: String): Gen[SubscriptionEndpoint.SQSEndpoint] =
  //    for (name ← nonEmptyString) yield Protocol.SQS(s"arn:aws:sqs:$region:$owner:$name")
  //
  //  val sqsEndpoint: Gen[SubscriptionEndpoint.SQSEndpoint] =
  //    withGeneratedRegionAndOwner(sqsEndpoint(_: String, _: String))
  //
  //  val platform: Gen[Platform] = Gen.oneOf(Platform.values)
  //
  //
  //  def platformApplicationArn(region: String, owner: String, platform: Platform, name: String): String =
  //    s"arn:aws:sns:$region:$owner:app/${platform.name}/$name"
  //
  //  def platformApplicationArn(region: String, owner: String): Gen[String] =
  //    for {
  //      platform ← platform
  //      name ← platformApplicationName
  //    } yield platformApplicationArn(region, owner, platform, name)
  //
  //  def platformApplicationArn(platform: Platform, name: String): Gen[String] =
  //    withGeneratedRegionAndOwner(platformApplicationArn(_: String, _: String, platform, name))
  //
  //  val platformApplicationArn: Gen[String] =
  //    withGeneratedRegionAndOwner(platformApplicationArn(_: String, _: String))
  //
  //  def applicationEndpoint(region: String, owner: String): Gen[SubscriptionEndpoint.ApplicationEndpoint] =
  //    for(arn ← platformApplicationArn(region, owner)) yield Protocol.Application(arn)
  //
  //  val applicationEndpoint: Gen[SubscriptionEndpoint.ApplicationEndpoint] =
  //    withGeneratedRegionAndOwner(applicationEndpoint(_: String, _: String))
  //
  //  private def lambdaEndpoint(region: String, owner: String): Gen[SubscriptionEndpoint.LambdaEndpoint] =
  //    for (name ← nonEmptyString) yield Protocol.Lambda(s"arn:aws:lambda:$region:$owner:function:$name")
  //
  //  val lambdaEndpoint: Gen[SubscriptionEndpoint.LambdaEndpoint] =
  //    withGeneratedRegionAndOwner(lambdaEndpoint(_: String, _: String))
  //
  //  case class TopicArn(region: String, owner: String, name: String) {
  //    override def toString = s"arn:aws:sns:$region:$owner:$name"
  //  }
  //  object TopicArn {
  //    val TopicArnRegex = "^arn:aws:sns:([^:]+):([^:]+):([^:]+)$".r
  //    def apply(arn: String): TopicArn = arn match {
  //      case TopicArnRegex(r, o, n) ⇒ TopicArn(r, o, n)
  //    }
  //  }
  //
  //  def subscriptionEndpoint(region: String, owner: String): Gen[SubscriptionEndpoint] =
  //    Gen.oneOf(
  //      httpEndpoint,
  //      httpsEndpoint,
  //      emailEndpoint,
  //      emailJsonEndpoint,
  //      smsEndpoint,
  //      sqsEndpoint(region, owner),
  //      applicationEndpoint(region, owner),
  //      lambdaEndpoint(region, owner))
  //
  //  def subscriptionEndpoint(topicArn: String): Gen[SubscriptionEndpoint] = {
  //    val TopicArn(region, owner, _) = TopicArn(topicArn)
  //    subscriptionEndpoint(region, owner)
  //  }
  //
  //  val subscriptionEndpoint: Gen[SubscriptionEndpoint] =
  //    withGeneratedRegionAndOwner(subscriptionEndpoint(_: String, _: String))
  //
  //  def subscriptionEndpoint(topicArn: TopicArn): Gen[SubscriptionEndpoint] =
  //    subscriptionEndpoint(topicArn.region, topicArn.owner)
  //
  //  val hexDigit: Gen[Char] = Gen.oneOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
  //
  //  val confirmationToken: Gen[String] = for(digits ← Gen.listOfN(64, hexDigit)) yield digits.mkString
  //
  //  val subscription: Gen[Subscription] =
  //    for {
  //      topicArn ← topicArn
  //      subscription ← subscription(topicArn)
  //    } yield subscription
  //
  //  def subscription(topic: Topic): Gen[Subscription] = subscription(topic.arn)
  //
  //  def subscriptionFrom(summary: SubscriptionSummary): Gen[Option[Subscription]] =
  //    if (summary.isPending) {
  //      Gen.const(None)
  //    } else {
  //      for {
  //        rawMessageDelivery ← boolean
  //        confirmationWasAuthenticated ← boolean
  //        maybeDeliveryPolicy ← maybeSubscriptionDeliveryPolicy
  //      } yield {
  //        val baseAttributes =
  //          Map(
  //            "SubscriptionArn" → summary.arn.get,
  //            "TopicArn" → summary.topicArn,
  //            "Protocol" → summary.endpoint.protocol.toAws,
  //            "Endpoint" → summary.endpoint.endpoint,
  //            "Owner" → summary.owner,
  //            "RawMessageDelivery" → rawMessageDelivery.toString,
  //            "ConfirmationWasAuthenticated" → confirmationWasAuthenticated.toString
  //          )
  //        val policyAttributes =
  //          maybeDeliveryPolicy.map(p ⇒ Map("DeliveryPolicy" → p, "EffectiveDeliveryPolicy" → p)).getOrElse(Map.empty)
  //
  //        Some(Subscription(baseAttributes ++ policyAttributes))
  //      }
  //    }
  //
  //  def subscription(topicArn: String): Gen[Subscription] =
  //    for {
  //      arn ← subscriptionArn(topicArn)
  //      rawMessageDelivery ← boolean
  //      SubscriptionEndpoint(protocol, endpoint) ← subscriptionEndpoint(topicArn)
  //      confirmationWasAuthenticated ← boolean
  //      maybeDeliveryPolicy ← maybeSubscriptionDeliveryPolicy
  //    } yield {
  //      val baseAttributes =
  //        Map(
  //          "SubscriptionArn" → arn,
  //          "TopicArn" → topicArn.toString,
  //          "Protocol" → protocol.toAws,
  //          "Endpoint" → endpoint,
  //          "Owner" → TopicArn(topicArn).owner,
  //          "RawMessageDelivery" → rawMessageDelivery.toString,
  //          "ConfirmationWasAuthenticated" → confirmationWasAuthenticated.toString
  //        )
  //      val policyAttributes =
  //        maybeDeliveryPolicy.map(p ⇒ Map("DeliveryPolicy" → p, "EffectiveDeliveryPolicy" → p)).getOrElse(Map.empty)
  //
  //      Subscription(baseAttributes ++ policyAttributes)
  //    }
  //
  //  val topicWithSubscription: Gen[(Topic,Subscription)] =
  //    for {
  //      topic ← topic
  //      subscription ← subscription(topic)
  //    } yield (topic, subscription)
  //
  //  val subscriptionArn: Gen[String] =
  //    for {
  //      topicArn ← topicArn
  //      subscriptionArn ← subscriptionArn(topicArn)
  //    } yield subscriptionArn
  //
  //  def subscriptionArn(topicArn: String): Gen[String] = Gen.uuid.map(uuid ⇒ s"$topicArn:$uuid")
  //
  //  val subscriptionSummary: Gen[SubscriptionSummary] =
  //    for {
  //      topicArn ← topicArn
  //      summary ← subscriptionSummary(topicArn)
  //    } yield summary
  //
  //  def subscriptionSummary(topicArn: String): Gen[SubscriptionSummary] =
  //    for {
  //      subscriptionArn ← Gen.frequency(3 → subscriptionArn(topicArn).map(Some.apply), 1 → Gen.const(None))
  //      endpoint ← subscriptionEndpoint(topicArn)
  //    } yield SubscriptionSummary(subscriptionArn, topicArn, endpoint, TopicArn(topicArn).owner)
  //
  //  val platformEndpointArn: Gen[String] =
  //    for {
  //      applicationArn ← platformApplicationArn
  //      endpointArn ← platformEndpointArn(applicationArn)
  //    } yield endpointArn
  //
  //  def platformEndpointArn(platformApplicationArn: String): Gen[String] =
  //    for (id ← Gen.uuid) yield s"$platformApplicationArn/$id".replaceFirst(":app/",":endpoint/")
  //
  //  val platformEndpointAttributes: Gen[Map[String,String]] =
  //    for {
  //      token ← SNSGen.nonEmptyString
  //      enabled ← SNSGen.boolean
  //      customUserData ← Gen.option(Gen.alphaStr)
  //    } yield {
  //      customUserData.map(x ⇒ Map("CustomUserData" → x)).getOrElse(Map.empty) ++
  //        Map("Token" → token, "Enabled" → enabled.toString)
  //    }
  //
  //  val platformEndpoint: Gen[PlatformEndpoint] =
  //    for {
  //      arn ← platformEndpointArn
  //      attributes ← platformEndpointAttributes
  //    } yield PlatformEndpoint(arn, attributes)
  //
  //  def platformEndpoint(platformApplicationArn: String): Gen[PlatformEndpoint] =
  //    for {
  //      arn ← platformEndpointArn(platformApplicationArn)
  //      attributes ← platformEndpointAttributes
  //    } yield PlatformEndpoint(arn, attributes)
  //
  //  val messageAttributeValue: Gen[MessageAttributeValue[_]] = {
  //    val stringValue = for(value ← nonEmptyString) yield MessageAttributeValue(value)
  //    val byteArrayValue = for(value ← byteArray) yield MessageAttributeValue(value)
  //
  //    Gen.oneOf(stringValue, byteArrayValue)
  //  }
  //
  //  val messageAttributes: Gen[Map[String,MessageAttributeValue[_]]] =
  //    for {
  //      size ← Gen.choose(0, 10)
  //      attributes ← Gen.mapOfN(size, Gen.zip(nonEmptyString, messageAttributeValue))
  //    } yield attributes
  //
  //  val jsonMessagePayload: Gen[String] =
  //    Gen.mapOf(Gen.zip(SNSGen.nonEmptyString, SNSGen.nonEmptyString.map(JsString(_)))).map(JsObject(_).compactPrint)
  //
  //  val messageMap: Gen[Map[String,String]] = {
  //    val protocol = Gen.oneOf("default", "http", "https", "email", "email-json", "sms", "sqs", "lambda",
  //      "ADM", "APNS", "APNS_SANDBOX", "BAIDU", "GCM", "MPNS", "WNS")
  //    val message = Gen.oneOf(jsonMessagePayload, SNSGen.nonEmptyString)
  //    for {
  //      size ← Gen.choose(0,4)
  //      messageMap ← Gen.mapOfN(size, Gen.zip(protocol, message))
  //    } yield messageMap
  //  }
  //
  //  val messageId: Gen[String] = Gen.uuid.map(_.toString)
  //}
}
