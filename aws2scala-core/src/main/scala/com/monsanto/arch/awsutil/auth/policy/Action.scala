package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.{policy ⇒ aws}

import scala.collection.concurrent

trait Action

object Action {
  private[awsutil] val toScalaConversions: concurrent.Map[aws.Action,Action] = concurrent.TrieMap.empty
  private[awsutil] val toAwsConversions: concurrent.Map[Action,aws.Action] = concurrent.TrieMap.empty

  private[awsutil] def registerActions(actions: (aws.Action,Action)*): Unit = {
    toScalaConversions ++= actions
    toAwsConversions ++= actions.map(_.swap)
  }
}
