package com.monsanto.arch.awsutil.sns.model

import java.net.URI

import com.monsanto.arch.awsutil.sns.model.Protocol._
import com.monsanto.arch.awsutil.sns.model.SubscriptionEndpoint._
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import org.scalacheck.{Arbitrary, Shrink}
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
      forAll { endpoint: HttpEndpoint ⇒
        URI.create(endpoint.endpoint) shouldBe endpoint.uri
      }
    }

    behave like theCorrectProtocol[HttpEndpoint](Http)
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
      forAll { endpoint: HttpsEndpoint ⇒
        URI.create(endpoint.endpoint) shouldBe endpoint.uri
      }
    }

    behave like theCorrectProtocol[HttpsEndpoint](Https)
  }

  "an EmailEndpoint" - {
    behave like theCorrectProtocol[EmailEndpoint](Email)
  }

  "an EmailJsonEndpoint" - {
    behave like theCorrectProtocol[EmailJsonEndpoint](EmailJson)
  }

  "an SMSEndpoint" - {
    behave like theCorrectProtocol[SMSEndpoint](SMS)
  }

  "an SQSEndpoint" - {
    behave like theCorrectProtocol[SQSEndpoint](SQS)
  }

  "an ApplicationEndpoint" - {
    behave like theCorrectProtocol[ApplicationEndpoint](Application)
  }

  "a LambdaEndpoint" - {
    behave like theCorrectProtocol[LambdaEndpoint](Lambda)
  }

  private def theCorrectProtocol[T <: SubscriptionEndpoint: Arbitrary: Shrink](protocol: Protocol): Unit = {
    "have the correct protocol" in {
      forAll { endpoint: T ⇒
        endpoint.protocol shouldBe protocol
      }
    }
  }
}
