package com.monsanto.arch.awsutil.rds

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.rds.AmazonRDSAsync
import com.amazonaws.services.rds.model.{Option ⇒ _, _}
import com.monsanto.arch.awsutil.AWSFlow

import scala.collection.JavaConverters._

class DefaultStreamingRDSClient(client: AmazonRDSAsync) extends StreamingRDSClient {
  override val dbInstanceCreator =
    AWSFlow.simple[CreateDBInstanceRequest,DBInstance](client.createDBInstanceAsync)
      .named("RDS.dbInstanceCreator")

  override val dbInstanceDeleter =
    Flow[(String, Option[String])]
      .map { args ⇒
        val request = new DeleteDBInstanceRequest(args._1)
        request.setSkipFinalSnapshot(args._2.isEmpty)
        args._2.foreach(request.setFinalDBSnapshotIdentifier)
        request
      }
      .via[DBInstance,NotUsed](AWSFlow.simple(client.deleteDBInstanceAsync))
      .named("RDS.dbInstanceDeleter")

  override val rawDbInstanceDescriber =
    Flow[DescribeDBInstancesRequest]
      .via[DescribeDBInstancesResult,NotUsed](AWSFlow.pagedByMarker(client.describeDBInstancesAsync))
      .mapConcat(_.getDBInstances.asScala.toList)
      .named("RDS.rawDbInstanceDescriber")

  override val identifiedDbInstanceDescriber =
    Flow[String]
      .map(id ⇒ new DescribeDBInstancesRequest().withDBInstanceIdentifier(id))
      .via(rawDbInstanceDescriber)
      .named("RDS.identifiedDbInstanceCreator")
}
