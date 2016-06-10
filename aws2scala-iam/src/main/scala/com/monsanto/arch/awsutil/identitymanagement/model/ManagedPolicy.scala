package com.monsanto.arch.awsutil.identitymanagement.model

import java.util.Date

import akka.Done
import akka.stream.Materializer
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.identitymanagement.AsyncIdentityManagementClient

import scala.concurrent.Future

/** Contains information about a managed policy.
  *
  * Note that this is an immutable object and any ‘modification’ operations
  * will not result in this object being mutated.  You can always [[refresh()]]
  * to retrieve an updated version of this object.
  *
  * @param name the friendly name identifying the policy
  * @param id the stable and unique string identifying the policy
  * @param arn the Amazon Resource Name (ARN) that uniquely identifies this policy
  * @param path the path to the policy
  * @param defaultVersionId the identifier for the version of the policy that is set as the default version
  * @param attachmentCount the number of entities (user, groups, and roles) that the policy is attached to
  * @param attachable specifies whether the policy can be attached to an IAM user, group, or role
  * @param description a friendly description of the policy
  * @param created the date and time when the policy was created
  * @param updated the date and time when the policy was last updated.  When a policy has only one version, this field
  *                contains the date and time when the policy was created.  When a policy has more than one version,
  *                this field contains the date and time when the most recent policy version was created.
  */
case class ManagedPolicy(name: String,
                         id: String,
                         arn: PolicyArn,
                         path: Path,
                         defaultVersionId: String,
                         attachmentCount: Int,
                         attachable: Boolean,
                         description: Option[String],
                         created: Date,
                         updated: Date) {
  /** Creates a new version for this managed policy. */
  def createVersion(document: Policy, setAsDefault: Boolean)
                   (implicit m: Materializer, client: AsyncIdentityManagementClient): Future[ManagedPolicyVersion] =
    client.createPolicyVersion(arn, document, setAsDefault)

  /** Gets the managed policy version corresponding to the given version identifier. */
  def getVersion(versionId: String)
                (implicit m: Materializer, client: AsyncIdentityManagementClient): Future[ManagedPolicyVersion] =
    client.getPolicyVersion(arn, versionId)

  /** Lists all of the versions for this policy. */
  def versions()(implicit m: Materializer, client: AsyncIdentityManagementClient): Future[Seq[ManagedPolicyVersion]] =
    client.listPolicyVersions(arn)

  /** Deletes the given version of this policy. */
  def deleteVersion(version: ManagedPolicyVersion)
                   (implicit m: Materializer, client: AsyncIdentityManagementClient): Future[Done] =
    ensuringCorrectPolicy(version, deleteVersion(_: String))

  /** Deletes the version of this policy identified by the given version identifier. */
  def deleteVersion(versionId: String)
                   (implicit m: Materializer, client: AsyncIdentityManagementClient): Future[Done] =
    client.deletePolicyVersion(arn, versionId)

  /** Sets the default version of this policy to given version.
    *
    * Note that you will need to [[refresh()]] to get a policy which reflects the update.
    */
  def setDefaultVersion(version: ManagedPolicyVersion)
                       (implicit m: Materializer, client: AsyncIdentityManagementClient): Future[Done] =
    ensuringCorrectPolicy(version, setDefaultVersion(_: String))

  /** Sets the default version of this policy to given version.
    *
    * Note that you will need to [[refresh()]] to get a policy which reflects the update.
    */
  def setDefaultVersion(versionId: String)
                       (implicit m: Materializer, client: AsyncIdentityManagementClient): Future[Done] =
    client.setDefaultPolicyVersion(arn, versionId)

  /** Requests deletion of this managed policy. */
  def delete()(implicit m: Materializer, client: AsyncIdentityManagementClient): Future[Done] =
    client.deletePolicy(arn)

  /** Fetches a new snapshot of this policy from AWS. */
  def refresh()(implicit m: Materializer, client: AsyncIdentityManagementClient): Future[ManagedPolicy] =
    client.getPolicy(arn)

  private def ensuringCorrectPolicy[T](version: ManagedPolicyVersion, fn: String ⇒ T): T = {
    if (version.policyArn != arn) {
      throw new IllegalArgumentException("The managed policy version provided does not belong to this managed policy.")
    }
    fn(version.versionId)
  }
}
