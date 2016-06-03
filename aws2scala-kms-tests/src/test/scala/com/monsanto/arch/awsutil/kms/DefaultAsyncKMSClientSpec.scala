package com.monsanto.arch.awsutil.kms

import com.monsanto.arch.awsutil.kms.model.{CreateKeyWithAliasRequest, KeyMetadata, KeyUsage}
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.{FlowMockUtils, Materialised}
import com.monsanto.arch.awsutil.testkit.KmsGen
import com.monsanto.arch.awsutil.testkit.KmsScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DefaultAsyncKMSClientSpec extends FreeSpec with MockFactory with Materialised with FlowMockUtils {
  "the default asynchronous KMS client should" - {
    "create keys with aliases when" - {
      "only the alias is specified" in {
        forAll(
          KmsGen.keyAlias → "alias",
          arbitrary[KeyMetadata] → "metadata"
        ) { (alias, metadata) ⇒
          val streaming = mock[StreamingKMSClient]("streaming")
          val async = new DefaultAsyncKMSClient(streaming)

          (streaming.keyWithAliasCreator _)
            .expects()
            .returningFlow(
              CreateKeyWithAliasRequest(alias, None, None, KeyUsage.EncryptDecrypt, None),
              metadata)

          val result = async.createKey(alias).futureValue
          result shouldBe metadata
        }
      }

      "an alias and a description" in {
        forAll(
          KmsGen.keyAlias → "alias",
          arbitrary[String] → "description",
          arbitrary[KeyMetadata] → "metadata"
        ) { (alias, description, metadata) ⇒
          val streaming = mock[StreamingKMSClient]("streaming")
          val async = new DefaultAsyncKMSClient(streaming)

          (streaming.keyWithAliasCreator _)
            .expects()
            .returningFlow(
              CreateKeyWithAliasRequest(alias, None, Some(description), KeyUsage.EncryptDecrypt, None),
              metadata)

          val result = async.createKey(alias, description).futureValue
          result shouldBe metadata
        }
      }

      "an arbitrary request" in {
        forAll { (request: CreateKeyWithAliasRequest, metadata: KeyMetadata) ⇒
          val streaming = mock[StreamingKMSClient]("streaming")
          val async = new DefaultAsyncKMSClient(streaming)

          (streaming.keyWithAliasCreator _)
            .expects()
            .returningFlow(request, metadata)

          val result = async.createKey(request).futureValue
          result shouldBe metadata
        }
      }
    }
  }
}
