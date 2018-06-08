package com.monsanto.arch.awsutil.ec2

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.ec2.{AmazonEC2Async, model ⇒ aws}
import com.monsanto.arch.awsutil.ec2.model._
import com.monsanto.arch.awsutil._

import scala.collection.JavaConverters._

private[awsutil] class DefaultStreamingEC2Client(client: AmazonEC2Async) extends StreamingEC2Client {
  override val keyPairCreator =
    Flow[String]
      .map(name ⇒ new aws.CreateKeyPairRequest(name))
      .via[aws.CreateKeyPairResult, NotUsed](AWSFlow.simple(client.createKeyPairAsync))
      .map(r ⇒ KeyPair.fromAws(r.getKeyPair))
      .named("EC2.keyPairCreator")

  override val keyPairDeleter =
    Flow[String]
      .map(name ⇒ new aws.DeleteKeyPairRequest(name))
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput(client.deleteKeyPairAsync)))
      .map(_.getKeyName)
      .named("EC2.keyPairDeleter")

  override val keyPairsDescriber =
    Flow[DescribeKeyPairsRequest]
      .map(_.toAws)
      .via[aws.DescribeKeyPairsResult, NotUsed](AWSFlow.simple(client.describeKeyPairsAsync))
      .mapConcat(_.getKeyPairs.asScala.map(KeyPairInfo.fromAws).toList)
      .named("EC2.keyPairsDescriber")

  override val instancesDescriber =
    Flow[DescribeInstancesRequest]
      .map(_.toAws)
      .via[aws.DescribeInstancesResult, NotUsed](AWSFlow.pagedByNextToken(client.describeInstancesAsync))
      .mapConcat(_.getReservations.asScala.toList)
      .map(Reservation.fromAws)
      .named("EC2.instancesDescriber")
}
