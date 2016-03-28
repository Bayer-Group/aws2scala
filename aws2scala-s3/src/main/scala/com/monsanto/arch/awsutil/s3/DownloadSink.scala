package com.monsanto.arch.awsutil.s3

import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.amazonaws.util.IOUtils

import scala.concurrent.{ExecutionContext, Future, blocking}

/** Type class to assist in converting an input stream to an instance of a type. */
trait DownloadSink[T] {
  def apply(objectInputStream: S3ObjectInputStream)(implicit ec: ExecutionContext): Future[T]
}

object DownloadSink {
  /** Downloads the stream to an array of bytes. */
  implicit val bytesSink = new DownloadSink[Array[Byte]] {
    override def apply(in: S3ObjectInputStream)(implicit ec: ExecutionContext) =
      Future {
        blocking {
          try {
            IOUtils.toByteArray(in)
          } finally in.close()
        }
      }
  }

  /** Downloads the stream to a string. */
  implicit val stringSink = new DownloadSink[String] {
    override def apply(in: S3ObjectInputStream)(implicit ec: ExecutionContext) =
      Future {
        blocking {
          try {
            IOUtils.toString(in)
          } finally in.close()
        }
      }
  }
}
