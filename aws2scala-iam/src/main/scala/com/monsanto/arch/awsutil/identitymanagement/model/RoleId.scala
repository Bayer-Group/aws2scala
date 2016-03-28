package com.monsanto.arch.awsutil.identitymanagement.model

private[awsutil] case class RoleId(value: String) {
  require(value.matches("^ARO[0-9A-Z]{18}$"))
}
