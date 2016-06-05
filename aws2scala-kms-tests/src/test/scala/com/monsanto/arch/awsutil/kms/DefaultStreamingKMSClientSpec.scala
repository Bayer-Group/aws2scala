package com.monsanto.arch.awsutil.kms

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.kms.{AWSKMSAsync, model ⇒ aws}
import com.monsanto.arch.awsutil.converters.KmsConverters._
import com.monsanto.arch.awsutil.kms.model.{CreateKeyWithAliasRequest, KeyMetadata}
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.{AwsMockUtils, Materialised}
import com.monsanto.arch.awsutil.testkit.KmsScalaCheckImplicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class DefaultStreamingKMSClientSpec extends FreeSpec with MockFactory with Materialised with AwsMockUtils {
  "the DefaultStreamingKMSClient should provide" - {
    "a key+alias creation flow" in {
      forAll { (request: CreateKeyWithAliasRequest, metadata: KeyMetadata) ⇒
        val kms = mock[AWSKMSAsync]("kms")
        val streaming = new DefaultStreamingKMSClient(kms)

        (kms.createKeyAsync(_: aws.CreateKeyRequest, _: AsyncHandler[aws.CreateKeyRequest, aws.CreateKeyResult]))
          .expects(whereRequest { r ⇒
            r shouldBe request.asAws
            true
          })
          .withAwsSuccess(new aws.CreateKeyResult().withKeyMetadata(metadata.asAws))

        (kms.createAliasAsync(_: aws.CreateAliasRequest, _: AsyncHandler[aws.CreateAliasRequest, aws.CreateAliasResult]))
          .expects(whereRequest { r ⇒
            r should have (
              'AliasName (
                if (request.alias.startsWith("alias/")) request.alias
                else s"alias/${request.alias}"
              ),
              'TargetKeyId (metadata.id)
            )
            true
          })
          .withVoidAwsSuccess()

        val result = Source.single(request).via(streaming.keyWithAliasCreator).runWith(Sink.head).futureValue
        result shouldBe metadata
      }
    }
  }
}
