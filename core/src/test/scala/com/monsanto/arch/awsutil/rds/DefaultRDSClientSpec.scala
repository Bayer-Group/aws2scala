package com.monsanto.arch.awsutil.rds

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.rds.AmazonRDSAsync
import com.amazonaws.services.rds.model._
import com.monsanto.arch.awsutil.test.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.{AwsMockUtils, Materialised}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._

import scala.collection.JavaConverters._

class DefaultRDSClientSpec  extends FreeSpec with MockFactory with Materialised with AwsMockUtils {
  "The RDS client" - {
    "can create a DB instance" in withFixture { f ⇒
      val dbInstance = new DBInstance()
      val request = new CreateDBInstanceRequest()

      (f.rds.createDBInstanceAsync(_: CreateDBInstanceRequest, _: AsyncHandler[CreateDBInstanceRequest,DBInstance]))
        .expects(request, *)
        .withAwsSuccess(dbInstance)

      val result = f.asyncClient.createDBInstance(request).futureValue
      result shouldBe dbInstance
    }

    "can delete a DB instance" - {
      "without a final snapshot" in withFixture { f ⇒
        val dbInstanceId = "forty-two"
        val dbInstance = new DBInstance().withDBInstanceIdentifier(dbInstanceId)

        (f.rds.deleteDBInstanceAsync(_: DeleteDBInstanceRequest, _: AsyncHandler[DeleteDBInstanceRequest,DBInstance]))
          .expects(whereRequest(r ⇒
            r.getDBInstanceIdentifier == dbInstanceId &&
              r.getSkipFinalSnapshot == true &&
              r.getFinalDBSnapshotIdentifier == null
          ))
          .withAwsSuccess(dbInstance)

        val result = f.asyncClient.deleteDBInstance(dbInstanceId).futureValue
        result shouldBe dbInstance
      }

      "with a final snapshot" in withFixture { f ⇒
        val dbInstanceId = "forty-two"
        val dbSnapshotId = "forty-two-snapshot"
        val dbInstance = new DBInstance().withDBInstanceIdentifier(dbInstanceId)

        (f.rds.deleteDBInstanceAsync(_: DeleteDBInstanceRequest, _: AsyncHandler[DeleteDBInstanceRequest,DBInstance]))
          .expects(whereRequest(r ⇒
            r.getDBInstanceIdentifier == dbInstanceId &&
              r.getSkipFinalSnapshot == false &&
              r.getFinalDBSnapshotIdentifier == dbSnapshotId
          ))
          .withAwsSuccess(dbInstance)

        val result = f.asyncClient.deleteDBInstance(dbInstanceId, dbSnapshotId).futureValue
        result shouldBe dbInstance
      }
    }

    "list DB instances" in withFixture { f ⇒
      val instances = Seq.tabulate(20)(i ⇒ new DBInstance().withDBName(s"instance$i"))

      val token = "token"
      (f.rds.describeDBInstancesAsync(_: DescribeDBInstancesRequest, _: AsyncHandler[DescribeDBInstancesRequest, DescribeDBInstancesResult]))
        .expects(whereRequest(r ⇒
          (r.getMarker == null || r.getMarker == token) &&
            r.getFilters.isEmpty &&
            r.getDBInstanceIdentifier == null
        ))
        .withAwsSuccess { r ⇒
          val first = r.getMarker == null
          val pagedInstances = if (first) instances.take(10) else instances.drop(10)
          val result = new DescribeDBInstancesResult().withDBInstances(pagedInstances.asJavaCollection)
          if(first) {
            result.setMarker(token)
          }
          result
        }
        .twice()

      val result = f.asyncClient.describeDBInstances().futureValue
      result shouldBe instances
    }

    "describe a specific DB instance" in withFixture { f ⇒
      val instanceId = "instance0"
      val instance = new DBInstance().withDBName("test").withDBInstanceIdentifier(instanceId)

      val token = "token"
      (f.rds.describeDBInstancesAsync(_: DescribeDBInstancesRequest, _: AsyncHandler[DescribeDBInstancesRequest, DescribeDBInstancesResult]))
        .expects(whereRequest(r ⇒
          (r.getMarker == null || r.getMarker == token) &&
            r.getFilters.isEmpty &&
            r.getDBInstanceIdentifier == instanceId
        ))
        .withAwsSuccess(new DescribeDBInstancesResult().withDBInstances(Seq(instance).asJavaCollection))

      val result = f.asyncClient.describeDBInstance(instanceId).futureValue
      result shouldBe instance
    }
  }

  private def withFixture(test: BasicFixture => Any): Unit = {
    val rds = mock[AmazonRDSAsync]("rds")
    val streamingClient = new DefaultStreamingRDSClient(rds)
    val asyncClient = new DefaultAsyncRDSClient(streamingClient)
    test(BasicFixture(rds, streamingClient, asyncClient))
  }

  case class BasicFixture(rds: AmazonRDSAsync,
                          streamingClient: DefaultStreamingRDSClient,
                          asyncClient: DefaultAsyncRDSClient)
}
