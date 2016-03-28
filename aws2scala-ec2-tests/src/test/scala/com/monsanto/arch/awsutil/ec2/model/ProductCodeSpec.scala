package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import com.monsanto.arch.awsutil.testkit.Ec2ScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ProductCodeSpec extends FreeSpec with AwsEnumerationBehaviours {
  "a ProductCode should" - {
    "be constructible from its AWS equivalent" in {
      forAll { productCode: ProductCode ⇒
        val awsProductCode = new aws.ProductCode()
          .withProductCodeId(productCode.id)
          .withProductCodeType(productCode.`type`.toAws)
        ProductCode.fromAws(awsProductCode) shouldBe productCode
      }
    }
  }

  "the ProductCode.Type enumeration" - {
    behave like anAwsEnumeration(ProductCode.Type)
  }
}
