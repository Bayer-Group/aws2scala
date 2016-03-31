package com.monsanto.arch.awsutil.s3.model

import com.amazonaws.services.s3.{model ⇒ aws}

object AwsConverters {
  implicit class ScalaCreateBucketRequest(val request: CreateBucketRequest) extends AnyVal {
    def asAws: aws.CreateBucketRequest =
      request.region match {
        case Some(r) ⇒ new aws.CreateBucketRequest(request.bucketName, r.toAws)
        case None ⇒ new aws.CreateBucketRequest(request.bucketName)
      }
  }
}
