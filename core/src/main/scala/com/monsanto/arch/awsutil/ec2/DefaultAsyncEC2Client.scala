package com.monsanto.arch.awsutil.ec2

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.AmazonServiceException
import com.monsanto.arch.awsutil.ec2.model.{DescribeInstancesRequest, DescribeKeyPairsRequest}

private[awsutil] class DefaultAsyncEC2Client(client: StreamingEC2Client) extends AsyncEC2Client {
  override def createKeyPair(name: String)(implicit m: Materializer) =
    Source.single(name)
      .via(client.keyPairCreator)
      .runWith(Sink.head)

  override def deleteKeyPair(name: String)(implicit m: Materializer) =
    Source.single(name)
      .via(client.keyPairDeleter)
      .runWith(Sink.head)

  override def describeKeyPairs()(implicit m: Materializer) =
    Source.single(DescribeKeyPairsRequest.allKeyPairs)
      .via(client.keyPairsDescriber)
      .runWith(Sink.seq)

  override def describeKeyPairs(filters: Map[String, Seq[String]])(implicit m: Materializer) =
    Source.single(DescribeKeyPairsRequest.filter(filters))
      .via(client.keyPairsDescriber)
      .runWith(Sink.seq)

  override def describeKeyPair(name: String)(implicit m: Materializer) =
    Source.single(DescribeKeyPairsRequest.withName(name))
      .via(client.keyPairsDescriber)
      .map(Some(_))
      .recover {
        case e: AmazonServiceException if e.getErrorCode == "InvalidKeyPair.NotFound" ⇒ None
      }
      .runWith(Sink.head)

  override def describeInstances()(implicit m: Materializer) =
    Source.single(DescribeInstancesRequest.allInstances)
      .via(client.instancesDescriber)
      .mapConcat(_.instances.toList)
      .runWith(Sink.seq)

  override def describeInstances(filters: Map[String, Seq[String]])(implicit m: Materializer) =
    Source.single(DescribeInstancesRequest.filter(filters))
      .via(client.instancesDescriber)
      .mapConcat(_.instances.toList)
      .runWith(Sink.seq)

  override def describeInstance(instanceId: String)(implicit m: Materializer) =
    Source.single(DescribeInstancesRequest.withId(instanceId))
      .via(client.instancesDescriber)
      .mapConcat(_.instances.toList)
      .map(Some(_))
      .recover {
        case e: AmazonServiceException if e.getErrorCode == "InvalidInstanceId.NotFound" ⇒ None
      }
      .runWith(Sink.head)
}
