package com.monsanto.arch.awsutil.rds

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.rds.model.{CreateDBInstanceRequest, DBInstance, DescribeDBInstancesRequest}
import com.monsanto.arch.awsutil.StreamingAwsClient

/** Akka streams-based interface to Amazon’s relational database service.
  *
  * @author Jorge Montero
  */
trait StreamingRDSClient extends StreamingAwsClient {
  /** Returns an Akka flow that will request creation of a new DB instance. */
  def dbInstanceCreator: Flow[CreateDBInstanceRequest, DBInstance, NotUsed]

  /** Returns an Akka flow that will delete a given db instance.  Each input is a 2-tuple consisting of the DB instance
    * identifier to delete and an optional final DB snapshot identifier.  if no snapshot identifier is provided, none
    * will be taken.
    */
  def dbInstanceDeleter: Flow[(String, Option[String]), DBInstance, NotUsed]

  /** Returns an Akka flow that performs an arbitrary DB instance description request, emitting all matching
    * instances.
    */
  def rawDbInstanceDescriber: Flow[DescribeDBInstancesRequest, DBInstance, NotUsed]

  /** Returns an Akka flow that given a DB instance identifier will return a description for the instance. */
  def identifiedDbInstanceDescriber: Flow[String, DBInstance, NotUsed]
}
