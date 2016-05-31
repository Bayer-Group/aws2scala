package com.monsanto.arch.awsutil.identitymanagement

import akka.Done
import akka.stream.Materializer
import com.monsanto.arch.awsutil.AsyncAwsClient
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.identitymanagement.model._

import scala.concurrent.Future

trait AsyncIdentityManagementClient extends AsyncAwsClient {
  /** Creates a new role.
    *
    * @param roleName the name of the new role
    * @param assumeRolePolicy the trust relationship policy that grants an entity permission to assume the role (a JSON
    *                         document)
    */
  def createRole(roleName: String, assumeRolePolicy: Policy)(implicit m: Materializer): Future[Role]

  /** Creates a new role.
    *
    * @param roleName the name of the new role
    * @param assumeRolePolicy the trust relationship policy that grants an entity permission to assume the role (a JSON
    *                         document)
    * @param path an optional path to the role
    */
  def createRole(roleName: String, assumeRolePolicy: Policy, path: Path)(implicit m: Materializer): Future[Role]

  /** Deletes the role with the given name.  The role must not have any policies attached. */
  def deleteRole(roleName: String)(implicit m: Materializer): Future[Done]

  /** Lists all roles. */
  def listRoles()(implicit m: Materializer): Future[Seq[Role]]

  /** Lists all roles with the given path prefix. */
  def listRoles(prefix: Path)(implicit m: Materializer): Future[Seq[Role]]

  /** Attaches the managed policy to a role. */
  def attachRolePolicy(roleName: String, policyArn: PolicyArn)(implicit m: Materializer): Future[Done]

  /** Detaches the managed policy from a role. */
  def detachRolePolicy(roleName: String, policyArn: PolicyArn)(implicit m: Materializer): Future[Done]

  /** Lists all managed policies attached to the given role. */
  def listAttachedRolePolicies(roleName: String)(implicit m: Materializer): Future[Seq[AttachedPolicy]]

  /** Lists all managed policies attached to the given role that match the given path prefix. */
  def listAttachedRolePolicies(roleName: String, prefix: Path)
                              (implicit m: Materializer): Future[Seq[AttachedPolicy]]

  /* Gets the information for the current IAM user. */
  def getCurrentUser()(implicit m: Materializer): Future[User]

  /* Gets the information for the IAM user with the given name. */
  def getUser(name: String)(implicit m: Materializer): Future[User]
}
