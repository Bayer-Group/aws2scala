package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.sns.model.Platform._
import org.scalacheck.Gen
import org.scalatest.FreeSpec
import org.scalatest.Inside._
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalatest.prop.TableDrivenPropertyChecks.{Table, forAll ⇒ forAllIn}

class PlatformSpec extends FreeSpec {
  private val namesAndPlatforms = Table(
    ("name", "platform"),
    ("ADM", ADM),
    ("APNS", APNSProduction),
    ("APNS_SANDBOX", APNSDevelopment),
    ("BAIDU", Baidu),
    ("GCM", GCM),
    ("MPNS", MPNS),
    ("WNS", WNS)
  )

  private val validNames = namesAndPlatforms.map(_._1).toSet

  private val invalidName: Gen[String] = Gen.alphaStr.suchThat(n ⇒ !validNames.contains(n))

  "the Platform object" - {
    "can pattern match on individual names" in {
      forAllIn(namesAndPlatforms) { (name, platform) ⇒
        inside(name) {
          case Platform(p) ⇒ p shouldBe platform
        }
      }
    }

    "can apply a name and get a platform" in {
      forAllIn(namesAndPlatforms) { (name, platform) ⇒
        Platform(name) shouldBe platform
      }
    }

    "will fail to pattern match with arbitrary strings" in {
      forAll(invalidName → "invalidName") { invalidName ⇒
        invalidName should not matchPattern { case Platform(_) ⇒ }
      }
    }

    "will fail to apply an invalid name" in {
      forAll(invalidName → "invalidName") { invalidName ⇒
        an [IllegalArgumentException] shouldBe thrownBy (Platform(invalidName))
      }
    }
  }

  "the ADM object" - {
    "can create credentials" in {
      forAll(Gen.alphaStr → "clientId", Gen.alphaStr → "clientSecret") { (clientId, clientSecret) ⇒
        val result = ADM(clientId, clientSecret)
        result shouldBe PlatformApplicationCredentials(ADM, clientId, clientSecret)
      }
    }
  }

  "the APNSDevelopment object" - {
    "can create credentials" in {
      forAll(Gen.alphaStr → "certificate", Gen.alphaStr → "key") { (certificate, key) ⇒
        val result = APNSDevelopment(certificate, key)
        result shouldBe PlatformApplicationCredentials(APNSDevelopment, certificate, key)
      }
    }
  }

  "the APNSProduction object" - {
    "can create credentials" in {
      forAll(Gen.alphaStr → "certificate", Gen.alphaStr → "key") { (certificate, key) ⇒
        val result = APNSProduction(certificate, key)
        result shouldBe PlatformApplicationCredentials(APNSProduction, certificate, key)
      }
    }
  }

  "the Baidu object" - {
    "can create credentials" in {
      forAll(Gen.alphaStr → "apiKey", Gen.alphaStr → "secretKey") { (apiKey, secretKey) ⇒
        val result = Baidu(apiKey, secretKey)
        result shouldBe PlatformApplicationCredentials(Baidu, apiKey, secretKey)
      }
    }
  }

  "the MPNS object" - {
    "can create authenticated credentials" in {
      forAll(Gen.alphaStr → "tlsCertificateChain", Gen.alphaStr → "privateKey") { (tlsCertificationChain, privateKey) ⇒
        val result = MPNS(tlsCertificationChain, privateKey)
        result shouldBe PlatformApplicationCredentials(MPNS, tlsCertificationChain, privateKey)
      }
    }

    "can create unauthenticated credentials" in {
      val result = MPNS()
      result shouldBe PlatformApplicationCredentials(MPNS, "", "")
    }
  }

  "the WNS object" - {
    "can create credentials" in {
      forAll(Gen.alphaStr → "packageSecurityIdentifier", Gen.alphaStr → "secretKey") { (packageSecurityIdentifier, secretKey) ⇒
        val result = WNS(packageSecurityIdentifier, secretKey)
        result shouldBe PlatformApplicationCredentials(WNS, packageSecurityIdentifier, secretKey)
      }
    }
  }
}
