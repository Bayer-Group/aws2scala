package com.monsanto.arch.awsutil.ec2.model

import java.util

import com.amazonaws.services.ec2.{model ⇒ aws}

import scala.collection.JavaConverters._

object AwsConverters {
  implicit class AwsGroupIdentifier(val groupIdentifier: GroupIdentifier) extends AnyVal {
    def toAws: aws.GroupIdentifier =
      new aws.GroupIdentifier()
        .withGroupId(groupIdentifier.id)
        .withGroupName(groupIdentifier.name)
  }

  implicit class AwsIamInstanceProfile(val profile: IamInstanceProfile) extends AnyVal {
    def toAws: aws.IamInstanceProfile = new aws.IamInstanceProfile().withId(profile.id).withArn(profile.arn)
  }

  implicit class AwsInstance(val instance: Instance) extends AnyVal {
    def toAws: aws.Instance = {
      val awsInstance = new aws.Instance()
      awsInstance.setInstanceId(instance.id)
      awsInstance.setImageId(instance.imageId)
      awsInstance.setState(instance.state.toAws)
      awsInstance.setPrivateDnsName(instance.privateDnsName)
      awsInstance.setPublicDnsName(instance.publicDnsName.getOrElse(""))
      awsInstance.setStateTransitionReason(instance.stateTransitionReason.getOrElse(""))
      instance.keyName.foreach(awsInstance.setKeyName)
      awsInstance.setAmiLaunchIndex(instance.amiLaunchIndex)
      awsInstance.setProductCodes(instance.productCodes.map(_.toAws).asJavaCollection)
      awsInstance.setInstanceType(instance.instanceType.toAws)
      awsInstance.setLaunchTime(instance.launchTime)
      awsInstance.setPlacement(instance.placement.toAws)
      instance.kernelId.foreach(awsInstance.setKernelId)
      instance.ramdiskId.foreach(awsInstance.setRamdiskId)
      instance.platform.foreach(x ⇒ awsInstance.setPlatform(x.toAws))
      awsInstance.setMonitoring(instance.monitoring.toAws)
      instance.subnetId.foreach(awsInstance.setSubnetId)
      instance.vpcId.foreach(awsInstance.setVpcId)
      awsInstance.setPrivateIpAddress(instance.privateIpAddress)
      instance.publicIpAddress.foreach(awsInstance.setPublicIpAddress)
      instance.stateReason.map(_.toAws).foreach(awsInstance.setStateReason)
      awsInstance.setArchitecture(instance.architecture.toAws)
      awsInstance.setRootDeviceType(instance.rootDeviceType.toAws)
      instance.rootDeviceName.foreach(awsInstance.setRootDeviceName)
      awsInstance.setBlockDeviceMappings(instance.blockDeviceMapping.toAws)
      awsInstance.setVirtualizationType(instance.virtualizationType.toAws)
      instance.lifecycleType.map(_.toAws).foreach(awsInstance.setInstanceLifecycle)
      instance.spotInstanceRequestId.foreach(awsInstance.setSpotInstanceRequestId)
      instance.clientToken.foreach(awsInstance.setClientToken)
      if (instance.tags.nonEmpty) {
        awsInstance.setTags(Tag.fromMap(instance.tags).map(_.toAws).asJavaCollection)
      }
      if (instance.securityGroups.nonEmpty) {
        awsInstance.setSecurityGroups(instance.securityGroups.map(_.toAws).asJavaCollection)
      }
      instance.sourceDestCheck.map(java.lang.Boolean.valueOf).foreach(awsInstance.setSourceDestCheck)
      awsInstance.setHypervisor(instance.hypervisorType.toAws)
      awsInstance.setNetworkInterfaces(instance.networkInterfaces.map(_.toAws).asJavaCollection)
      instance.iamInstanceProfile.map(_.toAws).foreach(awsInstance.setIamInstanceProfile)
      awsInstance.setEbsOptimized(instance.ebsOptimized)
      instance.sriovNetSupport.foreach(awsInstance.setSriovNetSupport)
      awsInstance
    }
  }

  implicit class AwsInstanceBlockDevice(val blockDevice: Instance.BlockDevice) extends AnyVal {
    def toAws: aws.EbsInstanceBlockDevice =
      new aws.EbsInstanceBlockDevice()
        .withVolumeId(blockDevice.volumeId)
        .withStatus(blockDevice.status.toAws)
        .withDeleteOnTermination(blockDevice.deleteOnTermination)
        .withAttachTime(blockDevice.attachTime)
  }

  implicit class AwsInstanceBlockDeviceMappings(val blockDeviceMappings: Map[String,Instance.BlockDevice]) extends AnyVal {
    def toAws: util.Collection[aws.InstanceBlockDeviceMapping] =
      blockDeviceMappings.map { entry ⇒
        new aws.InstanceBlockDeviceMapping()
          .withDeviceName(entry._1)
          .withEbs(entry._2.toAws)
      }.asJavaCollection
  }

  implicit class AwsInstanceNetworkInterface(val networkInterface: Instance.NetworkInterface) extends AnyVal {
    def toAws: aws.InstanceNetworkInterface = {
      val awsNetworkInterface = new aws.InstanceNetworkInterface()
        .withNetworkInterfaceId(networkInterface.id)
        .withSubnetId(networkInterface.subnetId)
        .withVpcId(networkInterface.vpcId)
        .withDescription(networkInterface.description.getOrElse(""))
        .withOwnerId(networkInterface.ownerId)
        .withStatus(networkInterface.status.toAws)
        .withMacAddress(networkInterface.macAddress)
        .withPrivateIpAddress(networkInterface.privateIpAddress)
        .withSourceDestCheck(networkInterface.sourceDestCheck)
        .withGroups(networkInterface.groups.map(_.toAws).asJavaCollection)
        .withAttachment(networkInterface.attachment.toAws)
        .withPrivateIpAddresses(networkInterface.privateIpAddresses.map(_.toAws).asJavaCollection)
      networkInterface.privateDnsName.foreach(awsNetworkInterface.setPrivateDnsName)
      networkInterface.association.map(_.toAws).foreach(awsNetworkInterface.setAssociation)
      awsNetworkInterface
    }
  }

  implicit class AwsInstanceNetworkInterfaceAssociation(val association: Instance.NetworkInterface.Association) extends AnyVal {
    def toAws: aws.InstanceNetworkInterfaceAssociation =
      new aws.InstanceNetworkInterfaceAssociation()
        .withIpOwnerId(association.ipOwner)
        .withPublicDnsName(association.publicDnsName.getOrElse(""))
        .withPublicIp(association.publicIp)
  }

  implicit class AwsInstanceNetworkInterfaceAttachment(val attachment: Instance.NetworkInterface.Attachment) extends AnyVal {
    def toAws: aws.InstanceNetworkInterfaceAttachment =
      new aws.InstanceNetworkInterfaceAttachment()
        .withAttachmentId(attachment.id)
        .withAttachTime(attachment.attachTime)
        .withDeleteOnTermination(attachment.deleteOnTermination)
        .withDeviceIndex(attachment.deviceIndex)
        .withStatus(attachment.status.toAws)
  }

  implicit class AwsInstanceNetworkInterfacePrivateIpAddress(val ipAddress: Instance.NetworkInterface.PrivateIpAddress) extends AnyVal {
    def toAws: aws.InstancePrivateIpAddress = {
      val awsIpAddress = new aws.InstancePrivateIpAddress()
        .withPrivateIpAddress(ipAddress.privateIpAddress)
        .withPrimary(ipAddress.primary)
      ipAddress.privateDnsName.foreach(n ⇒ awsIpAddress.setPrivateDnsName(n))
      ipAddress.association.foreach(a ⇒ awsIpAddress.setAssociation(a.toAws))
      awsIpAddress
    }
  }

  implicit class AwsInstanceState(val state: InstanceState) extends AnyVal {
    def toAws: aws.InstanceState = new aws.InstanceState()
      .withName(state.name.toAws)
      .withCode(Integer.valueOf(state.code))
  }

  implicit class AwsKeyPair(val keyPair: KeyPair) extends AnyVal {
    def toAws: aws.KeyPair =
      new aws.KeyPair()
        .withKeyName(keyPair.name)
        .withKeyFingerprint(keyPair.fingerprint)
        .withKeyMaterial(keyPair.key)
  }

  implicit class AwsKeyPairInfo(val keyPairInfo: KeyPairInfo) extends AnyVal {
    def toAws: aws.KeyPairInfo =
      new aws.KeyPairInfo()
        .withKeyName(keyPairInfo.name)
        .withKeyFingerprint(keyPairInfo.fingerprint)
  }

  implicit class AwsMonitoring(val monitoring: Monitoring) extends AnyVal {
    def toAws: aws.Monitoring = new aws.Monitoring().withState(monitoring.state.toAws)
  }

  implicit class AwsPlacement(val placement: Placement) extends AnyVal {
    def toAws: aws.Placement = {
      val awsPlacement = new aws.Placement()
        .withAvailabilityZone(placement.availabilityZone)
        .withGroupName(placement.groupName.getOrElse(""))
      placement.tenancy.foreach(t ⇒ awsPlacement.setTenancy(t.toAws))
      placement.hostId.foreach(h ⇒ awsPlacement.setHostId(h))
      placement.affinity.foreach(a ⇒ awsPlacement.setAffinity(a.toString))
      awsPlacement
    }
  }

  implicit class AwsProductCode(val productCode: ProductCode) extends AnyVal {
    def toAws: aws.ProductCode =
      new aws.ProductCode()
        .withProductCodeId(productCode.id)
        .withProductCodeType(productCode.`type`.toAws)
  }

  implicit class AwsReservation(val reservation: Reservation) extends AnyVal {
    def toAws: aws.Reservation = {
      val awsReservation = new aws.Reservation()
      awsReservation.setReservationId(reservation.id)
      awsReservation.setOwnerId(reservation.owner)
      reservation.requester.foreach(awsReservation.setRequesterId)
      if (reservation.groups.nonEmpty) {
        awsReservation.setGroups(reservation.groups.map(_.toAws).asJavaCollection)
      }
      awsReservation.setInstances(reservation.instances.map(_.toAws).asJavaCollection)
      awsReservation
    }
  }

  implicit class AwsStateReason(val stateReason: StateReason) extends AnyVal {
    def toAws: aws.StateReason =
      new aws.StateReason()
        .withCode(stateReason.code)
        .withMessage(stateReason.message)
  }
}
