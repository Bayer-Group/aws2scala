package com.monsanto.arch.awsutil.ec2

import java.util.concurrent.{Future ⇒ JFuture}

import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.Materialised
import com.monsanto.arch.awsutil.ec2.model.EC2Gen
import com.monsanto.arch.awsutil.test.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test.Samplers.EnhancedGen
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

import scala.collection.JavaConverters._

class StreamingEC2ClientSpec extends FreeSpec with Materialised {
  "the default streaming EC2 client can" - {
    "create key pairs" in {
      forAll { args: EC2Gen.KeyPairArgs ⇒
        val keyPair = args.toKeyPair
        val name = keyPair.name

        val ec2 = new FakeAmazonEC2Async {
          override def createKeyPairAsync(createKeyPairRequest: aws.CreateKeyPairRequest,
                                          asyncHandler: AsyncHandler[aws.CreateKeyPairRequest, aws.CreateKeyPairResult]) = {
            createKeyPairRequest should have ('keyName (name))
            val kp = new aws.KeyPair()
              .withKeyName(name)
              .withKeyFingerprint(keyPair.fingerprint)
              .withKeyMaterial(keyPair.key)
            val result = new aws.CreateKeyPairResult().withKeyPair(kp)
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
      forAll { arg: EC2Gen.KeyName ⇒
        val name = arg.value
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
      forAll { (requestArgs: EC2Gen.DescribeKeyPairRequestArgs, resultArgs: Seq[EC2Gen.KeyPairInfoArgs]) ⇒
        val request = requestArgs.toRequest
        val keyPairInfo = resultArgs.map(_.toKeyPairInfo)
        val ec2 = new FakeAmazonEC2Async {
          override def describeKeyPairsAsync(describeKeyPairsRequest: aws.DescribeKeyPairsRequest,
                                             asyncHandler: AsyncHandler[aws.DescribeKeyPairsRequest, aws.DescribeKeyPairsResult]): JFuture[aws.DescribeKeyPairsResult] = {
            describeKeyPairsRequest shouldBe request.toAws
            val awsKeyPairInfo = keyPairInfo.map { kpi ⇒
              new aws.KeyPairInfo().withKeyName(kpi.name).withKeyFingerprint(kpi.fingerprint)
            }.asJavaCollection
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
      forAll { requestArgs: EC2Gen.DescribeInstanceRequestArgs ⇒
        val request = requestArgs.toRequest
        val reservationsArgs = Gen.resize(10, arbitrary[Seq[EC2Gen.ReservationArgs]]).reallySample
        val reservations = reservationsArgs.map(_.toReservation)
        val ec2 = new FakeAmazonEC2Async {
          override def describeInstancesAsync(describeInstancesRequest: aws.DescribeInstancesRequest,
                                              asyncHandler: AsyncHandler[aws.DescribeInstancesRequest, aws.DescribeInstancesResult]): JFuture[aws.DescribeInstancesResult] = {
            describeInstancesRequest shouldBe request.toAws
            val awsReservations = reservationsArgs.map(_.toAws).asJavaCollection
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
