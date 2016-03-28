package com.monsanto.arch.awsutil.ec2

import java.util.concurrent.{Future ⇒ JFuture}

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.ec2.model.AwsConverters._
import com.monsanto.arch.awsutil.ec2.model._
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Materialised
import com.monsanto.arch.awsutil.test_support.Samplers.EnhancedGen
import com.monsanto.arch.awsutil.testkit.Ec2Gen
import com.monsanto.arch.awsutil.testkit.Ec2ScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

import scala.collection.JavaConverters._

class StreamingEC2ClientSpec extends FreeSpec with Materialised {
  "the default streaming EC2 client can" - {
    "create key pairs" in {
      forAll { keyPair: KeyPair ⇒
        val name = keyPair.name

        val ec2 = new FakeAmazonEC2Async {
          override def createKeyPairAsync(createKeyPairRequest: aws.CreateKeyPairRequest,
                                          asyncHandler: AsyncHandler[aws.CreateKeyPairRequest, aws.CreateKeyPairResult]) = {
            createKeyPairRequest should have ('keyName (name))
            val result = new aws.CreateKeyPairResult().withKeyPair(keyPair.toAws)
            asyncHandler.onSuccess(createKeyPairRequest, result)
            null.asInstanceOf[JFuture[aws.CreateKeyPairResult]]
          }
        }
        val streaming = new DefaultStreamingEC2Client(ec2)

        val result = Source.single(name).via(streaming.keyPairCreator).runWith(Sink.head).futureValue
        result shouldBe keyPair
      }
    }

    "delete key pairs" in {
      forAll(Ec2Gen.keyName → "keyName") { name ⇒
        val ec2 = new FakeAmazonEC2Async {
          override def deleteKeyPairAsync(deleteKeyPairRequest: aws.DeleteKeyPairRequest,
                                          asyncHandler: AsyncHandler[aws.DeleteKeyPairRequest, Void]) = {
            deleteKeyPairRequest should have ('keyName (name))
            asyncHandler.onSuccess(deleteKeyPairRequest, null.asInstanceOf[Void])
            null.asInstanceOf[JFuture[Void]]
          }
        }
        val streaming = new DefaultStreamingEC2Client(ec2)

        val result = Source.single(name).via(streaming.keyPairDeleter).runWith(Sink.head).futureValue
        result shouldBe name
      }
    }

    "describe key pairs" in {
      forAll { request: DescribeKeyPairsRequest ⇒
        val keyPairInfo = Gen.resize(10, arbitrary[Seq[KeyPairInfo]]).reallySample
        val ec2 = new FakeAmazonEC2Async {
          override def describeKeyPairsAsync(describeKeyPairsRequest: aws.DescribeKeyPairsRequest,
                                             asyncHandler: AsyncHandler[aws.DescribeKeyPairsRequest, aws.DescribeKeyPairsResult]): JFuture[aws.DescribeKeyPairsResult] = {
            describeKeyPairsRequest shouldBe request.toAws
            val awsKeyPairInfo = keyPairInfo.map(_.toAws).asJavaCollection
            val result = new aws.DescribeKeyPairsResult().withKeyPairs(awsKeyPairInfo)
            asyncHandler.onSuccess(describeKeyPairsRequest, result)
            null
          }
        }
        val streaming = new DefaultStreamingEC2Client(ec2)

        val result = Source.single(request).via(streaming.keyPairsDescriber).runWith(Sink.seq).futureValue
        result shouldBe keyPairInfo
      }
    }

    "describe instances" in {
      forAll { request: DescribeInstancesRequest ⇒
        val reservations = Gen.resize(10, arbitrary[Seq[Reservation]]).reallySample
        val ec2 = new FakeAmazonEC2Async {
          override def describeInstancesAsync(describeInstancesRequest: aws.DescribeInstancesRequest,
                                              asyncHandler: AsyncHandler[aws.DescribeInstancesRequest, aws.DescribeInstancesResult]): JFuture[aws.DescribeInstancesResult] = {
            describeInstancesRequest shouldBe request.toAws
            val awsReservations = reservations.map(_.toAws).asJavaCollection
            val result = new aws.DescribeInstancesResult().withReservations(awsReservations)
            asyncHandler.onSuccess(describeInstancesRequest, result)
            null
          }
        }
        val streaming = new DefaultStreamingEC2Client(ec2)

        val result = Source.single(request).via(streaming.instancesDescriber).runWith(Sink.seq).futureValue
        result shouldBe reservations
      }
    }
  }
}
