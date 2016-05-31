package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.auth.policy

/** Contains the information necessary to create a new role for an account.
  *
  * @param name the name of the role to create
  * @param assumeRolePolicy the trust relationship policy that grants an entity permission to assume the role
  * @param path the optional path to the role
  */
case class CreateRoleRequest(name: String, assumeRolePolicy: policy.Policy, path: Option[Path])
