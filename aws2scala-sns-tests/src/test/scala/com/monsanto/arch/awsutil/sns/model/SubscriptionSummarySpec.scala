package com.monsanto.arch.awsutil.sns.model

import akka.stream.scaladsl.Flow
import com.monsanto.arch.awsutil.sns.StreamingSNSClient
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Materialised
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class SubscriptionSummarySpec extends FreeSpec with MockFactory with Materialised {
  private implicit val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 50, maxSize = 50)

  "a SubscriptionSummary should" - {
    "indicate if it is pending" in {
      forAll { summary: SubscriptionSummary ⇒
        val result = summary.isPending
        result shouldBe summary.arn.isEmpty
      }
    }

    "indicate if it is confirmed" in {
      forAll { summary: SubscriptionSummary ⇒
        val result = summary.isConfirmed
        result shouldBe summary.arn.isDefined
      }
    }

    "convert to a full subscription" in {
      val summaryAndMaybeSubscription =
        for {
          summary ← arbitrary[SubscriptionSummary]
          maybeSubscription ← subscriptionFrom(summary)
        } yield (summary, maybeSubscription)
      forAll(summaryAndMaybeSubscription → "summaryAndMaybeSubscription") { case (summary, maybeSubscription) ⇒
        implicit val sns = mock[StreamingSNSClient]("sns")

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

  private def subscriptionFrom(subscriptionSummary: SubscriptionSummary): Gen[Option[Subscription]] = {
    subscriptionSummary.arn match {
      case Some(arn) ⇒
        for {
          confirmationWasAuthenticated ← arbitrary[Boolean]
          rawMessageDelivery ← arbitrary[Boolean]
          deliveryPolicy ← arbitrary[Option[SubscriptionDeliveryPolicy]]
          effectiveDeliveryPolicy ← arbitrary[Option[SubscriptionDeliveryPolicy]]
        } yield {
          val attrs = SubscriptionAttributes(
            SubscriptionArn.fromArnString(arn),
            subscriptionSummary.endpoint,
            confirmationWasAuthenticated,
            rawMessageDelivery,
            deliveryPolicy,
            effectiveDeliveryPolicy)
          Some(Subscription(attrs.asMap))
        }
      case None ⇒ Gen.const(Option.empty[Subscription])
    }
  }

}
