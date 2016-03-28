package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

/** Describes a product code.
  *
  * @param id the product code
  * @param `type` the type of product code
  */
case class ProductCode(id: String, `type`: ProductCode.Type)

object ProductCode {
  private[ec2] def fromAws(code: aws.ProductCode): ProductCode =
    ProductCode(code.getProductCodeId, Type.fromString(code.getProductCodeType).get)

  sealed abstract class Type(val toAws: aws.ProductCodeValues) extends AwsEnumeration[aws.ProductCodeValues]

  object Type extends AwsEnumerationCompanion[Type,aws.ProductCodeValues] {
    case object Devpay extends Type(aws.ProductCodeValues.Devpay)
    case object Marketplace extends Type(aws.ProductCodeValues.Marketplace)

    override def values: Seq[Type] = Seq(Devpay, Marketplace)
  }
}
