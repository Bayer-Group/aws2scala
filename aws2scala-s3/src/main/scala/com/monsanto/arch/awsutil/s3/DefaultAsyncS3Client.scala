package com.monsanto.arch.awsutil.s3

import java.io.File

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.monsanto.arch.awsutil.s3.model.BucketNameAndKey

private[awsutil] class DefaultAsyncS3Client(streamingS3Client: StreamingS3Client) extends AsyncS3Client {
  override def createBucket(bucketName: String)(implicit m: Materializer) =
    Source.single(bucketName)
      .via(streamingS3Client.bucketCreator)
      .runWith(Sink.head)

  override def deleteBucket(bucketName: String)(implicit m: Materializer) =
    Source.single(bucketName)
      .via(streamingS3Client.bucketDeleter)
      .runWith(Sink.head)

  override def emptyBucket(bucketName: String)(implicit m: Materializer) =
    Source.single(bucketName)
      .via(streamingS3Client.bucketEmptier)
      .runWith(Sink.head)

  override def emptyAndDeleteBucket(bucketName:String)(implicit m: Materializer) =
    Source.single(bucketName)
      .via(streamingS3Client.bucketEmptierAndDeleter)
      .runWith(Sink.head)

  override def listBuckets()(implicit m: Materializer) =
    streamingS3Client.bucketLister.runWith(Sink.seq)

  override def doesBucketExist(bucketName: String)(implicit m: Materializer) =
    Source.single(bucketName)
      .via(streamingS3Client.bucketExistenceChecker)
      .runWith(Sink.head)

  override def getBucketPolicy(bucketName: String)(implicit m: Materializer) =
    Source.single(bucketName)
      .via(streamingS3Client.bucketPolicyGetter)
      .runWith(Sink.head)

  override def setBucketPolicy(bucketName: String, policyText: Option[String])(implicit m: Materializer) =
    Source.single((bucketName, policyText))
      .via(streamingS3Client.bucketPolicySetter)
      .runWith(Sink.head)

  override def setBucketPolicy(bucketName: String, policyText: String)(implicit m: Materializer) =
    setBucketPolicy(bucketName, Option(policyText))

  override def deleteBucketPolicy(bucketName: String)(implicit m: Materializer) =
    setBucketPolicy(bucketName, None)

  override def getBucketTags(bucketName: String)(implicit m: Materializer) =
    Source.single(bucketName)
      .via(streamingS3Client.bucketTagsGetter)
      .runWith(Sink.head)

  override def setBucketTags(bucketName: String, tags: Map[String, String])
                            (implicit m: Materializer) =
    Source.single((bucketName, tags))
      .via(streamingS3Client.bucketTagsSetter)
      .runWith(Sink.head)

  override def deleteBucketTags(bucketName: String)(implicit m: Materializer) =
    setBucketTags(bucketName, Map.empty)

  override def upload[T: UploadSource](bucketName: String, key: String, t: T)(implicit m: Materializer) =
    Source.single((BucketNameAndKey(bucketName, key), t))
      .via(streamingS3Client.uploader)
      .runWith(Sink.head)

  override def copy(bucketName: String, sourceKey: String, destKey: String)(implicit m: Materializer) =
    copy(bucketName, sourceKey, bucketName, destKey)

  override def copy(sourceBucketName: String, sourceKey: String, destBucketName: String, destKey: String)
                   (implicit m: Materializer) =

      Source.single((BucketNameAndKey(sourceBucketName, sourceKey), BucketNameAndKey(destBucketName, destKey)))
        .via(streamingS3Client.copier)
        .runWith(Sink.head)

  override def listObjects(bucketName: String)(implicit m: Materializer) =
    Source.single((bucketName, None))
      .via(streamingS3Client.objectLister)
      .runWith(Sink.seq)

  override def listObjects(bucketName: String, prefix: String)(implicit m: Materializer) =
    Source.single((bucketName, Some(prefix)))
      .via(streamingS3Client.objectLister)
      .runWith(Sink.seq)

  override def deleteObject(bucketName: String, key: String)(implicit m: Materializer) =
      Source.single(BucketNameAndKey(bucketName, key))
        .via(streamingS3Client.objectDeleter)
        .runWith(Sink.head)

  override def download[T: DownloadSink](bucketName: String, key: String)(implicit m: Materializer) =
    Source.single(BucketNameAndKey(bucketName, key))
      .via(streamingS3Client.downloader)
      .runWith(Sink.head)

  override def downloadTo(bucketName: String, key: String, file: File)(implicit m: Materializer) =
    Source.single((BucketNameAndKey(bucketName, key), file))
      .via(streamingS3Client.fileDownloader)
      .runWith(Sink.head)

  override def getUrl(bucketName: String, key: String)(implicit m: Materializer) =
    Source.single(BucketNameAndKey(bucketName, key))
      .via(streamingS3Client.objectUrlGetter)
      .runWith(Sink.head)
}
