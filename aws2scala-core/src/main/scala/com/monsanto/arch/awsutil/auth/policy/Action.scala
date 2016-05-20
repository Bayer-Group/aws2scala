package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}

import scala.collection.concurrent

trait Action

object Action {
  private[awsutil] val toScalaConversions: concurrent.Map[aws.Action,Action] =
    concurrent.TrieMap(AllActions → AllActions)
  private[awsutil] val toAwsConversions: concurrent.Map[Action,aws.Action] =
    concurrent.TrieMap(AllActions → AllActions)
  private[awsutil] val stringToScalaConversion: concurrent.Map[String,Action] =
    concurrent.TrieMap("*" → AllActions)

  private[awsutil] def registerActions(actions: (aws.Action,Action)*): Unit = {
    toScalaConversions ++= actions
    toAwsConversions ++= actions.map(_.swap)
    stringToScalaConversion ++=
      actions.map(entry ⇒ (entry._1.getActionName, entry._2))
  }

  /** Handy constant for matching all actions. */
  case object AllActions extends Action with aws.Action {
    override def getActionName: String = "*"
  }
}
