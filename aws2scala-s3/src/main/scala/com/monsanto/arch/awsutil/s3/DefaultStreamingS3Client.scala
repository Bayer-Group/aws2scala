package com.monsanto.arch.awsutil.s3

import java.io.File
import java.net.URL
import java.util.concurrent.{Future ⇒ JFuture}

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl._
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.s3.transfer.{Download, TransferManager}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client, model ⇒ aws}
import com.monsanto.arch.awsutil.s3.model.AwsConverters._
import com.monsanto.arch.awsutil.s3.model.{BucketNameAndKey, CreateBucketRequest}
import com.monsanto.arch.awsutil._
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.concurrent.duration.{Duration, TimeUnit}
import scala.concurrent.{Await, ExecutionContext, Future, blocking}
import scala.util.{Failure, Success}

private[awsutil] class DefaultStreamingS3Client(s3client: AmazonS3, transferManager: TransferManager, settings: AwsSettings)
                                               (implicit ec: ExecutionContext) extends StreamingS3Client with LazyLogging {
  private val parallelism = settings.s3.parallelism

  override val bucketPolicySetter =
    Flow[(String, Option[String])]
      .mapAsync(parallelism) {
        asAsync { case (bucketName, maybePolicy) =>
          maybePolicy match {
            case None =>
              s3client.deleteBucketPolicy(bucketName)
            case Some(policyText) =>
              s3client.setBucketPolicy(bucketName, policyText)
          }
          bucketName
        }
      }
      .named("S3.bucketPolicySetter")

  override val bucketDeleter =
    Flow[String]
      .mapAsync(parallelism)(asReturnInputAsync(s3client.deleteBucket(_: String)))
      .named("S3.bucketDeleter")

  val bucketDeleterWithExceptions =
    Flow[String]
      .mapAsync(parallelism)(capturingAwsExceptions(asReturnInputAsync(s3client.deleteBucket(_: String))))

  override val bucketLister =
    Source.single(NotUsed.getInstance())
      .map(_ ⇒ new aws.ListBucketsRequest)
      .mapAsync(parallelism)(asAsync(s3client.listBuckets))
      .mapConcat(_.asScala.toList)
      .map(_.asScala)
      .named("S3.bucketLister")

  override val bucketCreator =
    Flow[CreateBucketRequest]
      .map(_.asAws)
      .mapAsync(parallelism)(asAsync(s3client.createBucket(_: aws.CreateBucketRequest)))
      .map(_.getName)
      .flatMapConcat(name ⇒ bucketLister.filter(_.name == name))
      .named("S3.bucketCreator")

  override val rawObjectLister =
    AWSFlow.pagedByNextMarker(AsyncObjectLister)
      .mapConcat(_.getObjectSummaries.asScala.toList)
      .named("S3.rawObjectLister")

  override val objectLister =
    Flow[(String, Option[String])]
      .map(args ⇒ new aws.ListObjectsRequest(args._1, args._2.orNull, null, null, null))
      .via(rawObjectLister)
      .named("S3.objectLister")

  override val bucketExistenceChecker =
    Flow[String]
      .mapAsync(parallelism)(asAsync(s3client.doesBucketExist))
      .named("S3.bucketExistenceChecker")

  override val bucketPolicyGetter =
    Flow[String]
      .mapAsync(parallelism)(asAsync(s3client.getBucketPolicy))
      .map(r => Option(r.getPolicyText))
      .named("S3.bucketPolicyGetter")

  override val bucketTagsGetter =
    Flow[String]
      .mapAsync(parallelism) {
        asAsync { bucketName ⇒
          val tagging = Option(s3client.getBucketTaggingConfiguration(bucketName))
          tagging.map { config ⇒
            config.getAllTagSets.asScala
              .map(_.getAllTags.asScala.toMap)
              .reduce(_ ++ _)
          }.getOrElse(Map.empty)
        }
      }
      .named("S3.bucketTagsGetter")

  override val bucketTagsSetter =
    Flow[(String, Map[String,String])]
      .mapAsync(parallelism) {
        asAsync { args ⇒
          val (bucketName, tags) = args
          if (tags.isEmpty) {
            s3client.deleteBucketTaggingConfiguration(bucketName)
          } else {
            s3client.setBucketTaggingConfiguration(
              bucketName,
              new aws.BucketTaggingConfiguration(Seq(new aws.TagSet(tags.asJava)).asJavaCollection))
          }
          bucketName
        }
      }
      .named("S3.bucketTagsSetter")

  override val rawUploader =
    Flow[aws.PutObjectRequest]
      .map(applyPutObjectDefaults)
      .mapAsync(parallelism)(asAsync(transferManager.upload))
      .mapAsync(parallelism) { upload ⇒
        Future(blocking(upload.waitForUploadResult()))
      }
      .map(result ⇒ (result.getBucketName, result.getKey))
      .flatMapConcat(pollForObjectSummary)
      .named("S3.rawUploader")

  override def uploader[T](implicit uploadable: UploadSource[T]) =
    Flow[(BucketNameAndKey,T)]
      .map(args ⇒ uploadable(args._1, args._2))
      .via(rawUploader)
      .named("S3.uploader")

  override val objectDeleter =
    Flow[BucketNameAndKey]
      .mapAsync(parallelism)(asAsync{ o ⇒
        val r = new aws.DeleteObjectRequest(o.bucketName, o.key)
        s3client.deleteObject(r)
        o
      })
      .flatMapConcat { o ⇒
        Source.tick(Duration.Zero, settings.s3.uploadCheckInterval, (o.bucketName, Some(o.key)))
          .flatMapConcat { spec ⇒
            Source.single(spec)
              .via(objectLister)
              .filter(os ⇒ os.getBucketName  == o.bucketName && os.getKey == o.key)
              .fold(Seq.empty[aws.S3ObjectSummary])(_ :+ _)
          }
          .filter(_.isEmpty)
          .map(_ ⇒ o)
          .take(1)
          .takeWithin(settings.s3.uploadCheckTimeout)
      }
      .named("S3.objectDeleter")

  override val rawCopier =
    Flow[aws.CopyObjectRequest]
      .map(applyCopyObjectDefaults)
      .mapAsync(parallelism) {
        asAsync { request ⇒
          s3client.copyObject(request)
          (request.getDestinationBucketName, request.getDestinationKey)
        }
      }
      .flatMapConcat(pollForObjectSummary)
      .named("S3.rawCopier")

  override val copier =
    Flow[(BucketNameAndKey, BucketNameAndKey)]
      .map { case (source, destination) ⇒
          new aws.CopyObjectRequest(source.bucketName, source.key, destination.bucketName, destination.key)
      }
      .via(rawCopier)
      .named("S3.copier")

  override val bucketEmptier = {
    Flow.fromGraph(GraphDSL.create() { implicit builder ⇒
      val broadcast = builder.add(Broadcast[String](2))
      val merge = builder.add(ZipWith[String, Unit, String]((x, _) ⇒ x))
      val deleter = Flow[String]
        .flatMapConcat { bucketName ⇒
          Source.single((bucketName, None))
            .via(objectLister)
            .mapAsyncUnordered(parallelism)(asAsync{ o ⇒
              val r = new aws.DeleteObjectRequest(o.getBucketName, o.getKey)
              s3client.deleteObject(r)
              o
            })
            .fold(())((_,_) ⇒ ())
        }
      broadcast ~> merge.in0
      broadcast ~> deleter ~> merge.in1
      FlowShape(broadcast.in, merge.out)
    })
    .named("S3.bucketEmptier")
  }

  override val bucketEmptierAndDeleter = bucketEmptier.via(bucketDeleter).named("S3.bucketEmptierAndDeleter")

  override val rawDownloader =
    Flow[aws.GetObjectRequest]
      .mapAsync(parallelism)(asAsync(s3client.getObject))
      .map(_.getObjectContent)
      .named("S3.rawDownloader")

  override def downloader[T](implicit downloadSink: DownloadSink[T]) =
    Flow[BucketNameAndKey]
      .map(x ⇒ new aws.GetObjectRequest(x.bucketName, x.key))
      .via(rawDownloader)
      .mapAsync(parallelism)(downloadSink.apply)
      .named("S3.downloader")

  override val rawFileDownloader =
    Flow.fromGraph(GraphDSL.create() { implicit b ⇒
      val broadcast = b.add(Broadcast[(aws.GetObjectRequest,File)](2))
      val merge = b.add(ZipWith[(aws.GetObjectRequest,File), Download, File]((x, _) => x._2))
      val downloader = Flow[(aws.GetObjectRequest,File)]
        .map(args ⇒ transferManager.download(args._1, args._2))
        .mapAsync(parallelism) { download ⇒
          Future(blocking(download.waitForCompletion())).map(_ ⇒ download)
        }
      broadcast.out(0) ~> merge.in0
      broadcast.out(1) ~> downloader ~> merge.in1
      FlowShape(broadcast.in, merge.out)
    }).named("S3.rawFileDownloader")

  override val fileDownloader =
    Flow[(BucketNameAndKey,File)]
      .map(args ⇒ (new aws.GetObjectRequest(args._1.bucketName, args._1.key), args._2))
      .via(rawFileDownloader)
      .named("S3.fileDownloader")

  override val objectUrlGetter =
    s3client match {
      case concreteClient: AmazonS3Client ⇒
        Flow[BucketNameAndKey]
          .mapAsync(parallelism) {
            asAsync(o ⇒ concreteClient.getUrl(o.bucketName, o.key))
          }
          .named("S3.objectUrlGetter")
      case _ ⇒
        logger.warn("Not using concrete Amazon S3 client, getUrl results may be incorrect")
        Flow[BucketNameAndKey]
          .map { o ⇒
            new URL(s"https://${o.bucketName}.${settings.region.getServiceEndpoint(AmazonS3.ENDPOINT_PREFIX)}/${o.key}")
          }
          .named("S3.pseudoObjectUrlGetter")
    }

  private def applyPutObjectDefaults(request: aws.PutObjectRequest): aws.PutObjectRequest = {
    val metadata = applyDefaultHeaders(request.getMetadata, settings.s3.defaultPutObjectHeaders)
    request.withMetadata(metadata)
  }

  private def applyCopyObjectDefaults(request: aws.CopyObjectRequest): aws.CopyObjectRequest = {
    val metadata = applyDefaultHeaders(request.getNewObjectMetadata, settings.s3.defaultCopyObjectHeaders)
    request.withNewObjectMetadata(metadata)
  }

  private def applyDefaultHeaders(metadata: aws.ObjectMetadata, defaults: Map[String,AnyRef]): aws.ObjectMetadata = {
    val defaultedMetadata = Option(metadata).getOrElse(new aws.ObjectMetadata())
    defaults.foreach { case (key, defaultValue) ⇒
      val value = defaultedMetadata.getRawMetadataValue(key)
      if (value == null) {
        defaultedMetadata.setHeader(key, defaultValue)
      }
    }
    defaultedMetadata
  }

  /** Given an upload result, return a source that will emit an object summary once the upload result is visible. */
  private def pollForObjectSummary(bucketAndKey: (String, String)): Source[aws.S3ObjectSummary,Cancellable] = {
    val (bucketName, key) = bucketAndKey
    val listObjectsRequest = new aws.ListObjectsRequest(bucketName, key, null, null, null)

    Source.tick(Duration.Zero, settings.s3.uploadCheckInterval, listObjectsRequest)
      .flatMapConcat(r ⇒ Source.single(r).via(rawObjectLister))
      .filter(summary ⇒ summary.getBucketName == bucketName && summary.getKey == key)
      .take(1)
      .completionTimeout(settings.s3.uploadCheckTimeout)
      .named("pollForObjectSummary")
  }

  /** Adapts S3’s `listObjects` so that it can be used as an [[AWSAsyncCall]]. */
  private object AsyncObjectLister extends AWSAsyncCall[aws.ListObjectsRequest, aws.ObjectListing] {
    private val AsyncCall = asAsync(s3client.listObjects(_: aws.ListObjectsRequest))

    override def apply(request: aws.ListObjectsRequest, handler: AsyncHandler[aws.ListObjectsRequest, aws.ObjectListing]) =  {
      val eventualObjectListing = AsyncCall(request)
      eventualObjectListing.onComplete {
        case Success(listing) ⇒
          handler.onSuccess(request, listing)
        case Failure(cause: Exception) ⇒
          handler.onError(cause)
        case Failure(rootCause) ⇒
          // $COVERAGE-OFF$ this should never occur, included to make the match comprehensive
          // note that Failure wraps a `Throwable`, so wrap it here.
          handler.onError(new RuntimeException("Operation failed", rootCause))
          // $COVERAGE-ON$ this should never occur, included to make the match comprehensive
      }
      // $COVERAGE-OFF$ theoretically, this will be invoked in AWSGraphStage to attempt to cancel a future
      new JFuture[aws.ObjectListing] {
        /** Cancellation is not supported. */
        override def isCancelled = false

        override def get() = Await.result(eventualObjectListing, Duration.Inf)

        override def get(timeout: Long, unit: TimeUnit) = Await.result(eventualObjectListing, Duration(timeout, unit))

        /** Cancellation is not supported. */
        override def cancel(mayInterruptIfRunning: Boolean) = false

        override def isDone = eventualObjectListing.isCompleted
      }
      // $COVERAGE-ON$
    }
  }

  private def asReturnInputAsync[In](fn: In => Unit): In => Future[In] = asAsync { in =>
    fn(in)
    in
  }

  private def capturingAwsExceptions[In,Out](f:In => Future[Out]): In =>Future[(Option[aws.AmazonS3Exception],Option[Out])] = {
   in:In =>
    f(in).map(x=>(None,Some(x))).recover{case e:aws.AmazonS3Exception => (Some(e),None)}
  }

  private def asAsync[In, Out](fn: In => Out): In => Future[Out] = { in: In =>
    import scala.concurrent.blocking
    Future(blocking(fn(in)))
  }
}

private[awsutil] object DefaultStreamingS3Client {
  val RegionRegex = aws.Region.S3_REGIONAL_ENDPOINT_PATTERN.pattern().r
}
