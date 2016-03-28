package com.monsanto.arch.awsutil.regions

import com.amazonaws.{regions ⇒ aws}
import com.monsanto.arch.awsutil.partitions.Partition
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

private[awsutil] abstract class Region(val toAws: aws.Regions,
                                       val partition: Partition) extends AwsEnumeration[aws.Regions] {
  private val awsRegion = aws.Region.getRegion(toAws)

  def name: String = awsRegion.getName
}

private[awsutil] object Region extends AwsEnumerationCompanion[Region, aws.Regions] {
  /** The AWS GovCloud. */
  case object GovCloud extends Region(aws.Regions.GovCloud, Partition.GovCloud)

  /** US East (N. Virginia). */
  case object US_EAST_1 extends Region(aws.Regions.US_EAST_1, Partition.Aws)

  /** US West (Oregon). */
  case object US_WEST_1 extends Region(aws.Regions.US_WEST_1, Partition.Aws)

  /** US West (N. California). */
  case object US_WEST_2 extends Region(aws.Regions.US_WEST_2, Partition.Aws)

  /** EU West (Ireland). */
  case object EU_WEST_1 extends Region(aws.Regions.EU_WEST_1, Partition.Aws)

  /** EU Central (Frankfurt). */
  case object EU_CENTRAL_1 extends Region(aws.Regions.EU_CENTRAL_1, Partition.Aws)

  /** Asia Pacific (Singapore). */
  case object AP_SOUTHEAST_1 extends Region(aws.Regions.AP_SOUTHEAST_1, Partition.Aws)

  /** Asia Pacific (Sydney). */
  case object AP_SOUTHEAST_2 extends Region(aws.Regions.AP_SOUTHEAST_2, Partition.Aws)

  /** Asia Pacific (Tokyo). */
  case object AP_NORTHEAST_1 extends Region(aws.Regions.AP_NORTHEAST_1, Partition.Aws)

  /** Asia Pacific (Seoul). */
  case object AP_NORTHEAST_2 extends Region(aws.Regions.AP_NORTHEAST_2, Partition.Aws)

  /** South America (Sao Paulo). */
  case object SA_EAST_1 extends Region(aws.Regions.SA_EAST_1, Partition.Aws)

  /** China (Beijing). */
  case object CN_NORTH_1 extends Region(aws.Regions.CN_NORTH_1, Partition.China)

  /** All valid values for the enumeration. */
  override def values: Seq[Region] = Seq(GovCloud, US_EAST_1, US_WEST_1, US_WEST_2, EU_WEST_1, EU_CENTRAL_1,
    AP_SOUTHEAST_1, AP_SOUTHEAST_2, AP_NORTHEAST_1, AP_NORTHEAST_2, SA_EAST_1, CN_NORTH_1)

  /** Gets a region from its name. */
  def fromName(name: String): Option[Region] = values.find(_.name == name)
}
