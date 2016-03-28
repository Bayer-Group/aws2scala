package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}

import scala.collection.concurrent

abstract class Action extends aws.Action {
  /** Returns an AWS object for this action. */
  def toAws: aws.Action

  final override def getActionName = toAws.getActionName
}

object Action {
  private val registeredActions: concurrent.Map[aws.Action,Action] = concurrent.TrieMap.empty

  private[awsutil] def registerActions(actions: Seq[Action]): Unit = {
    registeredActions ++= actions.map(action ⇒ action.toAws → action)
  }

  def fromAws(action: aws.Action): Action = registeredActions(action)
}
