package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}

import scala.collection.JavaConverters._

/** Contains the information necessary to request descriptions of one or more instances.  Considering using one of the
  * convenience values/methods:
  *
  *  - [[DescribeInstancesRequest.allInstances]] to describe all instances
  *  - [[DescribeInstancesRequest.withId DescribeInstancesRequest.withId(String)]] to describe a instance with the
  *    given ID
  *  - [[DescribeInstancesRequest.filter(filters:Seq* DescribeInstancesRequest.filter(Seq[Filter])]] to describe
  *    instances matching the given filters
  *  - [[[DescribeInstancesRequest.filter(filters:Map* DescribeInstancesRequest.filter(Map[String,Seq[String]])]]] to
  *    describe instances matching the given filters specified as a map
  *
  * @param instanceIds if non-empty, only describes instances that have one of the given IDs
  * @param filters if non-empty, only describes keys matching the filter
  */
case class DescribeInstancesRequest(instanceIds: Seq[String], filters: Seq[Filter]) {
  private[ec2] def toAws: aws.DescribeInstancesRequest = {
    val request = new aws.DescribeInstancesRequest
    if (instanceIds.nonEmpty) {
      request.setInstanceIds(instanceIds.asJavaCollection)
    }
    if (filters.nonEmpty) {
      request.setFilters(filters.map(_.toAws).asJavaCollection)
    }
    request
  }
}

object DescribeInstancesRequest {
  /** A request that will describe all instances. */
  val allInstances: DescribeInstancesRequest = DescribeInstancesRequest(Seq.empty, Seq.empty)

  /** Returns a request that will only describe a instance with the given ID. */
  def withId(instanceId: String): DescribeInstancesRequest = DescribeInstancesRequest(Seq(instanceId), Seq.empty)

  /** Returns a request that will only describe instance that match the given filters. */
  def filter(filters: Seq[Filter]): DescribeInstancesRequest = DescribeInstancesRequest(Seq.empty, filters)

  /** Returns a request that will only describe instance that match the given filters. */
  def filter(filters: Map[String,Seq[String]]): DescribeInstancesRequest = filter(Filter.fromMap(filters))
}
