package com.monsanto.arch.awsutil.ec2

import akka.stream.Materializer
import com.monsanto.arch.awsutil.AsyncAwsClient
import com.monsanto.arch.awsutil.ec2.model.{Instance, KeyPair, KeyPairInfo}

import scala.concurrent.Future

/** Scala future-based interface to Amazon’s Elastic Cloud Compute.
  *
  * @author Jorge Montero
  */
trait AsyncEC2Client extends AsyncAwsClient {
  /** Creates a key pair to use with EC2. */
  def createKeyPair(name: String)(implicit m: Materializer): Future[KeyPair]

  /** Removes a key pair from AWS. */
  def deleteKeyPair(name: String)(implicit m: Materializer): Future[String]

  /** Lists all available key pairs. */
  def describeKeyPairs()(implicit m: Materializer): Future[Seq[KeyPairInfo]]

  /** Describes all available key pairs matching the given filters. */
  def describeKeyPairs(filters: Map[String, Seq[String]])(implicit m: Materializer): Future[Seq[KeyPairInfo]]

  /** Gets information about a particular key pair. */
  def describeKeyPair(name: String)(implicit m: Materializer): Future[Option[KeyPairInfo]]

  /** Describes all available EC2 instances. */
  def describeInstances()(implicit m: Materializer): Future[Seq[Instance]]

  /** Describes all available EC2 instances matching the given filters. */
  def describeInstances(filters: Map[String, Seq[String]])(implicit m: Materializer): Future[Seq[Instance]]

  /** Describes the EC2 instance with the given ID. */
  def describeInstance(instanceId: String)(implicit m: Materializer): Future[Option[Instance]]
}






