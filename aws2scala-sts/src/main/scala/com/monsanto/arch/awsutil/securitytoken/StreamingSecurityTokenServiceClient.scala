package com.monsanto.arch.awsutil.securitytoken

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.monsanto.arch.awsutil.StreamingAwsClient
import com.monsanto.arch.awsutil.securitytoken.model.{AssumeRoleRequest, AssumeRoleResult}

trait StreamingSecurityTokenServiceClient extends StreamingAwsClient {
  /** Returns a flow that will submit requests to assume roles to AWS and emit the result. */
  def roleAssumer: Flow[AssumeRoleRequest, AssumeRoleResult, NotUsed]
}
