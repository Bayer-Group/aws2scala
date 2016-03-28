package com.monsanto.arch.awsutil.rds

import com.amazonaws.services.rds.model.{CreateDBInstanceRequest, DBInstance}
import com.monsanto.arch.awsutil.rds.AsyncRDSClient.Implicits._
import com.monsanto.arch.awsutil.test_support.AwsScalaFutures._
import com.monsanto.arch.awsutil.test_support.{AwsIntegrationSpec, IntegrationCleanup, IntegrationTest, TestDefaults}
import com.typesafe.scalalogging.StrictLogging
import org.scalactic.Equality
import org.scalatest.FreeSpec
import org.scalatest.Matchers._

@IntegrationTest
class RDSClientIntegrationSpec extends FreeSpec with AwsIntegrationSpec with StrictLogging with IntegrationCleanup {
  val streamingClient = awsClient.streaming(RDS)
  val asyncClient = awsClient.async(RDS)

  val testPrefix = "aws2scala-it-rds"
  val dbInstanceId = s"$testPrefix-$testId"

  "the RDS client" - {
    "can create DB instances" in {
      val request = new CreateDBInstanceRequest()
        .withDBName("aws2scalaRdsTest")
        .withDBInstanceIdentifier(dbInstanceId)
        .withDBInstanceClass("db.t2.micro")
        .withEngine("postgres")
        .withMasterUsername("missy")
        .withMasterUserPassword("changeMeNow!")
        .withAllocatedStorage(5)
        .withTags(TestDefaults.Tags)
        .withCopyTagsToSnapshot(true)

      logger.info(s"Creating DB instance: $dbInstanceId")
      val result = asyncClient.createDBInstance(request).futureValue
      result.getDBInstanceIdentifier shouldBe dbInstanceId
      result.getDBInstanceStatus shouldBe "creating"
    }

    "can describe db instances" - {
      val theDbInstanceId = new Equality[DBInstance] {
        override def areEqual(a: DBInstance, b: Any) = {
          b match {
            case id: String ⇒ a.getDBInstanceIdentifier == id
          }
        }
      }

      "all of them" in {
        val result = asyncClient.describeDBInstances().futureValue
        (result should contain (dbInstanceId)) (decided by theDbInstanceId)
      }

      "by identifier" in {
        val result = asyncClient.describeDBInstance(dbInstanceId).futureValue
        result.getDBInstanceIdentifier shouldBe dbInstanceId
        result.getDBInstanceStatus shouldBe "creating"
      }
    }

    "can delete DB instances" in {
      logger.info(s"Deleting DB instance: $dbInstanceId")
      val result = asyncClient.deleteDBInstance(dbInstanceId).futureValue
      result.getDBInstanceIdentifier shouldBe dbInstanceId
      result.getDBInstanceStatus shouldBe "deleting"
    }

    behave like cleanupDBInstances(testPrefix)
  }
}
