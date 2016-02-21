package com.monsanto.arch.awsutil.ec2.model

import java.util.Date

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

import scala.collection.JavaConverters._

/** Describes an instance.
  *
  * @param id the ID of the instance
  * @param imageId the ID of the AMI used to launch the instance
  * @param state the current state of the instance
  * @param privateDnsName the private DNS name assigned to the instance
  * @param publicDnsName the public DNS name assigned to the instance, if any
  * @param stateTransitionReason the reason for the most recent state transition as a string
  * @param keyName the name of the key pair, if this instance was launched with an associated key pair
  * @param amiLaunchIndex the AMI launch index, which can be used to find this instance in the launch group
  * @param productCodes the product codes attached to this instance, if applicable
  * @param instanceType the instance type
  * @param launchTime the time the instance was launched
  * @param placement the placement information for the instance
  * @param kernelId the kernel associated with the instance, if any
  * @param ramdiskId the RAM disk associated with this instance, if any
  * @param platform the value is `Windows` for Windows instances, otherwise empty
  * @param monitoring the monitoring information for the instance
  * @param subnetId the ID of the subnet in which the instance is running (EC2-VPC)
  * @param vpcId the ID of the VPC in which the instance is running (EC2-VPC)
  * @param privateIpAddress the private IP address assigned to the instance
  * @param publicIpAddress the public IP address assigned to the instance, if applicable
  * @param stateReason the reason for the most recent state transition
  * @param architecture the architecture of the image
  * @param rootDeviceType the root device type used by the AMI
  * @param rootDeviceName the root device name, if it is an EBS device
  * @param blockDeviceMapping any block device mappings for the instance
  * @param virtualizationType the virtualization type of the instance
  * @param lifecycleType the instance lifecycle type, if any
  * @param spotInstanceRequestId if this is a spot instance, the ID of the spot instance request
  * @param clientToken the idempotency token provided when the instance was launched, if any
  * @param tags any tags assigned to the instance
  * @param securityGroups  one or more security groups for the instance
  * @param sourceDestCheck specifies whether to enable an instance launched in a VPC to perform NAT (needs to be false
  *                        to perform NAT)
  * @param hypervisorType the hypervisor type of the image
  * @param networkInterfaces for EC2-VPC instances, one or more network interfaces for the instance
  * @param iamInstanceProfile the IAM instance profile associated with the instance, if any
  * @param ebsOptimized indicates whether the instance is optimized for EBS I/O
  * @param sriovNetSupport specifies whether enhanced networking is enabled (appears broken)
  */
case class Instance private[ec2] (id: String,
                                  imageId: String,
                                  state: InstanceState,
                                  privateDnsName: String,
                                  publicDnsName: Option[String],
                                  stateTransitionReason: Option[String],
                                  keyName: Option[String],
                                  amiLaunchIndex: Int,
                                  productCodes: Seq[ProductCode],
                                  instanceType: InstanceType,
                                  launchTime: Date,
                                  placement: Placement,
                                  kernelId: Option[String],
                                  ramdiskId: Option[String],
                                  platform: Option[Platform],
                                  monitoring: Monitoring,
                                  subnetId: Option[String],
                                  vpcId: Option[String],
                                  privateIpAddress: String,
                                  publicIpAddress: Option[String],
                                  stateReason: Option[StateReason],
                                  architecture: Architecture,
                                  rootDeviceType: DeviceType,
                                  rootDeviceName: Option[String],
                                  blockDeviceMapping: Map[String,Instance.BlockDevice],
                                  virtualizationType: VirtualizationType,
                                  lifecycleType: Option[Instance.LifecycleType],
                                  spotInstanceRequestId: Option[String],
                                  clientToken: Option[String],
                                  tags: Map[String,String],
                                  securityGroups: Seq[GroupIdentifier],
                                  sourceDestCheck: Option[Boolean],
                                  hypervisorType: HypervisorType,
                                  networkInterfaces: Seq[Instance.NetworkInterface],
                                  iamInstanceProfile: Option[IamInstanceProfile],
                                  ebsOptimized: Boolean,
                                  sriovNetSupport: Option[String])

object Instance {
  private[ec2] def fromAws(instance: aws.Instance): Instance =
    Instance(
      instance.getInstanceId,
      instance.getImageId,
      InstanceState.fromAws(instance.getState),
      instance.getPrivateDnsName,
      Option(instance.getPublicDnsName).filter(_.nonEmpty),
      Option(instance.getStateTransitionReason).filter(_.nonEmpty),
      Option(instance.getKeyName),
      instance.getAmiLaunchIndex.toInt,
      instance.getProductCodes.asScala.map(ProductCode.fromAws).toList,
      InstanceType.fromString(instance.getInstanceType).get,
      instance.getLaunchTime,
      Placement.fromAws(instance.getPlacement),
      Option(instance.getKernelId),
      Option(instance.getRamdiskId),
      Option(instance.getPlatform).map(p ⇒ Platform.fromString(p).get),
      Monitoring.fromAws(instance.getMonitoring),
      Option(instance.getSubnetId),
      Option(instance.getVpcId),
      instance.getPrivateIpAddress,
      Option(instance.getPublicIpAddress),
      Option(instance.getStateReason).map(StateReason.fromAws),
      Architecture.fromString(instance.getArchitecture).get,
      DeviceType.fromString(instance.getRootDeviceType).get,
      Option(instance.getRootDeviceName),
      instance.getBlockDeviceMappings.asScala.map(m ⇒ m.getDeviceName → BlockDevice.fromAws(m.getEbs)).toMap,
      VirtualizationType.fromString(instance.getVirtualizationType).get,
      Option(instance.getInstanceLifecycle).flatMap(t ⇒ LifecycleType.fromString(t)),
      Option(instance.getSpotInstanceRequestId),
      Option(instance.getClientToken).filter(_.nonEmpty),
      {
        val tags = Option(instance.getTags).map(_.asScala.map(Tag.fromAws)).getOrElse(Seq.empty)
        Tag.toMap(tags)
      },
      Option(instance.getSecurityGroups.asScala.toList).getOrElse(List.empty).map(GroupIdentifier.fromAws),
      Option(instance.getSourceDestCheck).map(_.booleanValue()),
      HypervisorType.fromString(instance.getHypervisor).get,
      instance.getNetworkInterfaces.asScala.map(NetworkInterface.fromAws).toList,
      Option(instance.getIamInstanceProfile).map(p ⇒ IamInstanceProfile.fromAws(p)),
      instance.getEbsOptimized.booleanValue(),
      Option(instance.getSriovNetSupport))

  /** Describes an EBS volume in a block device mapping.
    *
    * @param volumeId the ID of the EBS volume
    * @param status the attachment state
    * @param deleteOnTermination whether the volume is deleted on instance termination
    * @param attachTime the time stamp when the attachment initiated
    */
  case class BlockDevice private[ec2](volumeId: String,
                                      status: AttachmentStatus,
                                      deleteOnTermination: Boolean,
                                      attachTime: Date)

  object BlockDevice {
    private[ec2] def fromAws(bd: aws.EbsInstanceBlockDevice): BlockDevice =
      BlockDevice(
        bd.getVolumeId,
        AttachmentStatus.fromString(bd.getStatus).get,
        bd.getDeleteOnTermination.booleanValue(),
        bd.getAttachTime)
  }

  sealed abstract class LifecycleType(val toAws: aws.InstanceLifecycleType) extends AwsEnumeration[aws.InstanceLifecycleType]
  object LifecycleType extends AwsEnumerationCompanion[LifecycleType] {
    /** Denotes that this is a spot instance. */
    case object Spot extends LifecycleType(aws.InstanceLifecycleType.Spot)

    override def values: Seq[LifecycleType] = Seq(Spot)
  }

  /** Describes a network interface.
    *
    * @param id the ID of th network interface
    * @param subnetId the ID of the subnet
    * @param vpcId the ID of the VPC
    * @param description an optional description for the interface
    * @param ownerId the ID of the AWS account that created the network interface
    * @param status the status of the network interface
    * @param macAddress the MAC address of the interface
    * @param privateIpAddress the IP address of the network interface within the subnet
    * @param privateDnsName the private DNS name, if any
    * @param sourceDestCheck indicates whether to validate network traffic to or from this network interface
    * @param groups one or more security groups assigned to the interface
    * @param attachment the network interface attachment
    * @param association the association information for an Elastic IP associated with the network interface
    * @param privateIpAddresses the private IP addresses associated with the network interface
    */
  case class NetworkInterface private[ec2] (id: String,
                                            subnetId: String,
                                            vpcId: String,
                                            description: Option[String],
                                            ownerId: String,
                                            status: NetworkInterfaceStatus,
                                            macAddress: String,
                                            privateIpAddress: String,
                                            privateDnsName: Option[String],
                                            sourceDestCheck: Boolean,
                                            groups: Seq[GroupIdentifier],
                                            attachment: NetworkInterface.Attachment,
                                            association: Option[NetworkInterface.Association],
                                            privateIpAddresses: Seq[NetworkInterface.PrivateIpAddress])
  object NetworkInterface {
    private[ec2] def fromAws(interface: aws.InstanceNetworkInterface) =
      NetworkInterface(
        interface.getNetworkInterfaceId,
        interface.getSubnetId,
        interface.getVpcId,
        Option(interface.getDescription).filter(_.nonEmpty),
        interface.getOwnerId,
        NetworkInterfaceStatus.fromString(interface.getStatus).get,
        interface.getMacAddress,
        interface.getPrivateIpAddress,
        Option(interface.getPrivateDnsName),
        interface.getSourceDestCheck.booleanValue(),
        interface.getGroups.asScala.map(GroupIdentifier.fromAws).toList,
        Attachment.fromAws(interface.getAttachment),
        Option(interface.getAssociation).map(Association.fromAws),
        interface.getPrivateIpAddresses.asScala.map(PrivateIpAddress.fromAws).toList)

    /** Describes association information for an elastic IP address.
      *
      * @param ipOwner the ID of the owner of the Elastic IP address
      * @param publicDnsName the public DNS name
      * @param publicIp the public IP address or Elastic IP address bound to the network interface
      */
    case class Association private[ec2](ipOwner: String,
                                        publicDnsName: Option[String],
                                        publicIp: String)
    object Association {
      private[ec2] def fromAws(association: aws.InstanceNetworkInterfaceAssociation): Association =
        Association(
          association.getIpOwnerId,
          Option(association.getPublicDnsName).filter(_.nonEmpty),
          association.getPublicIp)
    }

    /** Describes a network interface attachment.
      *
      * @param id the ID of the network interface attachment
      * @param attachTime the time stamp when the attachment initiated
      * @param deleteOnTermination indicates whether the network interface is deleted when the instance is terminated
      * @param deviceIndex the index of the device on the instance for the network interface attachment
      * @param status the attachment state
      */
    case class Attachment private[ec2] (id: String,
                                        attachTime: Date,
                                        deleteOnTermination: Boolean,
                                        deviceIndex: Int,
                                        status: AttachmentStatus)
    object Attachment {
      private[ec2] def fromAws(attachment: aws.InstanceNetworkInterfaceAttachment): Attachment =
        Attachment(
          attachment.getAttachmentId,
          attachment.getAttachTime,
          attachment.getDeleteOnTermination.booleanValue(),
          attachment.getDeviceIndex.toInt,
          AttachmentStatus.fromString(attachment.getStatus).get)
    }

    /** Describes a private IP address.
      *
      * @param privateIpAddress the private IP address of the network interface
      * @param privateDnsName the private DNS name, if any
      * @param primary whether this IP address is the primary private IP address for the network interface
      * @param association the association information for an Elastic IP address for the network interface
      */
    case class PrivateIpAddress private[ec2] (privateIpAddress: String,
                                              privateDnsName: Option[String],
                                              primary: Boolean,
                                              association: Option[Association])
    object PrivateIpAddress {
      private[ec2] def fromAws(address: aws.InstancePrivateIpAddress): PrivateIpAddress =
        PrivateIpAddress(
          address.getPrivateIpAddress,
          Option(address.getPrivateDnsName).filter(_.nonEmpty),
          address.isPrimary.booleanValue(),
          Option(address.getAssociation).map(Association.fromAws))
    }
  }
}
