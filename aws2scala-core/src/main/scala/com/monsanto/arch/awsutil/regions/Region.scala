package com.monsanto.arch.awsutil.regions

import com.monsanto.arch.awsutil.partitions.Partition

sealed abstract class Region(val name: String, val partition: Partition)

object Region {
  /** The AWS GovCloud. */
  case object GovCloud extends Region("us-gov-west-1", Partition.GovCloud)

  /** US East (N. Virginia). */
  case object US_EAST_1 extends Region("us-east-1", Partition.Aws)

  /** US East (Ohio). */
  case object US_EAST_2 extends Region("us-east-2", Partition.Aws)

  /** US West (Oregon). */
  case object US_WEST_1 extends Region("us-west-1", Partition.Aws)

  /** US West (N. California). */
  case object US_WEST_2 extends Region("us-west-2", Partition.Aws)

  /** EU West (Ireland). */
  case object EU_WEST_1 extends Region("eu-west-1", Partition.Aws)

  /** EU West (London). */
  case object EU_WEST_2 extends Region("eu-west-2", Partition.Aws)

  /** EU West (Paris). */
  case object EU_WEST_3 extends Region("eu-west-3", Partition.Aws)

  /** EU Central (Frankfurt). */
  case object EU_CENTRAL_1 extends Region("eu-central-1", Partition.Aws)

  /** Asia Pacific (Singapore). */
  case object AP_SOUTHEAST_1 extends Region("ap-southeast-1", Partition.Aws)

  /** Asia Pacific (Sydney). */
  case object AP_SOUTHEAST_2 extends Region("ap-southeast-2", Partition.Aws)

  /** Asia Pacific (Mumbai). */
  case object AP_SOUTH_1 extends Region("ap-south-1", Partition.Aws)

  /** Asia Pacific (Tokyo). */
  case object AP_NORTHEAST_1 extends Region("ap-northeast-1", Partition.Aws)

  /** Asia Pacific (Seoul). */
  case object AP_NORTHEAST_2 extends Region("ap-northeast-2", Partition.Aws)

  /** South America (Sao Paulo). */
  case object SA_EAST_1 extends Region("sa-east-1", Partition.Aws)

  /** China (Beijing). */
  case object CN_NORTH_1 extends Region("cn-north-1", Partition.China)

  /** China (Ningxia). */
  case object CN_NORTHWEST_1 extends Region("cn-northwest-1", Partition.China)

  /** Canada (Central). */
  case object CA_CENTRAL_1 extends Region("ca-central-1", Partition.Aws)

  /** All valid values for the enumeration. */
  val values: Seq[Region] = Seq(GovCloud, US_EAST_1, US_EAST_2, US_WEST_1, US_WEST_2, EU_WEST_1, EU_WEST_2, EU_WEST_3, EU_CENTRAL_1,
    AP_SOUTHEAST_1, AP_SOUTHEAST_2, AP_SOUTH_1, AP_NORTHEAST_1, AP_NORTHEAST_2, SA_EAST_1, CN_NORTH_1, CN_NORTHWEST_1,
    CA_CENTRAL_1)

  /** Extracts a region from its name. */
  def unapply(str: String): Option[Region] = values.find(_.name == str)
}
