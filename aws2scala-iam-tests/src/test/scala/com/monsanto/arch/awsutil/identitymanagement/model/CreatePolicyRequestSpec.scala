package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.converters.IamConverters._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class CreatePolicyRequestSpec extends FreeSpec {
  "a CreatePolicyRequest should" - {
    "transform to the correct AWS request" in {
      forAll { request: CreatePolicyRequest ⇒
        request.asAws should have (
          'Description (request.description.orNull),
          'Path (request.path.pathString),
          'PolicyDocument (request.document.toJson),
          'PolicyName (request.name)
        )
      }
    }

    "round-trip via an AWS request" in {
      forAll { request: CreatePolicyRequest ⇒
        request.asAws.asScala shouldBe request
      }
    }
  }
}
