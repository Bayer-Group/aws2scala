package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.sns.model.AwsConverters._
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ProtocolSpec extends FreeSpec {
  "the Protocol object should" - {
    "round-trip protocol strings" in {
      forAll { protocol: Protocol ⇒
        protocol.asAws.asScala shouldBe protocol
      }
    }

    "fail to recognise bad protocols" in {
      an [IllegalArgumentException] shouldBe thrownBy("foo".asScala)
    }
  }

  "individual protocols should" - {
    "generate SubscriptionEndpoints" in {
      forAll { subscriptionEndpoint: SubscriptionEndpoint ⇒
        val SubscriptionEndpoint(protocol, endpoint) =  subscriptionEndpoint
        val result = protocol(endpoint)
        result shouldBe subscriptionEndpoint
      }
    }
  }
}
