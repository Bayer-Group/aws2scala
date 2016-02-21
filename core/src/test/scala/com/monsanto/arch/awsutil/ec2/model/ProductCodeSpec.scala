package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.test.AwsEnumerationBehaviours
import org.scalatest.FreeSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import org.scalatest.Matchers._

class ProductCodeSpec extends FreeSpec with AwsEnumerationBehaviours {
  "a ProductCode should" - {
    "be constructible from its AWS equivalent" in {
      forAll { args: EC2Gen.ProductCodeArgs ⇒
        ProductCode.fromAws(args.toAws) shouldBe args.toProductCode
      }
    }
  }

  "the ProductCode.Type enumeration" - {
    behave like anAwsEnumeration(ProductCode.Type)
  }
}
