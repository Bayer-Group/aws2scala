package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}

import scala.collection.JavaConverters._

private[awsutil] case class Policy(id: Option[String], statements: Seq[Statement]) {
  def toAws: aws.Policy = {
    val policy = new aws.Policy()
    id.foreach(id ⇒ policy.setId(id))
    policy.setStatements(statements.map(_.toAws).asJavaCollection)
    policy
  }
  override def toString = toAws.toJson
}

private[awsutil] object Policy {
  def fromAws(policy: aws.Policy): Policy =
    Policy(
      Option(policy.getId),
      policy.getStatements.asScala.map(Statement.fromAws).toList)

  def fromString(str: String): Policy = fromAws(aws.Policy.fromJson(str))
}
