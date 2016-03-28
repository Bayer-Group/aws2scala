package com.monsanto.arch.awsutil.s3

import java.io.File
import java.net.URL

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.amazonaws.services.s3.model._
import com.monsanto.arch.awsutil.StreamingAwsClient
import com.monsanto.arch.awsutil.s3.model.BucketNameAndKey

/** Provides an interface to S3 built around Akka streams.
  *
  * @author Daniel Solano Gómez
  * @author Jorge Montero
  */
trait StreamingS3Client extends StreamingAwsClient {
  /** Returns an Akka flow that given a bucket name, it will create the bucket, applying the default bucket policy. */
  def bucketCreator: Flow[String, Bucket, NotUsed]

  /** Returns an Akka flow that given a bucket name it will emit the policy for the bucket. */
  def bucketPolicyGetter: Flow[String, Option[String], NotUsed]

  /** Returns an Akka flow that given a bucket name and an optional policy text, will either set or remove the bucket‘s
    * policy and emit the bucket name.
    */
  def bucketPolicySetter: Flow[(String, Option[String]), String, NotUsed]

  /** Returns an Akka flow that given a bucket name will emit the tags for the bucket. */
  def bucketTagsGetter: Flow[String, Map[String,String], NotUsed]

  /** Returns an Akka flow that given a bucket name and some tags will set the tags for the bucket.  If the given
    * tag map is empty, it will delete all tagging from the bucket.  Emits the bucket name.
    */
  def bucketTagsSetter: Flow[(String, Map[String,String]), String, NotUsed]

  /** Returns an Akka flow that given a bucket name will delete the bucket and emit the deleted bucket‘s name. */
  def bucketDeleter: Flow[String, String, NotUsed]

  /** Returns an Akka flow that deletes a bucket name, even if it is not empty, and emits the deleted bucket's name */
  def bucketEmptierAndDeleter: Flow[String, String, NotUsed]

  /** Returns an Akka flow that empties the contents of the given bucket, and emits the bucket's name */
  def bucketEmptier: Flow[String, String, NotUsed]

  /** Returns an Akka source that will output all of the buckets available. */
  def bucketLister: Source[Bucket, NotUsed]

  /** Returns an Akka flow that given a bucket name emits whether or not the bucket exists. */
  def bucketExistenceChecker: Flow[String, Boolean, NotUsed]

  /** Returns an Akka flow that list objects given a bucket name and optional prefix, emitting the result object
    *  summaries.
    */
  def objectLister: Flow[(String, Option[String]), S3ObjectSummary, NotUsed]

  /** Returns an Akka flow that performs the list requests, emitting the result object summaries. */
  def rawObjectLister: Flow[ListObjectsRequest, S3ObjectSummary, NotUsed]

  /** Creates an Akka flow that takes a bucket name, key, and source, and uploads it to S3.  Emits a summary for the
    * object resulting from the upload.
    */
  def uploader[T: UploadSource]: Flow[(BucketNameAndKey, T), S3ObjectSummary, NotUsed]

  /** Returns an Akka flow that takes put object requests and emits the object summary of the uploaded object. */
  def rawUploader: Flow[PutObjectRequest, S3ObjectSummary, NotUsed]

  /** Returns an Akka flow that takes a bucket name and key and downloads its content to an object of the specified
    * type (by default, a string or byte array).
    */
  def downloader[T: DownloadSink]: Flow[BucketNameAndKey, T, NotUsed]

  /** Returns an Akka flow that takes a bucket name and key and downloads the object‘s content to the given file. */
  def fileDownloader: Flow[(BucketNameAndKey, File), File, NotUsed]

  /** Returns an Akka flow that takes a raw request and emits an input stream to the object‘s content. */
  def rawDownloader: Flow[GetObjectRequest, S3ObjectInputStream, NotUsed]

  /** Returns an Akka flow that takes a raw request and file downloads the object‘s content to the file.  Emits the
    * file once the download is complete.
    */
  def rawFileDownloader: Flow[(GetObjectRequest,File), File, NotUsed]

  /** Returns an Akka flow that performs an object copy, emitting the new object summary of the copy.  The input to
    * this flow is a tuple of two tuples (source and destination) of bucket name and key, e.g.
    * `((sourceBucketName, sourceKey) (destinationBucketName, destinationKey))`.
    */
  def copier: Flow[(BucketNameAndKey, BucketNameAndKey), S3ObjectSummary, NotUsed]

  /** Returns an Akka flow that performs the given request, emitting the new object summary of the copy. */
  def rawCopier: Flow[CopyObjectRequest, S3ObjectSummary, NotUsed]

  /** Deletes the given object, identified by bucket name and key, emitting a tuple of bucket name and key. */
  def objectDeleter: Flow[BucketNameAndKey, BucketNameAndKey, NotUsed]

  /** Returns the URL for the given bucket and key. */
  def objectUrlGetter: Flow[BucketNameAndKey, URL, NotUsed]
}
