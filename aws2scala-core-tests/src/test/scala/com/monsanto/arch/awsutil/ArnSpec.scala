package com.monsanto.arch.awsutil

import com.monsanto.arch.awsutil.partitions.Partition
import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.{AwsGen, UtilGen}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks.{whenever ⇒ _, _}
import org.scalatest.prop.TableDrivenPropertyChecks.{forAll ⇒ forAllIn, _}

class ArnSpec extends FreeSpec {
  "an Arn should" - {
    val resourceGen = UtilGen.stringOf(UtilGen.asciiChar, 1, 1024).suchThat(_.nonEmpty)
    "have the correct properties" in {
      forAll(
        arbitrary[Partition] → "partition",
        arbitrary[Arn.Namespace] → "namespace",
        arbitrary[Option[Region]] → "region",
        arbitrary[Option[Account]] → "account",
        resourceGen → "resource"
      ) { (partition, namespace, region, account, aResource) ⇒
        val arn = new Arn(partition, namespace, region, account) {
          override def resource: String = aResource
        }

        arn should have (
          'partition (partition),
          'namespace (namespace),
          'regionOption (region),
          'accountOption (account),
          'resource (aResource)
        )
      }
    }

    "produce the correct ARN string" in {
      forAll(
        arbitrary[Partition] → "partition",
        arbitrary[Arn.Namespace] → "namespace",
        arbitrary[Option[Region]] → "region",
        arbitrary[Option[Account]] → "account",
        resourceGen → "resource"
      ) { (partition, namespace, region, account, aResource) ⇒
        val arn = new Arn(partition, namespace, region, account) {
          override def resource: String = aResource
        }

        arn.value shouldBe s"arn:${partition.id}:${namespace.id}:${region.map(_.name).getOrElse("")}" +
          s":${account.map(_.id).getOrElse("")}:$aResource"
      }
    }

    "have a string representation that is the same as the ARN string" in {
      forAll(
        arbitrary[Partition] → "partition",
        arbitrary[Arn.Namespace] → "namespace",
        arbitrary[Option[Region]] → "region",
        arbitrary[Option[Account]] → "account",
        resourceGen → "resource"
      ) { (partition, namespace, region, account, aResource) ⇒
        val arn = new Arn(partition, namespace, region, account) {
          override def resource: String = aResource
        }

        arn.toString shouldBe arn.value
      }
    }

    "have an extractor" in {
      val partitionAndAccount: Gen[(Partition, Option[Account])] =
        for {
          partition ← arbitrary[Partition]
          account ← Gen.option(AwsGen.account(partition))
        } yield (partition, account)
      forAll(
        partitionAndAccount → "partitionAndAccount",
        arbitrary[Arn.Namespace] → "namespace",
        arbitrary[Option[Region]] → "region",
        resourceGen → "resource"
      ) { (partitionAndAccount, namespace, region, aResource) ⇒
        val (partition, account) = partitionAndAccount
        val arn = new Arn(partition, namespace, region, account) {
          override def resource: String = aResource
        }

        arn.value should matchPattern { case Arn(`partition`, `namespace`, `region`, `account`, `aResource`) ⇒ }
      }
    }
  }

  "the Arn.Namespace enumeration values" - {
    val namespaces = Table("Namespace", Arn.Namespace.values: _*)

    "have string representations equivalent to their ID" in {
      forAllIn(namespaces) { namespace ⇒
        namespace.toString shouldBe namespace.id
      }
    }

    "can generally be round-tripped by their string representation" in {
      forAllIn(namespaces) { namespace ⇒
        Arn.Namespace.fromString(namespace.toString) shouldBe Some(namespace)
      }
    }
  }
}
