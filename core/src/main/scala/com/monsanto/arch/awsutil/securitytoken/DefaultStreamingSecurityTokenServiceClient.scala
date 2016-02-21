package com.monsanto.arch.awsutil.securitytoken

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceAsync
import com.amazonaws.services.securitytoken.model.{AssumeRoleResult ⇒ AwsAssumeRoleResult}
import com.monsanto.arch.awsutil.AWSFlow
import com.monsanto.arch.awsutil.securitytoken.model.{AssumeRoleRequest, AssumeRoleResult}

private[awsutil] class DefaultStreamingSecurityTokenServiceClient(aws: AWSSecurityTokenServiceAsync) extends StreamingSecurityTokenServiceClient {
  override def roleAssumer =
    Flow[AssumeRoleRequest]
      .map(_.toAws)
      .via[AwsAssumeRoleResult, NotUsed](AWSFlow.simple(aws.assumeRoleAsync))
      .map(AssumeRoleResult.fromAws)
      .named("STS.roleAssumer")
}
