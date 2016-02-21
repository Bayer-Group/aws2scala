package com.monsanto.arch.awsutil.sns.model

import java.net.URI

import com.monsanto.arch.awsutil.sns.model.Protocol._
import com.monsanto.arch.awsutil.sns.model.SubscriptionEndpoint.{HttpEndpoint, HttpsEndpoint}
import org.scalacheck.Gen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks.forAll
import org.scalatest.prop.TableDrivenPropertyChecks.{Table, forAll ⇒ forAllIn}

class SubscriptionEndpointSpec extends FreeSpec {
  "an HttpEndpoint" - {
    "cannot be built from a non-HTTP URI" in {
      val badUris = Table(
        "uri",
        URI.create(""),
        URI.create("/foo"),
        URI.create("arn:aws:lambda:*"),
        URI.create("ftp://example.com"),
        URI.create("https://example.com")
      )

      forAllIn(badUris) { uri ⇒
        an [IllegalArgumentException] shouldBe thrownBy (HttpEndpoint(uri))
      }
    }

    "should have a valid endpoint value" in {
      forAll(SNSGen.httpEndpoint → "endpoint") { endpoint ⇒
        URI.create(endpoint.endpoint) shouldBe endpoint.uri
      }
    }

    behave like theCorrectProtocol(Http, SNSGen.httpEndpoint)
  }

  "an HttpsEndpoint" - {
    "cannot be built from a non-HTTPS URI" in {
      val badUris = Table(
        "uri",
        URI.create(""),
        URI.create("/foo"),
        URI.create("arn:aws:lambda:*"),
        URI.create("ftp://example.com"),
        URI.create("http://example.com")
      )

      forAllIn(badUris) { uri ⇒
        an [IllegalArgumentException] shouldBe thrownBy (HttpsEndpoint(uri))
      }
    }

    "should have a valid endpoint value" in {
      forAll(SNSGen.httpsEndpoint → "endpoint") { endpoint ⇒
        URI.create(endpoint.endpoint) shouldBe endpoint.uri
      }
    }

    behave like theCorrectProtocol(Https, SNSGen.httpsEndpoint)
  }

  "an EmailEndpoint" - {
    behave like theCorrectProtocol(Email, SNSGen.emailEndpoint)
  }

  "an EmailJsonEndpoint" - {
    behave like theCorrectProtocol(EmailJson, SNSGen.emailJsonEndpoint)
  }

  "an SMSEndpoint" - {
    behave like theCorrectProtocol(SMS, SNSGen.smsEndpoint)
  }

  "an SQSEndpoint" - {
    behave like theCorrectProtocol(SQS, SNSGen.sqsEndpoint)
  }

  "an ApplicationEndpoint" - {
    behave like theCorrectProtocol(Application, SNSGen.applicationEndpoint)
  }

  "a LambdaEndpoint" - {
    behave like theCorrectProtocol(Lambda, SNSGen.lambdaEndpoint)
  }

  private def theCorrectProtocol(protocol: Protocol, endpointGen: Gen[SubscriptionEndpoint]): Unit = {
    "have the correct protcol" in {
      forAll(endpointGen → "endpoint") { endpoint ⇒
        endpoint.protocol shouldBe protocol
      }
    }
  }
}
