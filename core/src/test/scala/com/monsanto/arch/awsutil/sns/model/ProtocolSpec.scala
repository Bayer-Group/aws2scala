package com.monsanto.arch.awsutil.sns.model

import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ProtocolSpec extends FreeSpec {
  "the Protocol object should" - {
    "round-trip protocol strings" in {
      forAll(SNSGen.protocol → "protocol") { protocol ⇒
        Protocol(protocol.toAws) shouldBe protocol
      }
    }

    "fail to recognise bad protocols" in {
      an [IllegalArgumentException] shouldBe thrownBy(Protocol("foo"))
    }
  }

  "individual protocols should" - {
    "generate SubscriptionEndpoints" in {
      forAll(SNSGen.subscriptionEndpoint → "subscriptionEndpoint") { subscriptionEndpoint ⇒
        val SubscriptionEndpoint(protocol, endpoint) =  subscriptionEndpoint
        val result = protocol(endpoint)
        result shouldBe subscriptionEndpoint
      }
    }
  }
}
