package com.monsanto.arch.awsutil.identitymanagement.model

private[awsutil] case class UserId(value: String) {
  require(value.matches("^AID[0-9A-Z]{18}$"))
}
