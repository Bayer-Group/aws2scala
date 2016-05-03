package com.monsanto.arch.awsutil.auth.policy

import java.util.{List ⇒ JList}

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.auth.policy.AwsConverters._
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

import scala.collection.JavaConverters._

private[awsutil] case class Statement(id: Option[String],
                                      principals: Seq[Principal],
                                      effect: Statement.Effect,
                                      actions: Seq[Action],
                                      resources: Seq[Resource],
                                      conditions: Seq[Condition]) {
  def toAws: aws.Statement = {
    val statement = new aws.Statement(effect.asAws)
    id.foreach(id ⇒ statement.setId(id))
    statement.setPrincipals(principals.map(_.asAws).asJavaCollection)
    statement.setActions(actions.map(_.asAws).asJavaCollection)
    statement.setResources(resources.map(_.asAws).asJavaCollection)
    statement.setConditions(conditions.map(_.asAws).asJava)
    statement
  }
}

private[awsutil] object Statement {
  def fromAws(statement: aws.Statement): Statement = {
    def asList[T](jList: JList[T]): List[T] = Option(jList).map(_.asScala.toList).getOrElse(List.empty)
    Statement(
      Option(statement.getId),
      asList(statement.getPrincipals).map(_.asScala),
      statement.getEffect.asScala,
      asList(statement.getActions).map(_.asScala),
      asList(statement.getResources).map(_.asScala),
      asList(statement.getConditions).map(_.asScala))
  }

  /** Enumeration type for statement effects. */
  sealed trait Effect
  object Effect {
    /** Explicitly allow access. */
    case object Allow extends Effect
    /** Explicitly deny access. */
    case object Deny extends Effect

    val values: Seq[Effect] = Seq(Allow,Deny)
  }
}
