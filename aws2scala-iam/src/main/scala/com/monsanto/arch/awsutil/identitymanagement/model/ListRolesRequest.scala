package com.monsanto.arch.awsutil.identitymanagement.model

/** Requests a listing of IAM roles.
  *
  * @param prefix an optional path prefix for filtering the results
  */
case class ListRolesRequest private (prefix: Option[Path])

object ListRolesRequest {
  /** Requests all roles. */
  val allRoles: ListRolesRequest = ListRolesRequest(None)

  /** Requests roles with the given path prefix. */
  def withPrefix(prefix: Path): ListRolesRequest = ListRolesRequest(Some(prefix))
}
