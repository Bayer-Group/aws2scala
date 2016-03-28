package com.monsanto.arch.awsutil.partitions

private[awsutil] sealed abstract class Partition(val id: String) {
  override def toString = id
}

private[awsutil] object Partition {
  case object Aws extends Partition("aws")
  case object China extends Partition("aws-cn")
  case object GovCloud extends Partition("aws-us-gov")

  val values: Seq[Partition] = Seq(Aws, China, GovCloud)

  def fromString(str: String): Option[Partition] = values.find(_.id == str)

  def unapply(str: String): Option[Partition] = fromString(str)
}
