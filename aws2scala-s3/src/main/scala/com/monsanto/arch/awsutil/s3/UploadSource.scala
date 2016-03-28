package com.monsanto.arch.awsutil.s3

import java.io.{ByteArrayInputStream, File}

import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.monsanto.arch.awsutil.s3.model.BucketNameAndKey

/** Type class for allowing uploads of arbitrary objects.  Support for strings, byte arrays, and files is included. */
trait UploadSource[T] {
  /** Given a bucket name, key, and source instance, return a PutObjectRequest to upload that source to the given
    * location.
    */
  def apply(bucketNameAndKey: BucketNameAndKey, t: T): PutObjectRequest
}

object UploadSource {
  /** Supports uploading arrays of bytes. */
  implicit val bytesSource = new UploadSource[Array[Byte]] {
    override def apply(bucketNameAndKey: BucketNameAndKey, bytes: Array[Byte]) = {
      val metadata = new ObjectMetadata()
      metadata.setContentLength(bytes.length)
      new PutObjectRequest(bucketNameAndKey.bucketName, bucketNameAndKey.key,
        new ByteArrayInputStream(bytes), metadata)
    }
  }

  /** Supports uploading strings. */
  implicit val stringSource = new UploadSource[String] {
    override def apply(bucketNameAndKey: BucketNameAndKey, content: String) =
      bytesSource(bucketNameAndKey, content.getBytes)
  }

  /** Supports uploading files. */
  implicit val fileSource = new UploadSource[File] {
    override def apply(bucketNameAndKey: BucketNameAndKey, file: File) =
      new PutObjectRequest(bucketNameAndKey.bucketName, bucketNameAndKey.key, file)
  }
}
