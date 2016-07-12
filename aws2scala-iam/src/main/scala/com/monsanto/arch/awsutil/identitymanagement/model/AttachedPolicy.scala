package com.monsanto.arch.awsutil.identitymanagement.model

import akka.Done
import akka.stream.Materializer
import com.monsanto.arch.awsutil.identitymanagement.AsyncIdentityManagementClient

import scala.concurrent.Future

/** Contains information about an attached policy. */
sealed trait AttachedPolicy {
  /** Returns the friendly name for the policy. */
  def name: String

  /** Returns the ARN specifying the policy. */
  def arn: PolicyArn

  /** Requests that the policy be detached. */
  def detach()(implicit m: Materializer, client: AsyncIdentityManagementClient): Future[Done]
}

case class AttachedRolePolicy(name: String, arn: PolicyArn, roleName: String) extends AttachedPolicy {
  override def detach()(implicit m: Materializer, client: AsyncIdentityManagementClient): Future[Done] = ???
}
