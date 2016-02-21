package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

/** Describes the current state of the instance.
  *
  * @param name the current state of the instance
  * @param code the low byte represents state.  The high byte is an opaque internal value and should be ignored
  */
case class InstanceState private[ec2](name: InstanceState.Name, code: Int)

object InstanceState {
  private[ec2] def fromAws(state: aws.InstanceState): InstanceState =
    InstanceState(Name.fromString(state.getName).get, state.getCode.toInt)

  sealed abstract class Name(val toAws: aws.InstanceStateName) extends AwsEnumeration[aws.InstanceStateName]

  object Name extends AwsEnumerationCompanion[Name] {
    case object Pending extends Name(aws.InstanceStateName.Pending)
    case object Running extends Name(aws.InstanceStateName.Running)
    case object ShuttingDown extends Name(aws.InstanceStateName.ShuttingDown)
    case object Terminated extends Name(aws.InstanceStateName.Terminated)
    case object Stopping extends Name(aws.InstanceStateName.Stopping)
    case object Stopped extends Name(aws.InstanceStateName.Stopped)

    override def values: Seq[Name] = Seq(Pending, Running, ShuttingDown, Terminated, Stopping, Stopped)
  }
}
