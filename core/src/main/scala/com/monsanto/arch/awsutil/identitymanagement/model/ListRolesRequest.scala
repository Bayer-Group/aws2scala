package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.model.{ListRolesRequest ⇒ AwsListRolesRequest}

/** Requests a listing of IAM roles.
  *
  * @param pathPrefix an optional path prefix for filtering the results
  */
case class ListRolesRequest private (pathPrefix: Option[String]) {
  def toAws: AwsListRolesRequest = {
    val aws = new AwsListRolesRequest
    pathPrefix.foreach(p ⇒ aws.setPathPrefix(p))
    aws
  }
}

object ListRolesRequest {
  /** Requests all roles. */
  val allRoles: ListRolesRequest = ListRolesRequest(None)

  /** Requests roles with the given path prefix. */
  def withPathPrefix(pathPrefix: String): ListRolesRequest = ListRolesRequest(Some(pathPrefix))
}
