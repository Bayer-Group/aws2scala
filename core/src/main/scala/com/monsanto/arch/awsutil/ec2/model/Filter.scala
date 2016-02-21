package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.model.{Filter ⇒ AwsFilter}

import scala.collection.JavaConverters._

/** A filter is a name and values pair used to return a more specific list of results by matching criteria such as
  * tags, attributes, or IDs.
  *
  * @param name the name of the filter
  * @param values the values for which to filter
  */
case class Filter(name: String, values: Seq[String]) {
  private[ec2] def toAws: AwsFilter = new AwsFilter(name, values.asJava)
}

object Filter {
  /** Constructs a sequence of filters from the values in a map. */
  def fromMap(filters: Map[String, Seq[String]]): Seq[Filter] =
    filters.map(filter ⇒ Filter(filter._1, filter._2)).toSeq
}
