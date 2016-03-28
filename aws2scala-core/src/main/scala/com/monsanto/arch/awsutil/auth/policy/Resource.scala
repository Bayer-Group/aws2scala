package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}

private[awsutil] case class Resource(id: String) {
  def toAws: aws.Resource = new aws.Resource(id)
}

private[awsutil] object Resource {
 def fromAws(resource: aws.Resource): Resource = Resource(resource.getId)
}
