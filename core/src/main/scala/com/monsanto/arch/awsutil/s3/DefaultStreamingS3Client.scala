package com.monsanto.arch.awsutil.s3

import java.io.File
import java.net.URL
import java.util.concurrent.{Future ⇒ JFuture}

import akka.actor.Cancellable
import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl._
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.regions.ServiceAbbreviations
import com.amazonaws.services.s3.model.{ProgressListener ⇒ _, _}
import com.amazonaws.services.s3.transfer.{Download, TransferManager}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client}
import com.monsanto.arch.awsutil.s3.DefaultStreamingS3Client._
import com.monsanto.arch.awsutil.s3.model.BucketNameAndKey
import com.monsanto.arch.awsutil.{AWSAsyncCall, AWSFlow, Settings}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.concurrent.duration.{Duration, TimeUnit}
import scala.concurrent.{Await, ExecutionContext, Future, blocking}
import scala.util.{Failure, Success}

private[awsutil] class DefaultStreamingS3Client(s3client: AmazonS3, transferManager: TransferManager, settings: Settings)
                                               (implicit ec: ExecutionContext) extends StreamingS3Client with LazyLogging {
  /** The S3 region derived from the settings. */
  private val region = {
    val s3endpoint = settings.region.getServiceEndpoint(ServiceAbbreviations.S3)
    s3endpoint match {
      case "s3.amazonaws.com" | "s3-external-1.amazonaws.com" =>
        // <sigh> the Region.S3_REGIONAL_ENDPOINT_PATTERN fails with the standard region
        Region.US_Standard
      case RegionRegex(regionStr, _) =>
        Region.fromValue(regionStr)
    }
  }

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

  /** Encapsulates the process for generating a bucket and applying the default bucket policy. */
  private val createBucketGraph = GraphDSL.create() { implicit b =>
    val createBucket = b.add(
      Flow[String]
        .map(new CreateBucketRequest(_, region))
        .mapAsync(parallelism)(asAsync(s3client.createBucket)))

    val outlet = settings.s3.defaultBucketPolicy match {
      case None =>
        // no default bucket policy, just create the bucket and go
        createBucket.outlet
      case Some(policy) =>
        // if there is a bucket policy, things are a little more tricky.  We only need the bucket name for setting the
        // policy, so we split the flow so that the bucket can be returned at the end

        // duplicates the bucket to two streams
        val splitter = b.add(Broadcast[Bucket](2))

        // given a bucket, returns a tuple of the bucket name and the policy to apply
        val toNameAndPolicyTuple = b.add(Flow[Bucket].map { bucket =>
          val name = bucket.getName
          (name, Some(policy.replace("@BUCKET_NAME@", name)))
        })
        // sets the policy on the bucket
        val setPolicy = b.add(bucketPolicySetter)
        // synchronises the stream with the duplicated bucket, producing a tuple of the bucket name and the bucket
        val zipper = b.add(Zip[String,Bucket]())
        // takes the tuple for the zipper and extracts the bucket
        val onlyBucket = b.add(Flow[(String,Bucket)].map(_._2))

        // the assembled flow
        createBucket ~> splitter ~> toNameAndPolicyTuple ~> setPolicy ~> zipper.in0; zipper.out ~> onlyBucket
        ;               splitter                         ~>              zipper.in1

        onlyBucket.outlet
    }

    FlowShape(createBucket.in, outlet)
  }

  override val bucketCreator = Flow.fromGraph(createBucketGraph).named("S3.bucketCreator")

  override val bucketDeleter =
    Flow[String]
      .mapAsync(parallelism)(asReturnInputAsync(s3client.deleteBucket(_: String)))
      .named("S3.bucketDeleter")

  val bucketDeleterWithExceptions =
    Flow[String]
      .mapAsync(parallelism)(capturingAwsExceptions(asReturnInputAsync(s3client.deleteBucket(_: String))))

  override val bucketLister =
    Source.single(new ListBucketsRequest)
      .mapAsync(parallelism)(asAsync(s3client.listBuckets))
      .mapConcat(_.asScala.toList)
      .named("S3.bucketLister")

  override val rawObjectLister =
    AWSFlow.pagedByNextMarker(AsyncObjectLister)
      .mapConcat(_.getObjectSummaries.asScala.toList)
      .named("S3.rawObjectLister")

  override val objectLister =
    Flow[(String, Option[String])]
      .map(args ⇒ new ListObjectsRequest(args._1, args._2.orNull, null, null, null))
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
              new BucketTaggingConfiguration(Seq(new TagSet(tags.asJava)).asJavaCollection))
          }
          bucketName
        }
      }
      .named("S3.bucketTagsSetter")

  override val rawUploader =
    Flow[PutObjectRequest]
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
        val r = new DeleteObjectRequest(o.bucketName, o.key)
        s3client.deleteObject(r)
        o
      })
      .flatMapConcat { o ⇒
        Source.tick(Duration.Zero, settings.s3.uploadCheckInterval, (o.bucketName, Some(o.key)))
          .flatMapConcat { spec ⇒
            Source.single(spec)
              .via(objectLister)
              .filter(os ⇒ os.getBucketName  == o.bucketName && os.getKey == o.key)
              .fold(Seq.empty[S3ObjectSummary])(_ :+ _)
          }
          .filter(_.isEmpty)
          .map(_ ⇒ o)
          .take(1)
          .takeWithin(settings.s3.uploadCheckTimeout)
      }
      .named("S3.objectDeleter")

  override val rawCopier =
    Flow[CopyObjectRequest]
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
          new CopyObjectRequest(source.bucketName, source.key, destination.bucketName, destination.key)
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
              val r = new DeleteObjectRequest(o.getBucketName, o.getKey)
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
    Flow[GetObjectRequest]
      .mapAsync(parallelism)(asAsync(s3client.getObject))
      .map(_.getObjectContent)
      .named("S3.rawDownloader")

  override def downloader[T](implicit downloadSink: DownloadSink[T]) =
    Flow[BucketNameAndKey]
      .map(x ⇒ new GetObjectRequest(x.bucketName, x.key))
      .via(rawDownloader)
      .mapAsync(parallelism)(downloadSink.apply)
      .named("S3.downloader")

  override val rawFileDownloader =
    Flow.fromGraph(GraphDSL.create() { implicit b ⇒
      val broadcast = b.add(Broadcast[(GetObjectRequest,File)](2))
      val merge = b.add(ZipWith[(GetObjectRequest,File), Download, File]((x, _) => x._2))
      val downloader = Flow[(GetObjectRequest,File)]
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
      .map(args ⇒ (new GetObjectRequest(args._1.bucketName, args._1.key), args._2))
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
            new URL(s"https://${o.bucketName}.${settings.region.getServiceEndpoint(ServiceAbbreviations.S3)}/${o.key}")
          }
          .named("S3.pseudoObjectUrlGetter")
    }

  private def applyPutObjectDefaults(request: PutObjectRequest): PutObjectRequest = {
    val metadata = applyDefaultHeaders(request.getMetadata, settings.s3.defaultPutObjectHeaders)
    request.withMetadata(metadata)
  }

  private def applyCopyObjectDefaults(request: CopyObjectRequest): CopyObjectRequest = {
    val metadata = applyDefaultHeaders(request.getNewObjectMetadata, settings.s3.defaultCopyObjectHeaders)
    request.withNewObjectMetadata(metadata)
  }

  private def applyDefaultHeaders(metadata: ObjectMetadata, defaults: Map[String,AnyRef]): ObjectMetadata = {
    val defaultedMetadata = Option(metadata).getOrElse(new ObjectMetadata())
    defaults.foreach { case (key, defaultValue) ⇒
      val value = defaultedMetadata.getRawMetadataValue(key)
      if (value == null) {
        defaultedMetadata.setHeader(key, defaultValue)
      }
    }
    defaultedMetadata
  }

  /** Given an upload result, return a source that will emit an object summary once the upload result is visible. */
  private def pollForObjectSummary(bucketAndKey: (String, String)): Source[S3ObjectSummary,Cancellable] = {
    val (bucketName, key) = bucketAndKey
    val listObjectsRequest = new ListObjectsRequest(bucketName, key, null, null, null)

    Source.tick(Duration.Zero, settings.s3.uploadCheckInterval, listObjectsRequest)
      .flatMapConcat(r ⇒ Source.single(r).via(rawObjectLister))
      .filter(summary ⇒ summary.getBucketName == bucketName && summary.getKey == key)
      .take(1)
      .completionTimeout(settings.s3.uploadCheckTimeout)
      .named("pollForObjectSummary")
  }

  /** Adapts S3’s `listObjects` so that it can be used as an [[AWSAsyncCall]]. */
  private object AsyncObjectLister extends AWSAsyncCall[ListObjectsRequest, ObjectListing] {
    private val AsyncCall = asAsync(s3client.listObjects(_: ListObjectsRequest))

    override def apply(request: ListObjectsRequest, handler: AsyncHandler[ListObjectsRequest, ObjectListing]) =  {
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
      new JFuture[ObjectListing] {
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

  private def capturingAwsExceptions[In,Out](f:In => Future[Out]): In =>Future[(Option[AmazonS3Exception],Option[Out])] = {
   in:In =>
    f(in).map(x=>(None,Some(x))).recover{case e:AmazonS3Exception => (Some(e),None)}
  }

  private def asAsync[In, Out](fn: In => Out): In => Future[Out] = { in: In =>
    import scala.concurrent.blocking
    Future(blocking(fn(in)))
  }
}

private[awsutil] object DefaultStreamingS3Client {
  val RegionRegex = Region.S3_REGIONAL_ENDPOINT_PATTERN.pattern().r
}
