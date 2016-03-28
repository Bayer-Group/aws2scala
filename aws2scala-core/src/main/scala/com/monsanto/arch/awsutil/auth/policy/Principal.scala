package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}

private[awsutil] case class Principal(provider: String, id: String) {
  def toAws: aws.Principal = new aws.Principal(provider, id)
}

private[awsutil] object Principal {
  def fromAws(principal: aws.Principal): Principal = Principal(principal.getProvider, principal.getId)
}
