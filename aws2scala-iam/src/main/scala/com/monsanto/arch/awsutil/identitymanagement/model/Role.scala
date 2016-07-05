package com.monsanto.arch.awsutil.identitymanagement.model

import java.util.Date

import akka.Done
import akka.stream.Materializer
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.identitymanagement.AsyncIdentityManagementClient

import scala.concurrent.Future

/** Contains information about an IAM role.
  *
  * @param arn the ARN specifying the role
  * @param name a friendly name for the role
  * @param path the path for the role
  * @param id a stable and unique string identifying the role
  * @param assumeRolePolicyDocument the policy that grants an entity permission to assume the role
  * @param created the date and time when the role was created
  */
case class Role(arn: RoleArn,
                name: String,
                path: Path,
                id: String,
                assumeRolePolicyDocument: Policy,
                created: Date) {
  /** Requests deletion of this role.  It must not have any policies attached. */
  def delete()(implicit m: Materializer, client: AsyncIdentityManagementClient): Future[Done] =
    client.deleteRole(name)

  /** Attaches the managed policy to this role. */
  def attachPolicy(policyArn: PolicyArn)
                  (implicit m: Materializer, client: AsyncIdentityManagementClient): Future[Done] =
    client.attachRolePolicy(name, policyArn)

  /** Detaches the managed policy from this role. */
  def detachPolicy(policyArn: PolicyArn)
                  (implicit m: Materializer, client: AsyncIdentityManagementClient): Future[Done] =
    client.detachRolePolicy(name, policyArn)

  /** Lists all managed policies attached to this role. */
  def listAttachedPolicies()(implicit m: Materializer, client: AsyncIdentityManagementClient): Future[Seq[AttachedPolicy]] =
    client.listAttachedRolePolicies(name)

  /** Lists all managed policies attached to this role that match the given path prefix. */
  def listAttachedPolicies(prefix: Path)(implicit m: Materializer, client: AsyncIdentityManagementClient): Future[Seq[AttachedPolicy]] =
    client.listAttachedRolePolicies(name, prefix)
}
