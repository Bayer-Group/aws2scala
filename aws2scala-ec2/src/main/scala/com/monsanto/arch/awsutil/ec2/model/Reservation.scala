package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}

import scala.collection.JavaConverters._

/** Describes a reservation.
  *
  * @param id the ID of the reservation
  * @param owner the ID of the AWS account that owns the reservation
  * @param requester the ID of the requester that launched the instances on your behalf (for example, AWS Management
  *                  Console or Auto Scaling)
  * @param groups the security groups for the reservation (EC2-Classic only)
  * @param instances the instances in the reservation
  */
case class Reservation (id: String,
                        owner: String,
                        requester: Option[String],
                        groups: Seq[GroupIdentifier],
                        instances: Seq[Instance])

object Reservation {
  private[ec2] def fromAws(reservation: aws.Reservation): Reservation =
    Reservation(
      reservation.getReservationId,
      reservation.getOwnerId,
      Option(reservation.getRequesterId),
      reservation.getGroups.asScala.map(GroupIdentifier.fromAws).toList,
      reservation.getInstances.asScala.map(Instance.fromAws).toList)
}
