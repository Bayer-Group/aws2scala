package com.monsanto.arch.awsutil.kms.model

import com.monsanto.arch.awsutil.converters.KmsConverters._
import com.monsanto.arch.awsutil.kms.KMS
import com.monsanto.arch.awsutil.testkit.KmsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class KeyMetadataSpec extends FreeSpec {
  // ensure that key ARNs will parse
  KMS.init()

  "a KeyMetadata should" - {
    "convert to the correct AWS value" in {
      forAll { metadata: KeyMetadata ⇒
        metadata.asAws.getEnabled shouldBe metadata.enabled
        metadata.asAws should have(
          'KeyId (metadata.id),
          'Arn (metadata.arn.arnString),
          'AWSAccountId (metadata.account.id),
          'CreationDate (metadata.creationDate),
          'DeletionDate (metadata.deletionDate.orNull),
          // the enabled field isn't detected for some reason
          //'Enabled (java.lang.Boolean.valueOf(metadata.enabled)),
          'KeyState (metadata.state.name),
          'KeyUsage (metadata.usage.name)
        )
      }
    }

    "round trip through its AWS equivalent" in {
      forAll { metadata: KeyMetadata ⇒
        metadata.asAws.asScala shouldBe metadata
      }
    }
  }
}
