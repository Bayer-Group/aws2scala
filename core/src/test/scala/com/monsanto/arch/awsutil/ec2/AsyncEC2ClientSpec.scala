package com.monsanto.arch.awsutil.ec2

import akka.stream.scaladsl.Flow
import com.amazonaws.AmazonServiceException
import com.monsanto.arch.awsutil.ec2.model._
import com.monsanto.arch.awsutil.test.Samplers.{EnhancedGen, arbitrarySample}
import com.monsanto.arch.awsutil.{FlowMockUtils, Materialised}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class AsyncEC2ClientSpec extends FreeSpec with MockFactory with Materialised with FlowMockUtils {
  "the default async EC2 client can" - {
    "create a key pair" in {
      forAll { args: EC2Gen.KeyPairArgs ⇒
        val streaming = mock[StreamingEC2Client]("streaming")
        val async = new DefaultAsyncEC2Client(streaming)

        val keyPair = args.toKeyPair
        val name = keyPair.name

        (streaming.keyPairCreator _)
          .expects()
          .returningFlow(name, keyPair)

        val result = async.createKeyPair(name).futureValue
        result shouldBe keyPair
      }
    }

    "delete a key pair" in {
      forAll { arg: EC2Gen.KeyName ⇒
        val streaming = mock[StreamingEC2Client]("streaming")
        val async = new DefaultAsyncEC2Client(streaming)

        val name = arg.value

        (streaming.keyPairDeleter _)
          .expects()
          .returningFlow(name, name)

        val result = async.deleteKeyPair(name).futureValue
        result shouldBe name
      }
    }

    "describe key pairs" - {
      "all of them" in {
        val streaming = mock[StreamingEC2Client]("streaming")
        val async = new DefaultAsyncEC2Client(streaming)

        val keyPairInfo = arbitrarySample[List[EC2Gen.KeyPairInfoArgs]].map(_.toKeyPairInfo)

        (streaming.keyPairsDescriber _)
          .expects()
          .returningConcatFlow(DescribeKeyPairsRequest(Seq.empty, Seq.empty), keyPairInfo)

        val result = async.describeKeyPairs().futureValue
        result shouldBe keyPairInfo
      }

      "using a filter" in {
        forAll { args: Seq[EC2Gen.FilterArgs] ⇒
          val streaming = mock[StreamingEC2Client]("streaming")
          val async = new DefaultAsyncEC2Client(streaming)

          val filters = args.map(_.toFilter)
          val mapFilter = filters.map(f ⇒ f.name → f.values).toMap
          val keyPairInfo = arbitrarySample[List[EC2Gen.KeyPairInfoArgs]].map(_.toKeyPairInfo)

          (streaming.keyPairsDescriber _)
            .expects()
            .returningConcatFlow(DescribeKeyPairsRequest(Seq.empty, Filter.fromMap(mapFilter)), keyPairInfo)

          val result = async.describeKeyPairs(mapFilter).futureValue
          result shouldBe keyPairInfo
        }
      }

      "by name" in {
        forAll { arg: EC2Gen.KeyName ⇒
          val name = arg.value

          val streaming = mock[StreamingEC2Client]("streaming")
          val async = new DefaultAsyncEC2Client(streaming)

          val maybeKeyPairInfo = arbitrarySample[Option[EC2Gen.KeyPairInfoArgs]].map(_.toKeyPairInfo)

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

        val reservations = Gen.resize(10, arbitrary[List[EC2Gen.ReservationArgs]]).reallySample.map(_.toReservation)
        val instances = reservations.flatMap(_.instances)

        (streaming.instancesDescriber _)
          .expects()
          .returningConcatFlow(DescribeInstancesRequest(Seq.empty, Seq.empty), reservations)

        val result = async.describeInstances().futureValue
        result shouldBe instances
      }

      "using a filter" in {
        forAll { args: Seq[EC2Gen.FilterArgs] ⇒
          val streaming = mock[StreamingEC2Client]("streaming")
          val async = new DefaultAsyncEC2Client(streaming)

          val filters = args.map(_.toFilter)
          val mapFilter = filters.map(f ⇒ f.name → f.values).toMap
          val reservations = Gen.resize(10, arbitrary[List[EC2Gen.ReservationArgs]]).reallySample.map(_.toReservation)
          val instances = reservations.flatMap(_.instances)

          (streaming.instancesDescriber _)
            .expects()
            .returningConcatFlow(DescribeInstancesRequest(Seq.empty, Filter.fromMap(mapFilter)), reservations)

          val result = async.describeInstances(mapFilter).futureValue
          result shouldBe instances
        }
      }

      "by name" in {
        forAll { arg: EC2Gen.InstanceId ⇒
          val id = arg.value

          val streaming = mock[StreamingEC2Client]("streaming")
          val async = new DefaultAsyncEC2Client(streaming)

          val reservationGen = Gen.resize(10, arbitrary[EC2Gen.ReservationArgs]).map { args ⇒
            val r = args.toReservation
            Reservation(r.id, r.owner, r.requester, r.groups, Seq(r.instances.head))
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
