package com.monsanto.arch.awsutil.ec2

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.monsanto.arch.awsutil.ec2.model._

/** Akka streams-based interface to Amazon’s Elastic Cloud Compute.
  *
  * @author Jorge Montero
  */
trait StreamingEC2Client {
  /** Returns an Akka flow that will take a key name and create and emit a key pair. */
  def keyPairCreator: Flow[String, KeyPair, NotUsed]

  /** Returns an Akka flow that will take a key name and delete the corresponding key pair.  Emits the key name. */
  def keyPairDeleter: Flow[String, String, NotUsed]

  /** Returns an Akka flow that given a key pair listing request will emit all matching key pair information. */
  def keyPairsDescriber: Flow[DescribeKeyPairsRequest, KeyPairInfo, NotUsed]

  /** Returns an Akka flow that will emit all reservations satisfying the given request. */
  def instancesDescriber: Flow[DescribeInstancesRequest, Reservation, NotUsed]
}
