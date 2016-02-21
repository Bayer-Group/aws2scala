package com.monsanto.arch.awsutil.auth.policy

import com.amazonaws.auth.policy.{Action ⇒ AwsAction}

abstract class Action extends AwsAction {
  /** Returns an AWS object for this action. */
  def toAws: AwsAction

  final override def getActionName = toAws.getActionName
}
