package com.monsanto.arch.awsutil.ec2

import akka.stream.scaladsl.Flow
import com.amazonaws.AmazonServiceException
import com.monsanto.arch.awsutil.ec2.model._
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Samplers.{EnhancedGen, arbitrarySample}
import com.monsanto.arch.awsutil.test_support.{FlowMockUtils, Materialised}
import com.monsanto.arch.awsutil.testkit.Ec2Gen
import com.monsanto.arch.awsutil.testkit.Ec2ScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class AsyncEC2ClientSpec extends FreeSpec with MockFactory with Materialised with FlowMockUtils {
  "the default async EC2 client can" - {
    "create a key pair" in {
      forAll { keyPair: KeyPair ⇒
        val streaming = mock[StreamingEC2Client]("streaming")
        val async = new DefaultAsyncEC2Client(streaming)

        val name = keyPair.name

        (streaming.keyPairCreator _)
          .expects()
          .returningFlow(name, keyPair)

        val result = async.createKeyPair(name).futureValue
        result shouldBe keyPair
      }
    }

    "delete a key pair" in {
      forAll(Ec2Gen.keyName → "keyName") { name ⇒
        val streaming = mock[StreamingEC2Client]("streaming")
        val async = new DefaultAsyncEC2Client(streaming)

        (streaming.keyPairDeleter _)
          .expects()
          .returningFlow(name, name)

        val result = async.deleteKeyPair(name).futureValue
        result shouldBe name
      }
    }

    "describe key pairs" - {
      "all of them" in {
        forAll(SizeRange(30)) { keyPairInfo: List[KeyPairInfo] ⇒
          val streaming = mock[StreamingEC2Client]("streaming")
          val async = new DefaultAsyncEC2Client(streaming)

          (streaming.keyPairsDescriber _)
            .expects()
            .returningConcatFlow(DescribeKeyPairsRequest(Seq.empty, Seq.empty), keyPairInfo)

          val result = async.describeKeyPairs().futureValue
          result shouldBe keyPairInfo
        }
      }

      "using a filter" in {
        forAll("filters", minSize(20)) { filters: Seq[Filter] ⇒
          val streaming = mock[StreamingEC2Client]("streaming")
          val async = new DefaultAsyncEC2Client(streaming)

          val mapFilter = filters.map(f ⇒ f.name → f.values).toMap
          val keyPairInfo = arbitrarySample[List[KeyPairInfo]](20)

          (streaming.keyPairsDescriber _)
            .expects()
            .returningConcatFlow(DescribeKeyPairsRequest(Seq.empty, Filter.fromMap(mapFilter)), keyPairInfo)

          val result = async.describeKeyPairs(mapFilter).futureValue
          result shouldBe keyPairInfo
        }
      }

      "by name" in {
        forAll(Ec2Gen.keyName → "keyName") { name ⇒
          val streaming = mock[StreamingEC2Client]("streaming")
          val async = new DefaultAsyncEC2Client(streaming)

          val maybeKeyPairInfo = arbitrarySample[Option[KeyPairInfo]](5)

          (streaming.keyPairsDescriber _)
            .expects()
            .onCall { () ⇒
              Flow[DescribeKeyPairsRequest]
                .mapConcat { request ⇒
                  request shouldBe DescribeKeyPairsRequest(Seq(name), Seq.empty)
                  maybeKeyPairInfo match {
                    case None ⇒
                      val ex = new AmazonServiceException("Oh, no!")
                      ex.setErrorCode("InvalidKeyPair.NotFound")
                      throw ex
                    case Some(kpi) ⇒
                      List(kpi)
                  }
                }
            }

          val result = async.describeKeyPair(name).futureValue
          result shouldBe maybeKeyPairInfo
        }
      }
    }

    "describe instance" - {
      "all of them" in {
        val streaming = mock[StreamingEC2Client]("streaming")
        val async = new DefaultAsyncEC2Client(streaming)

        val reservations = Gen.resize(10, arbitrary[List[Reservation]]).reallySample
        val instances = reservations.flatMap(_.instances)

        (streaming.instancesDescriber _)
          .expects()
          .returningConcatFlow(DescribeInstancesRequest(Seq.empty, Seq.empty), reservations)

        val result = async.describeInstances().futureValue
        result shouldBe instances
      }

      "using a filter" in {
        forAll { filters: Seq[Filter] ⇒
          val streaming = mock[StreamingEC2Client]("streaming")
          val async = new DefaultAsyncEC2Client(streaming)

          val mapFilter = filters.map(f ⇒ f.name → f.values).toMap
          val reservations = Gen.resize(10, arbitrary[List[Reservation]]).reallySample
          val instances = reservations.flatMap(_.instances)

          (streaming.instancesDescriber _)
            .expects()
            .returningConcatFlow(DescribeInstancesRequest(Seq.empty, Filter.fromMap(mapFilter)), reservations)

          val result = async.describeInstances(mapFilter).futureValue
          result shouldBe instances
        }
      }

      "by name" in {
        forAll(Ec2Gen.instanceId → "instanceId") { id ⇒
          val streaming = mock[StreamingEC2Client]("streaming")
          val async = new DefaultAsyncEC2Client(streaming)

          val reservationGen = Gen.resize(5, arbitrary[Reservation]).map { reservation ⇒
            reservation.copy(instances = Seq(reservation.instances.head))
          }
          val maybeReservation = Gen.option(reservationGen).reallySample

          (streaming.instancesDescriber _)
            .expects()
            .onCall { () ⇒
              Flow[DescribeInstancesRequest]
                .mapConcat { request ⇒
                  request shouldBe DescribeInstancesRequest(Seq(id), Seq.empty)
                  maybeReservation match {
                    case None ⇒
                      val ex = new AmazonServiceException("Oh, no!")
                      ex.setErrorCode("InvalidInstanceId.NotFound")
                      throw ex
                    case Some(r) ⇒
                      List(r)
                  }
                }
            }

          val result = async.describeInstance(id).futureValue
          result shouldBe maybeReservation.map(_.instances.head)
        }
      }
    }
  }
}
