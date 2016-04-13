package com.monsanto.arch.awsutil.s3

import java.io.File
import java.net.URL

import akka.stream.Materializer
import com.amazonaws.services.s3.{model ⇒ aws}
import com.monsanto.arch.awsutil.AsyncAwsClient
import com.monsanto.arch.awsutil.s3.model.{Bucket, BucketNameAndKey}

import scala.concurrent.Future

/** Wraps the Amazon S3 client to provide a Scala future-based interface.
  *
  * @author Daniel Solano Gómez
  * @author Jorge Montero
  */
trait AsyncS3Client extends AsyncAwsClient {
  /** Creates a new bucket with the given name.  The location of the bucket is dependent on the location for which the
    * client is configured.
    *
    * @param bucketName the name of the bucket to create
    * @return a future containing the bucket created by AWS
    */
  def createBucket(bucketName: String)(implicit m: Materializer): Future[Bucket]

  /** Deletes the bucket with the given name. This bucket must be empty
    *
    * @param bucketName the name of the bucket to delete
    * @return a future containing the name of the bucket that was deleted
    */
  def deleteBucket(bucketName: String)(implicit m: Materializer): Future[String]

  /** Empties the bucket with the given name.
    *
    * @return a future containing the name of the bucket that was emptied out
    */
  def emptyBucket(bucketName: String)(implicit m: Materializer):Future[String]

  /** Deletes the bucket with the given name, and all its contents
    *
    * @param bucketName the name of the bucket to delete
    * @return a future containing the name of the bucket that was deleted
    */
  def emptyAndDeleteBucket(bucketName:String)(implicit m: Materializer) :Future[String]

  /** Returns a sequence of all of the buckets available. */
  def listBuckets()(implicit m: Materializer): Future[Seq[aws.Bucket]]

  /** Returns whether the bucket exists. */
  def doesBucketExist(bucketName: String)(implicit m: Materializer): Future[Boolean]

  /** Returns the given bucket‘s policy, if any.
    *
    * @param bucketName the name of the bucket for which to retrieve a policy
    * @return a future that may contain the policy text for the bucket, if it has one
    */
  def getBucketPolicy(bucketName: String)(implicit m: Materializer): Future[Option[String]]

  /** Sets the policy for the given bucket.
    *
    * @param bucketName the name of the bucket for which to set policy
    * @param policyText if defined, the new policy text for the bucket. If empty, removes any policy on the bucket
    * @return a future containing the name of the bucket whose policy was modified
    */
  def setBucketPolicy(bucketName: String, policyText: Option[String])(implicit m: Materializer): Future[String]

  /** Sets the policy for the given bucket.
    *
    * @param bucketName the name of the bucket for which to set policy
    * @param policyText the new policy text for the bucket
    * @return a future containing the name of the bucket whose policy was modified
    */
  def setBucketPolicy(bucketName: String, policyText: String)(implicit m: Materializer): Future[String]

  /** Deletes the policy for the given bucket.
    *
    * @param bucketName the name of the bucket from which to remove any policy
    * @return a future containing the name of the bucket whose policy was removed
    */
  def deleteBucketPolicy(bucketName: String)(implicit m: Materializer): Future[String]

  /** Gets the tagging configuration for the given bucket. */
  def getBucketTags(bucketName: String)(implicit m: Materializer): Future[Map[String,String]]

  /** Sets the tagging configuration for the given bucket.  If the tags map is empty, this function is equivalent to
    * deleting the taggs for the bucket.
    *
    * @return a future containing the bucket name whose tagging was set
    */
  def setBucketTags(bucketName: String, tags: Map[String,String])(implicit m: Materializer): Future[String]

  /** Removes the tagging configuration for the given bucket.
    *
    * @return a future containing the bucket name whose tagging was set
    */
  def deleteBucketTags(bucketName: String)(implicit m: Materializer): Future[String]

  /** Uploads the content to the given bucket and key.  Once the transfer is complete, it will wait until S3 lists the
    * uploaded object and return the object summary.  By default, the content may be a string, array of bytes, or a
    * file.
    */
  def upload[T: UploadSource](bucketName: String, key: String, t: T)(implicit m: Materializer): Future[aws.S3ObjectSummary]

  /** Downloads the object at the given bucket and key to an object.  By default, the sink type may be a string or a
    * byte array.
    */
  def download[T: DownloadSink](bucketName: String, key: String)(implicit m: Materializer): Future[T]

  /** Downloads the object at the given bucket and key to a specific file. */
  def downloadTo(bucketName: String, key: String, file: File)(implicit m: Materializer): Future[File]

  /** Copies the object from one key to another in the same bucket, returning the object summary of the copy. */
  def copy(bucketName: String, sourceKey: String, destKey: String)
          (implicit m: Materializer): Future[aws.S3ObjectSummary]

  /** Copies the object from one bucket and key to another bucket and key, returning the object summary of the copy. */
  def copy(sourceBucketName: String, sourceKey: String,
           destBucketName: String, destKey: String)
          (implicit m: Materializer): Future[aws.S3ObjectSummary]

  /** List all the objects in a bucket*/
  def listObjects(bucketName: String)(implicit m: Materializer): Future[Seq[aws.S3ObjectSummary]]

  /** List all the objects in a bucket matching the given prefix*/
  def listObjects(bucketName: String, prefix: String)(implicit m: Materializer): Future[Seq[aws.S3ObjectSummary]]

  /** Deletes an object. */
  def deleteObject(bucketName: String, key: String)(implicit m: Materializer): Future[BucketNameAndKey]

  /** Returns the URL for the object in the given bucket at the given key. */
  def getUrl(bucketName: String, key: String)(implicit m: Materializer): Future[URL]
}


