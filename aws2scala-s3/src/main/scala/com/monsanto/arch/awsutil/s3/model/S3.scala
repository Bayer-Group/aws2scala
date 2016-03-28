package com.monsanto.arch.awsutil.s3.model

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.amazonaws.services.s3.{model ⇒ aws}
import com.monsanto.arch.awsutil.s3.{AsyncS3Client, StreamingS3Client}

import scala.concurrent.Future
import scala.language.implicitConversions

/** Object-oriented interface to Amazon S3.
  *
  * @author Daniel Solano Gómez
  */
object S3 {
  /** Returns whether or not a bucket with the given name exists. */
  def exists(name: String)(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Boolean] =
    asyncS3Client.doesBucketExist(name)

  /** Creates a bucket with the given name. */
  def create(name: String)(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Bucket] = {
    import m.executionContext
    asyncS3Client.createBucket(name).map(Implicits.fromAws)
  }

  /** Lists all available buckets. */
  def list()(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Seq[Bucket]] = {
    import m.executionContext
    asyncS3Client.listBuckets().map(_.map(Implicits.fromAws))
  }

  /** Returns the bucket with the given name, if any. */
  def find(name: String)(implicit streamingS3Client: StreamingS3Client, m: Materializer): Future[Option[Bucket]] =
    streamingS3Client.bucketLister
      .filter(_.getName == name)
      .map(Implicits.fromAws)
      .runWith(Sink.headOption)

  object Implicits {
    implicit def fromAws(awsBucket: aws.Bucket): Bucket = Bucket(awsBucket.getName, awsBucket.getOwner, awsBucket.getCreationDate)
    implicit def fromAws(awsOwner: aws.Owner): Owner = Owner(awsOwner.getId, awsOwner.getDisplayName)
    implicit def fromAws(awsStorageClass: aws.StorageClass): StorageClass = awsStorageClass match {
      case aws.StorageClass.Glacier                  ⇒ StorageClass.Glacier
      case aws.StorageClass.ReducedRedundancy        ⇒ StorageClass.ReducedRedundancy
      case aws.StorageClass.Standard                 ⇒ StorageClass.Standard
      case aws.StorageClass.StandardInfrequentAccess ⇒ StorageClass.StandardInfrequentAccess
    }
    implicit def fromAws(objectSummary: aws.S3ObjectSummary): Object =
      Object(
        objectSummary.getBucketName,
        objectSummary.getKey,
        objectSummary.getETag,
        objectSummary.getLastModified,
        objectSummary.getOwner,
        objectSummary.getSize,
        aws.StorageClass.fromValue(objectSummary.getStorageClass))
  }
}
