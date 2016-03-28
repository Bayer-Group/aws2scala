package com.monsanto.arch.awsutil.testkit

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.partitions.Partition
import com.monsanto.arch.awsutil.regions.Region
import org.scalacheck.Gen

object AwsGen {
  val accountId: Gen[String] = Gen.listOfN(12, Gen.numChar).map(_.mkString)

  def account(partition: Partition): Gen[Account] = accountId.map(id ⇒ Account(id, partition))

  def regionFor(partition: Partition): Gen[Region] =
    partition match {
      case Partition.Aws ⇒
        Gen.oneOf(Region.US_EAST_1, Region.US_WEST_1, Region.US_WEST_2, Region.EU_WEST_1, Region.EU_CENTRAL_1,
          Region.AP_SOUTHEAST_1, Region.AP_SOUTHEAST_2, Region.AP_NORTHEAST_1, Region.AP_NORTHEAST_2, Region.SA_EAST_1)
      case Partition.China ⇒
        Gen.const(Region.CN_NORTH_1)
      case Partition.GovCloud ⇒
        Gen.const(Region.GovCloud)
    }

  def regionFor(account: Account): Gen[Region] = regionFor(account.partition)

  val statementId: Gen[String] = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString).suchThat(_.nonEmpty)
}
