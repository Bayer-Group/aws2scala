package com.monsanto.arch.awsutil.partitions

/** AWS is divided into several partitions, each of which is completely unconnected from the rest.  Note that this is
  * a higher-level grouping than a region.
  *
  * @param id the unique identifier for the region, e.g. `aws` for general AWS and `aws-cn` for AWS China
  */
sealed abstract class Partition(val id: String) {
  /** The string representation is the ID. */
  override def toString: String = id
}

object Partition {
  /** The general AWS partition. */
  case object Aws extends Partition("aws")

  /** The China AWS partition. */
  case object China extends Partition("aws-cn")

  /** The U.S. Government's AWS partition. */
  case object GovCloud extends Partition("aws-us-gov")

  /** All possible partitions. */
  val values: Seq[Partition] = Seq(Aws, China, GovCloud)

  /** Extractor for partitions based on ID. */
  def unapply(str: String): Option[Partition] = Partition.values.find(_.id == str)
}
