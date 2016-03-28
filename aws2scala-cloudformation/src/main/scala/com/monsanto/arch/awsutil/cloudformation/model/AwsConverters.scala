package com.monsanto.arch.awsutil.cloudformation.model

import com.amazonaws.services.cloudformation.{model ⇒ aws}

import scala.collection.JavaConverters._

object AwsConverters {
  implicit class ScalaDeleteStackRequest(val request: DeleteStackRequest) extends AnyVal {
    def asAws: aws.DeleteStackRequest =
      new aws.DeleteStackRequest()
        .withStackName(request.stackNameOrId)
        .withRetainResources(request.retainResources.asJava)
  }
}
