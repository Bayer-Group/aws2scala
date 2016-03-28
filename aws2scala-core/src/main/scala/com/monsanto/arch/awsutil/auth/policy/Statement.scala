package com.monsanto.arch.awsutil.auth.policy

import java.util.{List ⇒ JList}

import com.amazonaws.auth.{policy ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

import scala.collection.JavaConverters._

private[awsutil] case class Statement(id: Option[String],
                                      principals: Seq[Principal],
                                      effect: Statement.Effect,
                                      actions: Seq[Action],
                                      resources: Seq[Resource],
                                      conditions: Seq[Condition]) {
  def toAws: aws.Statement = {
    val statement = new aws.Statement(effect.toAws)
    id.foreach(id ⇒ statement.setId(id))
    statement.setPrincipals(principals.map(_.toAws).asJavaCollection)
    statement.setActions(actions.map(_.toAws).asJavaCollection)
    statement.setResources(resources.map(_.toAws).asJavaCollection)
    statement.setConditions(conditions.map(_.toAws).asJava)
    statement
  }
}

private[awsutil] object Statement {
  def fromAws(statement: aws.Statement): Statement = {
    def asList[T](jList: JList[T]): List[T] = Option(jList).map(_.asScala.toList).getOrElse(List.empty)
    Statement(
      Option(statement.getId),
      asList(statement.getPrincipals).map(Principal.fromAws),
      Effect.fromAws(statement.getEffect),
      asList(statement.getActions).map(Action.fromAws),
      asList(statement.getResources).map(Resource.fromAws),
      asList(statement.getConditions).map(Condition.fromAws))
  }

  sealed abstract class Effect(val toAws: aws.Statement.Effect) extends AwsEnumeration[aws.Statement.Effect]
  object Effect extends AwsEnumerationCompanion[Effect, aws.Statement.Effect] {
    case object Allow extends Effect(aws.Statement.Effect.Allow)
    case object Deny extends Effect(aws.Statement.Effect.Deny)
    override val values: Seq[Effect] = Seq(Allow,Deny)
  }
}
