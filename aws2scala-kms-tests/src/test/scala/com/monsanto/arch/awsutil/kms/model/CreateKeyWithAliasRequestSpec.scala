package com.monsanto.arch.awsutil.kms.model

import com.monsanto.arch.awsutil.converters.KmsConverters._
import com.monsanto.arch.awsutil.testkit.KmsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class CreateKeyWithAliasRequestSpec extends FreeSpec {
  "a CreateKeyWithAliasRequest should" - {
    "convert to a correct AWS CreateKeyRequest object" in {
      forAll { request: CreateKeyWithAliasRequest ⇒
        request.asAws should have (
          'Description (request.description.orNull),
          'KeyUsage (request.keyUsage.name),
          'Policy (request.policy.map(_.toJson).orNull)
        )
      }
    }
  }
}
