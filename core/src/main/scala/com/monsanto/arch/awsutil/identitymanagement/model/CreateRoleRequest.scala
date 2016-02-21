package com.monsanto.arch.awsutil.identitymanagement.model

import com.amazonaws.services.identitymanagement.model.{CreateRoleRequest ⇒ AwsCreateRoleRequest}

/** Contains the information necessary to create a new role for an account.
  *
  * @param name the name of the role to create
  * @param assumeRolePolicy the trust relationship policy that grants an entity permission to assume the role (a JSON
  *                         document)
  * @param path the optional path to the role
  */
case class CreateRoleRequest(name: String, assumeRolePolicy: String, path: Option[String]) {
  /** Creates the AWS `CreateRoleRequest` corresponding to this object. */
  def toAws: AwsCreateRoleRequest = {
    val aws = new AwsCreateRoleRequest()
      .withRoleName(name)
      .withAssumeRolePolicyDocument(assumeRolePolicy)
    path.foreach(p ⇒ aws.setPath(p))
    aws
  }
}

object CreateRoleRequest {
  /** Creates the `CreateRoleRequest` object corresponding to the AWs instance. */
  def fromAws(aws: AwsCreateRoleRequest): CreateRoleRequest =
    CreateRoleRequest(aws.getRoleName, aws.getAssumeRolePolicyDocument, Option(aws.getPath))
}
