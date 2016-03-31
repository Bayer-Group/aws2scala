package com.monsanto.arch.awsutil.s3.model

import java.util.Date

import akka.stream.Materializer
import com.monsanto.arch.awsutil.s3.{UploadSource, AsyncS3Client}
import com.monsanto.arch.awsutil.s3.model.S3.Implicits

import scala.concurrent.Future
import scala.language.implicitConversions

/** Represents an S3 bucket and allows operations on the bucket through method invocation.
  *
  * @param name the name of the bucket
  * @param owner the bucket‘s owner
  * @param creationDate the date when the bucket was created
  */
case class Bucket(name: String, owner: Owner, creationDate: Date) {
  /** Deletes the bucket from S3. */
  def delete()(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Unit] = {
    import m.executionContext
    asyncS3Client.deleteBucket(name).map(_ => ())
  }

  /** Empties the bucket. */
  def empty()(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Unit] = {
    import m.executionContext
    asyncS3Client.emptyBucket(name).map(_ => ())
  }

  /** Empties the bucket and then attempts to delete it. */
  def emptyAndDelete()(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Unit] = {
    import m.executionContext
    asyncS3Client.emptyAndDeleteBucket(name).map(_ => ())
  }

  /** Checks to see if the bucket, in fact, exists. */
  def exists()(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Boolean] =
    asyncS3Client.doesBucketExist(name)

  /** Gets the policy for the bucket, if any. */
  def getPolicy()(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Option[String]] =
    asyncS3Client.getBucketPolicy(name)

  /** Sets or deletes the policy for the bucket.
    *
    * @param policyText if defined, the new policy text for the bucket.  If empty, requests that the bucket‘s policy
    *                   be deleted
    */
  def setPolicy(policyText: Option[String])(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Unit] = {
    import m.executionContext
    asyncS3Client.setBucketPolicy(name, policyText).map(_ => ())
  }

  /** Sets the policy for the bucket.
    *
    * @param policyText the new policy text for the bucket
    */
  def setPolicy(policyText: String)(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Unit] =
    setPolicy(Option(policyText))

  /** Deletes the bucket‘s policy. */
  def deletePolicy()(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Unit] = setPolicy(None)

  /* Gets the tags for the bucket.  If the bucket has no tags, it will return an empty map. */
  def getTags()(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Map[String,String]] =
    asyncS3Client.getBucketTags(name)

  /** Sets the tags for the bucket.  An empty map is equivalent to deleting the tags on the bucket. */
  def setTags(tags: Map[String,String])(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Unit] =
    asyncS3Client.setBucketTags(name, tags).map(_ ⇒ ())(m.executionContext)

  /** Deletes the tags from the bucket. */
  def deleteTags()(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Unit] =
    asyncS3Client.deleteBucketTags(name).map(_ ⇒ ())(m.executionContext)

  /** Returns all the objects in the bucket. */
  def list()(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Seq[Object]] = {
    import m.executionContext
    asyncS3Client.listObjects(name).map(_.map(Implicits.fromAws))
  }

  /** Returns all the objects in the bucket with the given prefix. */
  def list(prefix: String)(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Seq[Object]] = {
    import m.executionContext
    asyncS3Client.listObjects(name, prefix).map(_.map(Implicits.fromAws))
  }

  /** Uploads the content to the given key in this bucket. */
  def upload[T: UploadSource](key: String, content: T)(implicit asyncS3Client: AsyncS3Client, m: Materializer): Future[Object] =
    asyncS3Client.upload(name, key, content).map(Implicits.fromAws)(m.executionContext)
}

object Bucket {
  /** The set of all valid bucket name characters. */
  private val ValidBucketNameChar = (('a' to 'z') ++ ('0' to '9') :+ '-' :+ '.').toSet

  private val IPv4Regex = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$"

  /** Verifies that the given name may be used as a bucket name in any region (US East is a little more permissive). */
  def validName(name: String): Boolean = {
    name.length >= 3 &&
      name.length < 64 &&
      name.forall(ValidBucketNameChar) &&
      !name.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") &&
      {
        val labels = {
          val result = name.foldLeft((Seq.empty[String], Seq.empty[Char])) {
            case ((strings, chars), '.') ⇒ (strings :+ chars.mkString, Seq.empty)
            case ((strings, chars), c) ⇒ (strings, chars :+ c)
          }
          result._1 :+ result._2.mkString
        }
        labels.forall(l ⇒ l.nonEmpty && l.head.isLetterOrDigit && l.last.isLetterOrDigit)
      }
  }
}
