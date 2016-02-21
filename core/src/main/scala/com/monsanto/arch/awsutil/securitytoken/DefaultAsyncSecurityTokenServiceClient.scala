package com.monsanto.arch.awsutil.securitytoken

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.monsanto.arch.awsutil.securitytoken.model.AssumeRoleRequest

private[awsutil] class DefaultAsyncSecurityTokenServiceClient(streaming: StreamingSecurityTokenServiceClient) extends AsyncSecurityTokenServiceClient {
  override def assumeRole(roleArn: String, sessionName: String)(implicit m: Materializer) =
    Source.single(AssumeRoleRequest(roleArn, sessionName))
      .via(streaming.roleAssumer)
      .map(_.credentials)
      .runWith(Sink.head)

  override def assumeRole(roleArn: String, sessionName: String, externalId: String)(implicit m: Materializer) =
    Source.single(AssumeRoleRequest(roleArn, sessionName, externalId = Some(externalId)))
      .via(streaming.roleAssumer)
      .map(_.credentials)
      .runWith(Sink.head)

  override def assumeRole(request: AssumeRoleRequest)(implicit m: Materializer) =
    Source.single(request)
      .via(streaming.roleAssumer)
      .runWith(Sink.head)
}
