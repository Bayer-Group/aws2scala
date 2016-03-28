package com.monsanto.arch.awsutil.kms.model

import com.amazonaws.services.kms.{model ⇒ aws}

sealed trait KeyState extends AnyRef {
  def toAws: aws.KeyState

  override def toString = toAws.toString
}

object KeyState {
  case object Enabled extends KeyState {
    override val toAws = aws.KeyState.Enabled
  }

  case object Disabled extends KeyState {
    override val toAws = aws.KeyState.Disabled
  }

  case object PendingDeletion extends KeyState {
    override val toAws = aws.KeyState.PendingDeletion
  }

  def apply(str: String): KeyState = apply(aws.KeyState.fromValue(str))

  def apply(awsKeyState: aws.KeyState): KeyState = {
    awsKeyState match {
      case aws.KeyState.Disabled ⇒ Disabled
      case aws.KeyState.Enabled ⇒ Enabled
      case aws.KeyState.PendingDeletion ⇒ PendingDeletion
    }
  }
}
