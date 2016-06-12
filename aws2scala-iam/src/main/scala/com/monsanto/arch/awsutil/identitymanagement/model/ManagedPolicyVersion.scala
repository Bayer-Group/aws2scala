package com.monsanto.arch.awsutil.identitymanagement.model

import java.util.Date

import akka.Done
import akka.stream.Materializer
import com.monsanto.arch.awsutil.auth.policy.Policy
import com.monsanto.arch.awsutil.identitymanagement.AsyncIdentityManagementClient

import scala.concurrent.Future

/** Contains information about a version of a managed policy.
  *
  * @param policyArn the ARN of the managed policy of which this is a version
  * @param document the policy document, if available, usually only as a result of getting a particular
  *                 policy version
  * @param versionId the identifier for the policy version
  * @param isDefaultVersion specifies whether the policy version is set as the policy’s default version
  * @param created the date and time when the policy version was created
  */
case class ManagedPolicyVersion(policyArn: PolicyArn,
                                document: Option[Policy],
                                versionId: String,
                                isDefaultVersion: Boolean,
                                created: Date) {
  /** Retrieves a new copy of this policy version.  This should have the
    * additional effect of retrieving the document.
    */
  def refresh()(implicit m: Materializer, client: AsyncIdentityManagementClient): Future[ManagedPolicyVersion] =
    client.getPolicyVersion(policyArn, versionId)

  /** Sets this version of the managed policy as the default (operational) version. */
  def setAsDefault()(implicit m: Materializer, client: AsyncIdentityManagementClient): Future[Done] =
    client.setDefaultPolicyVersion(policyArn, versionId)

  /** Deletes this version of the managed policy. */
  def delete()(implicit m: Materializer, client: AsyncIdentityManagementClient): Future[Done] =
    client.deletePolicyVersion(policyArn, versionId)
}
