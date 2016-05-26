package com.monsanto.arch.awsutil

import com.monsanto.arch.awsutil.partitions.Partition
import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.{CoreGen, UtilGen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen, Shrink}
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks.{whenever ⇒ _, _}
import org.scalatest.prop.TableDrivenPropertyChecks.{forAll ⇒ forAllIn, _}

class ArnSpec extends FreeSpec {
  "an Arn should" - {
    case class TestArn(testPartition: Partition,
                       testNamespace: Arn.Namespace,
                       testRegion: Option[Region],
                       testAccount: Option[Account],
                       testResource: String) extends Arn(testPartition, testNamespace, testRegion, testAccount) {
      override def resource = testResource
    }
    implicit val arbTestArn: Arbitrary[TestArn] =
      Arbitrary {
        for {
          partition ← arbitrary[Partition]
          namespace ← arbitrary[Arn.Namespace]
          region ← arbitrary[Option[Region]]
          account ← Gen.option(CoreGen.accountId).map(_.map(id ⇒ Account(id, partition)))
          resource ← UtilGen.stringOf(UtilGen.asciiChar, 1, 1024).suchThat(_.nonEmpty)
        } yield TestArn(partition, namespace, region, account, resource)
      }

    implicit val shrinkTestArn: Shrink[TestArn] =
      Shrink { arn ⇒
        Shrink.shrink(arn.testResource).filter(_.nonEmpty).map(x ⇒ arn.copy(testResource = x))
      }

    "have the correct properties" in {
      forAll { arn: TestArn ⇒
        arn should have (
          'partition (arn.testPartition),
          'namespace (arn.testNamespace),
          'regionOption (arn.testRegion),
          'accountOption (arn.testAccount),
          'resource (arn.testResource)
        )
      }
    }

    "produce the correct ARN string" in {
      forAll { arn: TestArn ⇒
        val partition = arn.testPartition.id
        val namespace = arn.testNamespace.id
        val region = arn.testRegion.map(_.name).getOrElse("")
        val account = arn.testAccount.map(_.id).getOrElse("")
        val resource = arn.testResource

        arn.arnString shouldBe s"arn:$partition:$namespace:$region:$account:$resource"
      }
    }

    "have an extractor that returns" - {
      "a generic result when no applicable partial function has been registered" in {
        forAll { arn: TestArn ⇒
          Arn.fromArnString(arn.arnString) shouldBe
            Arn.GenericArn(arn.testPartition, arn.testNamespace, arn.testRegion, arn.testAccount, arn.testResource)
        }
      }

      "a specfic result once a partial function has been registered" in {
        val testMatcher: PartialFunction[Arn.ArnParts, TestArn] = {
          case (partition, namespace, region, account, resource) ⇒ TestArn(partition, namespace, region, account, resource)
        }
        Arn.registerArnPartialFunctions(testMatcher)

        forAll { arn: TestArn ⇒
          Arn.fromArnString(arn.arnString) shouldBe arn
        }
      }
    }
  }

  "the Arn.Namespace enumeration values" - {
    val namespaces = Table("Namespace", Arn.Namespace.values: _*)

    "can generally be round-tripped by their string representation" in {
      forAllIn(namespaces) { namespace ⇒
        namespace.id should matchPattern { case Arn.Namespace.fromId(x) if x == namespace ⇒ }
      }
    }
  }
}
