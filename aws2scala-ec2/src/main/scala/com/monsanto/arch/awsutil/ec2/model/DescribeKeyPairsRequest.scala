package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.model.{DescribeKeyPairsRequest ⇒ AwsDescribeKeyPairsRequest}

import scala.collection.JavaConverters._

/** Contains the information necessary to request descriptions of one or more key pairs.  Considering using one of the
  * convenience values/methods:
  *
  *  - [[DescribeKeyPairsRequest.allKeyPairs]] to describe all key pairs
  *  - [[DescribeKeyPairsRequest.withName DescribeKeyPairsRequest.withName(String)]] to describe a key pair with the
  *    given name
  *  - [[DescribeKeyPairsRequest.filter(filters:Seq* DescribeKeyPairsRequest.filter(Seq[Filter])]] to describe key
  *    pairs matching the given filters
  *  - [[[DescribeKeyPairsRequest.filter(filters:Map* DescribeKeyPairsRequest.filter(Map[String,Seq[String]])]]] to
  *    describe key pairs matching the given filters specified as a map
  *
  * @param keyNames if non-empty, only describes key pairs that have one of the given names
  * @param filters if non-empty, only describes keys matching the filter
  */
case class DescribeKeyPairsRequest(keyNames: Seq[String], filters: Seq[Filter]) {
  private[ec2] def toAws: AwsDescribeKeyPairsRequest = {
    val aws = new AwsDescribeKeyPairsRequest
    if (keyNames.nonEmpty) {
      aws.setKeyNames(keyNames.asJavaCollection)
    }
    if (filters.nonEmpty) {
      aws.setFilters(filters.map(_.toAws).asJavaCollection)
    }
    aws
  }
}

object DescribeKeyPairsRequest {
  /** A request that will describe all key pairs. */
  val allKeyPairs: DescribeKeyPairsRequest = DescribeKeyPairsRequest(Seq.empty, Seq.empty)

  /** Returns a request that will only describe a key pair with the given name. */
  def withName(keyName: String): DescribeKeyPairsRequest = DescribeKeyPairsRequest(Seq(keyName), Seq.empty)

  /** Returns a request that will only describe key pair that match the given filters. */
  def filter(filters: Seq[Filter]): DescribeKeyPairsRequest = DescribeKeyPairsRequest(Seq.empty, filters)

  /** Returns a request that will only describe key pair that match the given filters. */
  def filter(filters: Map[String,Seq[String]]): DescribeKeyPairsRequest = filter(Filter.fromMap(filters))
}
