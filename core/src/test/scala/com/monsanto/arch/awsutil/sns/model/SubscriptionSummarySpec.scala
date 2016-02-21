package com.monsanto.arch.awsutil.sns.model

import akka.stream.scaladsl.Flow
import com.monsanto.arch.awsutil.Materialised
import com.monsanto.arch.awsutil.sns.StreamingSNSClient
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class SubscriptionSummarySpec extends FreeSpec with MockFactory with Materialised {
  private implicit val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 50, maxSize = 50)

  "a SubscriptionSummary should" - {
    "indicate if it is pending" in {
      forAll(SNSGen.subscriptionSummary → "summary") { summary ⇒
        val result = summary.isPending
        result shouldBe summary.arn.isEmpty
      }
    }

    "indicate if it is confirmed" in {
      forAll(SNSGen.subscriptionSummary → "summary") { summary ⇒
        val result = summary.isConfirmed
        result shouldBe summary.arn.isDefined
      }
    }

    "convert to a full subscription" in {
      val summaryAndMaybeSubscription =
        for {
          summary ← SNSGen.subscriptionSummary
          maybeSubscription ← SNSGen.subscriptionFrom(summary)
        } yield (summary, maybeSubscription)
      forAll(summaryAndMaybeSubscription → "summaryAndMaybeSubscription") { summaryAndMaybeSubscription ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")
        val (summary, maybeSubscription) = summaryAndMaybeSubscription

        (sns.subscriptionAttributesGetter _)
          .expects()
          .returning(Flow[String].map { arn ⇒
            maybeSubscription match {
              case None ⇒ fail("Should not be called.")
              case Some(sub) ⇒
                arn shouldBe sub.arn
                sub.attributes
            }
          })

        val result = summary.asSubscription().futureValue
        result shouldBe maybeSubscription
      }
    }
  }
}
