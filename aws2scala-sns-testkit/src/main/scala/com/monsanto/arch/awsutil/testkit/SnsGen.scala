package com.monsanto.arch.awsutil.testkit

import java.util.UUID

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.monsanto.arch.awsutil.identitymanagement.model.{Path, RoleArn}
import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.sns.model._
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import com.monsanto.arch.awsutil.{Account, Arn}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import scala.util.Try

object SnsGen {
  val admPlatformApplicationCredentials: Gen[PlatformApplicationCredentials] =
    for {
      clientId ← UtilGen.nonEmptyString
      clientSecret ← UtilGen.nonEmptyString
    } yield Platform.ADM(clientId, clientSecret)

  val apnsDevelopmentPlatformApplicationCredentials: Gen[PlatformApplicationCredentials] =
    for {
      certificate ← UtilGen.nonEmptyString
      key ← UtilGen.nonEmptyString
    } yield Platform.APNSDevelopment(certificate, key)

  val apnsProductionPlatformApplicationCredentials: Gen[PlatformApplicationCredentials] =
    for {
      certificate ← UtilGen.nonEmptyString
      key ← UtilGen.nonEmptyString
    } yield Platform.APNSProduction(certificate, key)

  val baiduPlatformApplicationCredentials: Gen[PlatformApplicationCredentials] =
    for {
      apiKey ← UtilGen.nonEmptyString
      secretKey ← UtilGen.nonEmptyString
    } yield Platform.Baidu(apiKey, secretKey)

  val gcmPlatformApplicationCredentials: Gen[PlatformApplicationCredentials] =
    for (serverApiKey ← UtilGen.nonEmptyString) yield Platform.GCM(serverApiKey)

  val mpnsPlatformApplicationCredentials: Gen[PlatformApplicationCredentials] = {
    val unauthenticatedGen = Gen.const(Platform.MPNS())
    val authenticatedGen = for {
      tlsCertificateChain ← UtilGen.nonEmptyString
      privateKey ← UtilGen.nonEmptyString
    } yield Platform.MPNS(tlsCertificateChain, privateKey)

    Gen.frequency(4 → authenticatedGen, 1 → unauthenticatedGen)
  }

  val wnsPlatformApplicationCredentials: Gen[PlatformApplicationCredentials] =
    for {
      packageSecurityIdentifier ← UtilGen.nonEmptyString
      secretKey ← UtilGen.nonEmptyString
    } yield Platform.WNS(packageSecurityIdentifier, secretKey)

  val platformApplicationCredentials: Gen[PlatformApplicationCredentials] = {
    Gen.oneOf(
      admPlatformApplicationCredentials,
      apnsDevelopmentPlatformApplicationCredentials,
      apnsProductionPlatformApplicationCredentials,
      baiduPlatformApplicationCredentials,
      gcmPlatformApplicationCredentials,
      mpnsPlatformApplicationCredentials,
      wnsPlatformApplicationCredentials)
  }

  def platformApplicationCredentials(platform: Platform): Gen[PlatformApplicationCredentials] = {
    platform match {
      case Platform.ADM ⇒ admPlatformApplicationCredentials
      case Platform.APNSDevelopment ⇒ apnsDevelopmentPlatformApplicationCredentials
      case Platform.APNSProduction ⇒ apnsProductionPlatformApplicationCredentials
      case Platform.Baidu ⇒ baiduPlatformApplicationCredentials
      case Platform.GCM ⇒ gcmPlatformApplicationCredentials
      case Platform.MPNS ⇒ mpnsPlatformApplicationCredentials
      case Platform.WNS ⇒ wnsPlatformApplicationCredentials
    }
  }

  val platformApplicationName: Gen[String] = {
    val applicationNameChars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ Seq('-', '_', '.')
    UtilGen.stringOf(Gen.oneOf(applicationNameChars), 1, 256).suchThat(_.nonEmpty)
  }

  def platformApplicationAttributes(arn: PlatformApplicationArn): Gen[Map[String,String]] = {
    val maybeEventTopicArn: Gen[Option[String]] =
      Gen.frequency(
        4 → Gen.option(SnsGen.topicArn(arn.account, arn.region).map(_.arnString)),
        1 → arbitrary[TopicArn].map(arn ⇒ Some(arn.arnString))
      )

    def maybeFeedbackRoleArn(name: String): Gen[Option[String]] =
      Gen.frequency(
        1 → arbitrary[RoleArn].map(arn ⇒ Some(arn.arnString)),
        7 → Some(RoleArn(arn.account, name, Path.empty).arnString),
        1 → Some(""),
        1 → None)

    for {
      enabled ← arbitrary[Boolean]
      eventEndpointCreated ← maybeEventTopicArn
      eventEndpointDeleted ← maybeEventTopicArn
      eventEndpointUpdated ← maybeEventTopicArn
      eventDeliveryFailure ← maybeEventTopicArn
      successFeedbackRoleArn ← maybeFeedbackRoleArn("SNSSuccessFeedback")
      failureFeedbackRoleArn ← maybeFeedbackRoleArn("SNSFailureFeedback")
      successFeedbackSampleRate ← Gen.option(Gen.choose(0, 100))
    } yield {
      Map(
        "Enabled" → Some(enabled.toString),
        "EventEndpointCreated" → eventEndpointCreated,
        "EventEndpointDeleted" → eventEndpointDeleted,
        "EventEndpointUpdated" → eventEndpointUpdated,
        "EventDeliveryFailure" → eventDeliveryFailure,
        "SuccessFeedbackRoleArn" → successFeedbackRoleArn,
        "FailureFeedbackRoleArn" → failureFeedbackRoleArn,
        "SuccessFeedbackSampleRate" → successFeedbackSampleRate.map(_.toString)
      ).filter(_._2.isDefined).mapValues(_.get)
    }
  }

  val platformEndpointAttributes: Gen[Map[String,String]] = {
    for {
      enabled ← arbitrary[Boolean].map(_.toString)
      token ← UtilGen.nonEmptyString
      customUserData ← Gen.option(UtilGen.nonEmptyString)
    } yield {
      val baseMap = Map("Token" → token, "Enabled" → enabled)
      customUserData
        .map(data ⇒ baseMap + ("CustomUserData" → data))
        .getOrElse(baseMap)
    }
  }

  private val uuidString: Gen[String] = Gen.uuid.map(_.toString).suchThat(id ⇒ Try(UUID.fromString(id)).isSuccess)

  val subscriptionId: Gen[String] = uuidString

  val endpointId: Gen[String] = uuidString

  val messageId: Gen[String] = uuidString

  val targetArn: Gen[Arn] = Gen.oneOf(arbitrary[TopicArn], arbitrary[PlatformEndpointArn])

  def topicArn(account: Account, region: Region): Gen[TopicArn] =
    topicName.map(n ⇒ TopicArn(account, region, n))

  val topicName: Gen[String] = {
    val topicChars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ Seq('-', '_')
    UtilGen.stringOf(Gen.oneOf(topicChars), 1, 256).suchThat(_.nonEmpty)
  }

  val jsonMessagePayload: Gen[String] = {
    val keyGen = Gen.identifier
    val valueGen = Gen.identifier
    val jsonObjectGen =
      Gen.mapOf(Gen.zip(keyGen, valueGen)).map { data ⇒
        val om = new ObjectMapper()
        val obj = om.createObjectNode()
        data.foreach { entry ⇒
          obj.put(entry._1, entry._2)
        }
        obj.toString
      }

    jsonObjectGen.suchThat { json ⇒
      val om = new ObjectMapper()
      try {
        om.readTree(json).isObject
      } catch {
        case _: JsonParseException ⇒ false
      }
    }
  }

  val confirmationToken: Gen[String] =
    (for (digits ← Gen.listOfN(64, UtilGen.lowerHexChar)) yield digits.mkString).suchThat(_.length == 64)

  val messageMap: Gen[Map[String,String]] = {
    val protocols = Seq("http", "https", "email", "email-json", "sms", "sqs", "lambda",
      "ADM", "APNS", "APNS_SANDBOX", "BAIDU", "GCM", "MPNS", "WNS")
    val protocol = Gen.oneOf(protocols)
    val message = Gen.oneOf(SnsGen.jsonMessagePayload, Gen.identifier)
    val generator =
      for {
        size ← Gen.choose(0, protocols.size)
        messageMap ← Gen.mapOfN(size, Gen.zip(protocol, message))
        defaultMessage ← UtilGen.nonEmptyString
      } yield messageMap + ("default" → defaultMessage)
    generator.suchThat(messageMap ⇒
      messageMap.contains("default") &&
        messageMap.keys.forall(p ⇒ protocols.contains(p) || p == "default") &&
        messageMap.values.forall(_.nonEmpty))
  }
}
