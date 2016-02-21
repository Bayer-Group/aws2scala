package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}

/** Describes a state change.
  *
  * @param code the reason code for the state change
  * @param message the message for the state change
  */
case class StateReason private[ec2] (code: String, message: String)

object StateReason {
  private[ec2] def fromAws(reason: aws.StateReason): StateReason =
    StateReason(reason.getCode, reason.getMessage)
}
