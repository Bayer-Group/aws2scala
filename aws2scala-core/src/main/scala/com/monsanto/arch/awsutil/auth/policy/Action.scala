package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}

import scala.collection.concurrent

/** Identifies specific actions that can be performed on an AWS resource.
  *
  * @param name the unique name of the action
  */
abstract class Action(val name: String)

object Action {
  private[awsutil] val toScalaConversions: concurrent.Map[aws.Action,Action] =
    concurrent.TrieMap(AllActions → AllActions)
  private[awsutil] val toAwsConversions: concurrent.Map[Action,aws.Action] =
    concurrent.TrieMap(AllActions → AllActions)
  private[awsutil] val nameToScalaConversion: concurrent.Map[String,Action] =
    concurrent.TrieMap("*" → AllActions)

  private[awsutil] def registerActions(actions: (aws.Action,Action)*): Unit = {
    toScalaConversions ++= actions
    toAwsConversions ++= actions.map(_.swap)
    nameToScalaConversion ++=
      actions.map(entry ⇒ (entry._1.getActionName, entry._2))
  }

  /** Handy constant for matching all actions. */
  case object AllActions extends Action("*") with aws.Action {
    override def getActionName = name
  }

  /** Extracts the name from the action. */
  def unapply(action: Action): Option[String] = Some(action.name)

  /** Extracts an `Action` given a name. */
  object fromName {
    def unapply(name: String): Option[Action] =  nameToScalaConversion.get(name)
  }
}
