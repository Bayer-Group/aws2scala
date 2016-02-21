package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}

/** Describes the placement for an instance.
  *
  * @param availabilityZone the Availability Zone of the instance
  * @param groupName the name of the placement group the instance is in, if any
  * @param tenancy the tenancy of the instance (if the instance is running in a VPC)
  * @param hostId the ID of the Dedicated host on which the instance resides, if any
  * @param affinity the affinity setting for the instance on the Dedicated host
  */
case class Placement private[ec2] (availabilityZone: String,
                                   groupName: Option[String],
                                   tenancy: Option[Tenancy],
                                   hostId: Option[String],
                                   affinity: Option[Affinity])

object Placement {
  private[ec2] def fromAws(placement: aws.Placement): Placement =
    Placement(
      placement.getAvailabilityZone,
      Option(placement.getGroupName).filter(_.nonEmpty),
      Option(placement.getTenancy).map(t ⇒ Tenancy.fromString(t).get),
      Option(placement.getHostId),
      Option(placement.getAffinity).map(t ⇒ Affinity.fromString(t).get))
}
