package com.monsanto.arch.awsutil.identitymanagement.model

/** A request to get a user’s information.
  *
  * @param userName if specified, get the information for the user with the given name.  Otherwise, get the information
  *                 for the current IAM user
  */
case class GetUserRequest private (userName: Option[String])

object GetUserRequest {
  /** A request that will get the information for the current user. */
  val currentUser: GetUserRequest = GetUserRequest(None)

  /** Create a request that will get the information for a user with the given name. */
  def forUserName(userName: String): GetUserRequest = GetUserRequest(Some(userName))
}
