package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.converters.CoreConverters._

case class Policy(id: Option[String], statements: Seq[Statement]) {
  def toJson: String = this.asAws.toJson
}

object Policy {
  def fromJson(json: String): Policy = aws.Policy.fromJson(json).asScala
}
