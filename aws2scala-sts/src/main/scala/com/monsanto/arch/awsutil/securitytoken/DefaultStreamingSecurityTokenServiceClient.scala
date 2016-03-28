package com.monsanto.arch.awsutil.securitytoken

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.securitytoken.{AWSSecurityTokenServiceAsync, model ⇒ aws}
import com.monsanto.arch.awsutil.AWSFlow
import com.monsanto.arch.awsutil.securitytoken.model.AssumeRoleRequest
import com.monsanto.arch.awsutil.securitytoken.model.AwsConverters._

private[awsutil] class DefaultStreamingSecurityTokenServiceClient(sts: AWSSecurityTokenServiceAsync) extends StreamingSecurityTokenServiceClient {
  override def roleAssumer =
    Flow[AssumeRoleRequest]
      .map(_.asAws)
      .via[aws.AssumeRoleResult, NotUsed](AWSFlow.simple(sts.assumeRoleAsync))
      .map(_.asScala)
      .named("STS.roleAssumer")
}
