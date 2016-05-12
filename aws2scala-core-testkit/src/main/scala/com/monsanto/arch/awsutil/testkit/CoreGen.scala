package com.monsanto.arch.awsutil.testkit

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.partitions.Partition
import com.monsanto.arch.awsutil.regions.Region
import org.scalacheck.Gen

/** Generators useful for the core ''aws2scala'' module. */
object CoreGen {
  /** Generates an arbitrary account ID (a 12 digit string). */
  val accountId: Gen[String] = Gen.listOfN(12, Gen.numChar).map(_.mkString).suchThat(_.length == 12)

  /** Generates an [[com.monsanto.arch.awsutil.Account Account]] within the given
    * [[com.monsanto.arch.awsutil.partitions.Partition Partition]].
    */
  def account(partition: Partition): Gen[Account] = accountId.map(id ⇒ Account(id, partition))

  /** Generates an arbitrary [[com.monsanto.arch.awsutil.regions.Region Region]] located within the given
    * [[com.monsanto.arch.awsutil.partitions.Partition Partition]].
    */
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

  /** Generates an arbitrary [[com.monsanto.arch.awsutil.regions.Region Region]] located within the given
    * [[com.monsanto.arch.awsutil.partitions.Partition Partition]] of the given [[Account]].
    */
  def regionFor(account: Account): Gen[Region] = regionFor(account.partition)

  /** Generates an arbitrary IAM policy statement identifier. */
  val statementId: Gen[String] = Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString).suchThat(_.nonEmpty)

  /** Generates a valid IAM name for user, roles, and more. */
  val iamName: Gen[String] = UtilGen.stringOf(UtilGen.extendedWordChar, 1, 64).suchThat(_.nonEmpty)

  /** Generates a SAML provider name. */
  val samlProviderName: Gen[String] = {
    val samlProviderNameChar = Gen.oneOf(('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') :+ '.' :+ '_' :+ '-')
    UtilGen.stringOf(samlProviderNameChar, 1, 128).suchThat(_.nonEmpty)
  }

  /** Generates an STS assumed role session name. */
  val assumedRoleSessionName: Gen[String] = UtilGen.stringOf(UtilGen.extendedWordChar, 2, 64).suchThat(_.length > 1)
}
