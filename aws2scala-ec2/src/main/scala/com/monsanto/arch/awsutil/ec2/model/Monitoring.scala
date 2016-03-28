package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

case class Monitoring(state: Monitoring.State)

object Monitoring {
  private[ec2] def fromAws(monitoring: aws.Monitoring): Monitoring =
    Monitoring(State.fromString(monitoring.getState).get)

  sealed abstract class State(val toAws: aws.MonitoringState) extends AwsEnumeration[aws.MonitoringState]

  object State extends AwsEnumerationCompanion[State, aws.MonitoringState] {
    case object Disabled extends State(aws.MonitoringState.Disabled)
    case object Disabling extends State(aws.MonitoringState.Disabling)
    case object Enabled extends State(aws.MonitoringState.Enabled)
    case object Pending extends State(aws.MonitoringState.Pending)

    override def values: Seq[State] = Seq(Disabled, Disabling, Enabled, Pending)
  }
}

