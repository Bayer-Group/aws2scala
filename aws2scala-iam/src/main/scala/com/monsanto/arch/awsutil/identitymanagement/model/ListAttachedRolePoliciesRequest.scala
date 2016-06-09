package com.monsanto.arch.awsutil.identitymanagement.model

/** Requests a listing fo the managed role policies attached to a request.
  *
  * @param roleName the friendly name of the role for which to list attached role policies
  * @param prefix an optional path prefix for filtering the results
  */
case class ListAttachedRolePoliciesRequest private (roleName: String, prefix: Option[Path])

object ListAttachedRolePoliciesRequest {
  /** Requests all managed policies attached to the given role. */
  def apply(roleName: String): ListAttachedRolePoliciesRequest = ListAttachedRolePoliciesRequest(roleName, None)

  /** Requests all managed policies attached to the given role that match the given path prefix. */
  def apply(roleName: String, pathPrefix: Path): ListAttachedRolePoliciesRequest =
    ListAttachedRolePoliciesRequest(roleName, Some(pathPrefix))
}
