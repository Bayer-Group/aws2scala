package com.monsanto.arch.awsutil.testkit

import java.util.Date

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.ec2.model._
import com.monsanto.arch.awsutil.identitymanagement.model.InstanceProfileArn
import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.testkit.AwsScalaCheckImplicits._
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen, Shrink}

object Ec2ScalaCheckImplicits {
  implicit lazy val arbAffinity: Arbitrary[Affinity] = Arbitrary(Gen.oneOf(Affinity.values))

  implicit lazy val arbArchitecture: Arbitrary[Architecture] = Arbitrary(Gen.oneOf(Architecture.values))

  implicit lazy val arbAttachmentStatus: Arbitrary[AttachmentStatus] = Arbitrary(Gen.oneOf(AttachmentStatus.values))

  implicit lazy val arbDescribeInstancesRequest: Arbitrary[DescribeInstancesRequest] =
    Arbitrary {
      for {
        ids ← UtilGen.listOfSqrtN(Ec2Gen.instanceId)
        filters ← Ec2Gen.filterSeq
      } yield  DescribeInstancesRequest(ids, filters)
    }

  implicit lazy val shrinkDescribeInstancesRequest: Shrink[DescribeInstancesRequest] =
    Shrink { request ⇒
      Shrink.shrink(request.instanceIds).filter(_.forall(validId("i"))).map(i ⇒ request.copy(instanceIds = i)) append
        Shrink.shrink(request.filters).map(f ⇒ request.copy(filters = f))
    }

  implicit lazy val arbDescribeKeyPairsRequest: Arbitrary[DescribeKeyPairsRequest] =
    Arbitrary {
      for {
        keyNames ← UtilGen.listOfSqrtN(Ec2Gen.keyName)
        filters ← Ec2Gen.filterSeq
      } yield  DescribeKeyPairsRequest(keyNames, filters)
    }

  implicit lazy val shrinkDescribeKeyPairsRequest: Shrink[DescribeKeyPairsRequest] =
    Shrink { request ⇒
      Shrink.shrink(request.keyNames).filter(_.forall(_.nonEmpty)).map(kn ⇒ request.copy(keyNames = kn)) append
      Shrink.shrink(request.filters).map(f ⇒ request.copy(filters = f))
    }

  implicit lazy val arbFilter: Arbitrary[Filter] = {
    val filterValue = UtilGen.stringOf(UtilGen.extendedWordChar, 1, 64)
    val filterValues = UtilGen.nonEmptyListOfSqrtN(filterValue).retryUntil(x ⇒ x.distinct == x)
    Arbitrary {
      for {
        name ← UtilGen.stringOf(UtilGen.extendedWordChar, 1, 32)
        values ← filterValues
      } yield Filter(name, values)
    }
  }

 implicit lazy val shrinkFilter: Shrink[Filter] =
   Shrink { filter ⇒
     Shrink.shrink(filter.name).filter(_.nonEmpty).map(n ⇒ filter.copy(name = n)) append
     Shrink.shrink(filter.values)
       .filter(x ⇒ x.nonEmpty && x.forall(_.nonEmpty) && (x.distinct == x))
       .map(v ⇒ filter.copy(values = v))
   }

  implicit lazy val arbFilterSeq: Arbitrary[Seq[Filter]] = Arbitrary(Ec2Gen.filterSeq)

  implicit lazy val arbGroupIdentifier: Arbitrary[GroupIdentifier] = {
    val nameChar = Gen.oneOf(('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ "._-:/()#,@[]+=&;{}!$*".toList)
    Arbitrary {
      for  {
        id ← Ec2Gen.groupIdentifierId
        name ← UtilGen.stringOf(nameChar, 1, 255).suchThat(_.nonEmpty)
      } yield GroupIdentifier(id, name)
    }
  }

  implicit lazy val shrinkGroupIdentifier: Shrink[GroupIdentifier] =
    Shrink(groupId ⇒ Shrink.shrink(groupId.name).filter(_.nonEmpty).map(n ⇒ groupId.copy(name = n)))

  implicit lazy val arbHypervisorType: Arbitrary[HypervisorType] = Arbitrary(Gen.oneOf(HypervisorType.values))

  implicit lazy val arbIamInstanceProfile: Arbitrary[IamInstanceProfile] =
    Arbitrary {
      for {
        id ← IamGen.instanceProfileId
        arn ← arbitrary[InstanceProfileArn].map(_.value)
      } yield IamInstanceProfile(id, arn)
    }

  implicit lazy val shrinkIamInstanceProfile: Shrink[IamInstanceProfile] =
    Shrink { profile ⇒
      Shrink.shrink(InstanceProfileArn.fromArn(profile.arn).get).map(arn ⇒ profile.copy(arn = arn.value))
    }

  implicit lazy val arbInstance: Arbitrary[Instance] = {
    val rootDeviceGen = {
      val instanceStore: Gen[(DeviceType, Option[String])] = Gen.const(DeviceType.InstanceStore → None)
      val ebs = Gen.oneOf("/dev/sda1", "/dev/xvda").map(name ⇒ DeviceType.Ebs → Some(name))
      Gen.oneOf(instanceStore, ebs)
    }
    val blockDeviceMappingGen = {
      val deviceName: Gen[String] = {
        val xvdX = Gen.alphaLowerChar.map(c ⇒ s"/dev/xvd$c")
        val xvdXY =
          for {
            x ← Gen.oneOf('b', 'c')
            y ← Gen.alphaLowerChar
          } yield s"/dev/xvd$x$y"
        val sda1 = Gen.const("/dev/sda1")
        val sdX = Gen.alphaLowerChar.map(x ⇒ s"/dev/sd$x")
        val XdYN =
          for {
            x ← Gen.oneOf('s', 'h')
            y ← Gen.alphaLowerChar
            n ← Gen.choose(1,15)
          } yield s"/dev/${x}d$y$n"
        Gen.oneOf(xvdX, xvdXY, sda1, sdX, XdYN)
      }
      UtilGen.Sizer(0, 5).sized(n ⇒ Gen.mapOfN(n, Gen.zip(deviceName, arbitrary[Instance.BlockDevice])))
    }
    val tagMap = {
      val key = UtilGen.stringOf(UtilGen.asciiChar, 1, 127)
      val value = UtilGen.stringOf(UtilGen.asciiChar, 1, 255)
      val entry = Gen.zip(key, value)
      UtilGen.Sizer(0,10).sized(n ⇒ Gen.mapOfN(n, entry))
    }

    Arbitrary {
      for {
        id ← Ec2Gen.instanceId
        imageId ← Ec2Gen.imageId
        state ← arbitrary[InstanceState]
        privateIp ← privateIpAddress
        publicIp ← Gen.option(publicIpAddress)
        stateReason ← arbitrary[Option[StateReason]]
        keyName ← Gen.option(Ec2Gen.keyName)
        amiLaunchIndex ← Gen.posNum[Int]
        productCodes ← arbitrary[Seq[ProductCode]]
        instanceType ← arbitrary[InstanceType]
        launchTime ← arbitrary[Date]
        placement ← arbitrary[Placement]
        kernelId ← Gen.option(Ec2Gen.kernelId)
        ramdiskId ← Gen.option(Ec2Gen.ramdiskId)
        platform ← arbitrary[Option[Platform]]
        monitoring ← arbitrary[Monitoring]
        subnetId ← Gen.option(Ec2Gen.subnetId)
        vpcId ← Gen.option(Ec2Gen.vpcId)
        stateReason ← Gen.option(arbitrary[StateReason])
        architecture ← arbitrary[Architecture]
        rootDevice ← rootDeviceGen
        blockDeviceMapping ← blockDeviceMappingGen
        virtualizationType ← arbitrary[VirtualizationType]
        lifecycleType ← Gen.option(arbitrary[Instance.LifecycleType])
        spotInstanceRequestId ← Gen.option(Ec2Gen.spotInstanceRequestId)
        clientToken ← Gen.option(UtilGen.stringOf(UtilGen.asciiChar, 1, 64))
        tags ← tagMap
        securityGroups ← UtilGen.nonEmptyListOfSqrtN(arbitrary[GroupIdentifier])
        sourceDestCheck ← arbitrary[Option[Boolean]]
        hypervisorType ← arbitrary[HypervisorType]
        networkInterfaces ← UtilGen.listOfSqrtN(arbitrary[Instance.NetworkInterface])
        profile ← arbitrary[Option[IamInstanceProfile]]
        ebsOptimized ← arbitrary[Boolean]
        sriovNetSupport ← Gen.option(Gen.const("simple"))
      } yield Instance(id, imageId, state, privateIp.name, publicIp.map(_.name), stateReason.map(_.message), keyName, amiLaunchIndex,
        productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId,
        privateIp.address, publicIp.map(_.address), stateReason, architecture, rootDevice._1, rootDevice._2,
        blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags,
        securityGroups, sourceDestCheck, hypervisorType, networkInterfaces, profile, ebsOptimized, sriovNetSupport)
    }
  }

  implicit lazy val shrinkInstance: Shrink[Instance] = {
    def validBlockDeviceKeys(m: Map[String,Instance.BlockDevice]): Boolean =
      m.keys.forall(_.matches("^/dev/(xvd[a-z]|xvd[bc][a-z]|sda1|sd[a-z]|[sh]d[a-z]([2-9]|1[0-5]))$"))
    def nonEmptyStringOption(o: Option[String]): Boolean = o.forall(_.nonEmpty)


    Shrink { instance ⇒
      noRecurShrinkOption(instance.publicDnsName).map(x ⇒ instance.copy(publicDnsName = x)) append
        Shrink.shrink(instance.stateReason).map(x ⇒ instance.copy(stateReason = x)) append
        Shrink.shrink(instance.keyName).filter(nonEmptyStringOption).map(x ⇒ instance.copy(keyName = x)) append
        Shrink.shrink(instance.productCodes).map(x ⇒ instance.copy(productCodes = x)) append
        Shrink.shrink(instance.placement).map(x ⇒ instance.copy(placement = x)) append
        noRecurShrinkOption(instance.kernelId).map(x ⇒ instance.copy(kernelId = x)) append
        noRecurShrinkOption(instance.ramdiskId).map(x ⇒ instance.copy(ramdiskId = x)) append
        Shrink.shrink(instance.platform).map(x ⇒ instance.copy(platform = x)) append
        noRecurShrinkOption(instance.subnetId).map(x ⇒ instance.copy(subnetId = x)) append
        noRecurShrinkOption(instance.vpcId).map(x ⇒ instance.copy(vpcId = x)) append
        Shrink.shrink(instance.blockDeviceMapping).filter(validBlockDeviceKeys).map(x ⇒ instance.copy(blockDeviceMapping = x)) append
        noRecurShrinkOption(instance.spotInstanceRequestId).map(x ⇒ instance.copy(spotInstanceRequestId = x)) append
        Shrink.shrink(instance.clientToken).filter(nonEmptyStringOption).map(x ⇒ instance.copy(clientToken = x)) append
        Shrink.shrink(instance.tags).filter(m ⇒ m.keySet.size == m.size).map(x ⇒ instance.copy(tags = x)) append
        Shrink.shrink(instance.securityGroups).filter(_.nonEmpty).map(x ⇒ instance.copy(securityGroups = x)) append
        Shrink.shrink(instance.sourceDestCheck).map(x ⇒ instance.copy(sourceDestCheck = x)) append
        Shrink.shrink(instance.networkInterfaces).map(x ⇒ instance.copy(networkInterfaces = x)) append
        Shrink.shrink(instance.iamInstanceProfile).map(x ⇒ instance.copy(iamInstanceProfile = x)) append
        noRecurShrinkOption(instance.sriovNetSupport).map(x ⇒ instance.copy(sriovNetSupport = x))
    }
  }

  implicit lazy val arbInstanceBlockDevice: Arbitrary[Instance.BlockDevice] =
    Arbitrary {
      for {
        volumeId ← Ec2Gen.volumeId
        blockDevice ← Gen.resultOf(Instance.BlockDevice(volumeId, _: AttachmentStatus, _: Boolean, _: Date))
      } yield blockDevice
    }

  implicit lazy val arbInstanceLifecycleType: Arbitrary[Instance.LifecycleType] =
    Arbitrary(Gen.oneOf(Instance.LifecycleType.values))

  implicit lazy val arbInstanceNetworkInterface: Arbitrary[Instance.NetworkInterface] =
    Arbitrary {
      for {
        id ← Ec2Gen.instanceNetworkInterfaceId
        subnetId ← Ec2Gen.subnetId
        vpcId ← Ec2Gen.vpcId
        description ← Gen.option(UtilGen.stringOf(UtilGen.asciiChar, 1, 255))
        owner ← arbitrary[Account].map(_.id)
        status ← arbitrary[NetworkInterfaceStatus]
        macAddress ← Gen.listOfN(12, UtilGen.lowerHexChar).map(chars ⇒ chars.grouped(2).map(_.mkString).mkString(":"))
        ip ← privateIpAddress
        hostname ← Gen.option(Gen.const(ip.name))
        sourceDestCheck ← arbitrary[Boolean]
        groups ← UtilGen.nonEmptyListOfSqrtN(arbitrary[GroupIdentifier])
        attachment ← arbitrary[Instance.NetworkInterface.Attachment]
        association ← arbitrary[Option[Instance.NetworkInterface.Association]]
        privateIps ← UtilGen.Sizer(0, 5).sized(n ⇒ Gen.listOfN(n, arbitrary[Instance.NetworkInterface.PrivateIpAddress]))
      } yield Instance.NetworkInterface(id, subnetId, vpcId, description, owner, status, macAddress, ip.address,
        hostname, sourceDestCheck, groups, attachment, association, privateIps)
    }

  implicit lazy val shrinkInstanceNetworkInterface: Shrink[Instance.NetworkInterface] =
    Shrink { networkInterface ⇒
      Shrink.shrink(networkInterface.description).filter(_.forall(_.nonEmpty)).map(x ⇒ networkInterface.copy(description = x)) append
      noRecurShrinkOption(networkInterface.privateDnsName).map(x ⇒ networkInterface.copy(privateDnsName = x)) append
      Shrink.shrink(networkInterface.groups).filter(_.nonEmpty).map(x ⇒ networkInterface.copy(groups = x)) append
      Shrink.shrink(networkInterface.association).map(x ⇒ networkInterface.copy(association = x)) append
      Shrink.shrink(networkInterface.privateIpAddresses).map(x ⇒ networkInterface.copy(privateIpAddresses = x))
    }

  implicit lazy val arbInstanceNetworkInterfaceAssociation: Arbitrary[Instance.NetworkInterface.Association] =
    Arbitrary {
      for {
        owner ← Gen.frequency(1 → Gen.const("AWS"), 9 → arbitrary[Account].map(_.id))
        ip ← publicIpAddress
        name ← Gen.option(Gen.const(ip.name))
      } yield Instance.NetworkInterface.Association(owner, name, ip.address)
    }

  implicit lazy val shrinkInstanceNetworkInterfaceAssociation: Shrink[Instance.NetworkInterface.Association] =
    Shrink { association ⇒
      noRecurShrinkOption(association.publicDnsName).map(n ⇒ association.copy(publicDnsName = n))
    }

  implicit lazy val arbInstanceNetworkInterfaceAttachment: Arbitrary[Instance.NetworkInterface.Attachment] =
    Arbitrary {
      for {
        id ← Ec2Gen.instanceNetworkInterfaceAttachmentId
        attachDate ← arbitrary[Date]
        deleteOnTermination ← arbitrary[Boolean]
        deviceIndex ← Gen.posNum[Int]
        status ← arbitrary[AttachmentStatus]
      } yield Instance.NetworkInterface.Attachment(id, attachDate, deleteOnTermination, deviceIndex, status)
    }

  implicit lazy val arbInstanceNetworkInterfacePrivateIpAddress: Arbitrary[Instance.NetworkInterface.PrivateIpAddress] =
    Arbitrary {
      for {
        ip ← privateIpAddress
        name ← Gen.option(Gen.const(ip.name))
        primary ← arbitrary[Boolean]
        association ← arbitrary[Option[Instance.NetworkInterface.Association]]
      } yield Instance.NetworkInterface.PrivateIpAddress(ip.address, name, primary, association)
    }

  implicit lazy val shrinkInstanceNetworkInterfacePrivateIpAddress: Shrink[Instance.NetworkInterface.PrivateIpAddress] =
    Shrink { address ⇒
      noRecurShrinkOption(address.privateDnsName).map(n ⇒ address.copy(privateDnsName = n)) append
      Shrink.shrink(address.association).map(a ⇒ address.copy(association = a))
    }

  implicit lazy val arbInstanceState: Arbitrary[InstanceState] =
    Arbitrary {
      for {
        name ← Gen.oneOf(InstanceState.Name.values)
      } yield {
        val code = {
          name match {
            case InstanceState.Name.Pending      ⇒  0
            case InstanceState.Name.Running      ⇒ 16
            case InstanceState.Name.ShuttingDown ⇒ 32
            case InstanceState.Name.Terminated   ⇒ 48
            case InstanceState.Name.Stopping     ⇒ 64
            case InstanceState.Name.Stopped      ⇒ 80
          }
        }
        InstanceState(name, code)
      }
    }

  implicit lazy val arbInstanceType: Arbitrary[InstanceType] = Arbitrary(Gen.oneOf(InstanceType.values))

  implicit lazy val arbKeyPair: Arbitrary[KeyPair] =
    Arbitrary {
      for {
        keyName ← Ec2Gen.keyName
        fingerprint ← Ec2Gen.keyFingerprint
        material ← Ec2Gen.privateKey
      } yield KeyPair(keyName, fingerprint, material)
    }

  implicit lazy val shrinkKeyPair: Shrink[KeyPair] =
    Shrink { kp ⇒
      Shrink.shrink(kp.name).filter(_.nonEmpty).map(n ⇒ kp.copy(name = n))
    }

  implicit lazy val arbKeyPairInfo: Arbitrary[KeyPairInfo] =
    Arbitrary {
      for {
        keyName ← Ec2Gen.keyName
        fingerprint ← Ec2Gen.keyFingerprint
      } yield KeyPairInfo(keyName, fingerprint)
    }

  implicit lazy val shrinkKeyPairInfo: Shrink[KeyPairInfo] =
    Shrink { kp ⇒
      Shrink.shrink(kp.name).filter(_.nonEmpty).map(n ⇒ kp.copy(name = n))
    }

  implicit lazy val arbMonitoring: Arbitrary[Monitoring] =
    Arbitrary(Gen.resultOf(Monitoring(_: Monitoring.State)))

  implicit lazy val arbMonitoringState: Arbitrary[Monitoring.State] =
    Arbitrary(Gen.oneOf(Monitoring.State.values))

  implicit lazy val arbNetworkInterfaceStatus: Arbitrary[NetworkInterfaceStatus] =
    Arbitrary(Gen.oneOf(NetworkInterfaceStatus.values))

  implicit lazy val arbPlacement: Arbitrary[Placement] =
    Arbitrary {
      val availabilityZoneGen =
        for {
          region ← arbitrary[Region].map(_.name)
          zone ← Gen.oneOf('a' to 'e')
        } yield s"$region$zone"
      val groupNameGen = UtilGen.stringOf(UtilGen.asciiChar, 1, 255)

      for {
        availabilityZone ← availabilityZoneGen
        groupName ← Gen.option(groupNameGen)
        tenancy ← arbitrary[Option[Tenancy]]
        hostId ← Gen.option(Ec2Gen.hostId)
        affinity ← arbitrary[Option[Affinity]]
      } yield Placement(availabilityZone, groupName, tenancy, hostId, affinity)
    }

  implicit lazy val shrinkPlacement: Shrink[Placement] =
    Shrink { placement ⇒
      Shrink.shrink(placement.groupName)
        .filter(_.forall(_.nonEmpty))
        .map(gn ⇒ placement.copy(groupName = gn)) append
      Shrink.shrink(placement.tenancy).map(t ⇒ placement.copy(tenancy = t)) append
      Shrink.shrink(placement.hostId)
        .filter(_.forall(validId("h")))
        .map(h ⇒ placement.copy(hostId = h)) append
      Shrink.shrink(placement.affinity).map(a ⇒ placement.copy(affinity = a))
    }

  implicit lazy val arbPlatform: Arbitrary[Platform] = Arbitrary(Gen.oneOf(Platform.values))

  implicit lazy val arbProductCode: Arbitrary[ProductCode] = {
    val idGen = UtilGen.stringOf(Gen.alphaNumChar, 24, 32)
    Arbitrary {
      for {
        id ← idGen
        aType ← arbitrary[ProductCode.Type]
      } yield ProductCode(id, aType)
    }
  }

  implicit lazy val arbProductCodeType: Arbitrary[ProductCode.Type] = Arbitrary(Gen.oneOf(ProductCode.Type.values))

  implicit lazy val arbReservation: Arbitrary[Reservation] =
    Arbitrary {
      for {
        reservationId ← Ec2Gen.instanceId
        owner ← arbitrary[Account]
        requester ← Gen.option(AwsGen.account(owner.partition))
        securityGroups ← UtilGen.listOfSqrtN(arbitrary[GroupIdentifier])
        instances ← UtilGen.nonEmptyListOfSqrtN(arbitrary[Instance])
      } yield Reservation(reservationId, owner.id, requester.map(_.id), securityGroups, instances)
    }

  implicit lazy val shrinkReservation: Shrink[Reservation] =
    Shrink { reservation ⇒
      noRecurShrinkOption(reservation.requester).map(x ⇒ reservation.copy(requester = x)) append
        Shrink.shrink(reservation.groups).map(x ⇒ reservation.copy(groups = x)) append
        Shrink.shrink(reservation.instances).filter(_.nonEmpty).map(x ⇒ reservation.copy(instances = x))
    }

  implicit lazy val arbStateReason: Arbitrary[StateReason] =
    Arbitrary {
      Gen.oneOf(
        StateReason("Server.SpotInstanceTermination",
          "Server.SpotInstanceTermination: A Spot instance was terminated due to an increase in the market price."),
        StateReason("Server.InternalError",
          "Server.InternalError: An internal error occurred during instance launch, resulting in termination."),
        StateReason("Server.InsufficientInstanceCapacity",
          "Server.InsufficientInstanceCapacity: There was insufficient instance capacity to satisfy the launch request."),
        StateReason("Client.InternalError",
          "Client.InternalError: A client error caused the instance to terminate on launch"),
        StateReason("Client.InstanceInitiatedShutdown",
          "Client.InstanceInitiatedShutdown: The instance was shut down using the shutdown -h command from the instance."),
        StateReason("Client.UserInitiatedShutdown",
          "Client.UserInitiatedShutdown: The instance was shut down using the Amazon EC2 API"),
        StateReason("Client.VolumeLimitExceeded",
          "Client.VolumeLimitExceeded: The limit on the number of EBS volumes or total storage was exceeded. " +
            "Decrease usage or request an increase in your limits."),
        StateReason("Client.InvalidSnapshot.NotFound",
          "Client.InvalidSnapshot.NotFound: The specified snapshot was not found."),
        StateReason("Server.ScheduledStop",
          "Server.ScheduledStop: Stopped due to scheduled retirement")
      )
    }

  implicit lazy val arbTag: Arbitrary[Tag] =
    Arbitrary {
      for {
        key ← UtilGen.stringOf(UtilGen.asciiChar, 1, 127)
        value ← UtilGen.stringOf(UtilGen.asciiChar, 1, 255)
      } yield Tag(key, value)
    }

  implicit lazy val shrinkTag: Shrink[Tag] =
    Shrink { tag ⇒
      Shrink.shrink(tag.key).filter(_.nonEmpty).map(k ⇒ tag.copy(key = k)) append
      Shrink.shrink(tag.value).filter(_.nonEmpty).map(v ⇒ tag.copy(value = v))
    }

  implicit lazy val arbTagSeq: Arbitrary[Seq[Tag]] =
    Arbitrary(UtilGen.listOfSqrtN(arbitrary[Tag]).suchThat(uniqueKeys))

  implicit lazy val shrinkTagSeq: Shrink[Seq[Tag]] =
    Shrink { tags ⇒
      Shrink.shrinkContainer[Seq, Tag].shrink(tags).filter(uniqueKeys)
    }

  implicit lazy val arbTenancy: Arbitrary[Tenancy] = Arbitrary(Gen.oneOf(Tenancy.values))

  implicit lazy val arbVirtualizationType: Arbitrary[VirtualizationType] = Arbitrary(Gen.oneOf(VirtualizationType.values))

  private def uniqueKeys(tags: Seq[Tag]): Boolean = {
    val keys = tags.map(_.key)
    keys.distinct == keys
  }

  private def validId(prefix: String)(id: String): Boolean = id.matches(s"^$prefix-([0-9a-f]{8}|[0-9a-f]{17})")

  private val privateIpAddress: Gen[IpAddress] = ipAddress("ip-", ".ec2.internal")

  private val publicIpAddress: Gen[IpAddress] = ipAddress("ec2-", ".compute-1.amazonaws.com")

  private def ipAddress(prefix: String, suffix: String): Gen[IpAddress] =
    for (octets ← Gen.listOfN(4, Gen.choose(1, 254))) yield {
      val address = octets.mkString(".")
      val name = octets.mkString(prefix, "-", suffix)
      IpAddress(address, name)
    }

  private def noRecurShrinkOption[T](t: Option[T]): Stream[Option[T]] = {
    if (t.isDefined) {
      None #:: Stream.empty
    } else {
      Stream.empty
    }
  }

  private case class IpAddress(address: String, name: String)

//  case class DescribeInstanceRequestArgs(instanceIds: Seq[InstanceId], filters: Seq[FilterArgs]) {
//    def toRequest: DescribeInstancesRequest = DescribeInstancesRequest(instanceIds.map(_.value), filters.map(_.toFilter))
//  }
//  object DescribeInstanceRequestArgs {
//    implicit lazy val arbDescribeInstanceRequestArgs: Arbitrary[DescribeInstanceRequestArgs] =
//      Arbitrary {
//        Gen.sized { size ⇒
//          val namesSizedMax = Math.sqrt(size).toInt
//          val filtersSizedMax = Math.pow(size, 0.2).toInt
//          for {
//            nIds ← Gen.choose(0, namesSizedMax)
//            ids ← Gen.listOfN(nIds, arbitrary[InstanceId])
//            nFilters ← Gen.choose(0, filtersSizedMax)
//            filters ← Gen.listOfN(nFilters, arbitrary[FilterArgs])
//          } yield DescribeInstanceRequestArgs(ids, filters)
//        }
//      }
//
//    implicit lazy val shrinkDescribeInstanceRequestArgs: Shrink[DescribeInstanceRequestArgs] =
//      Shrink.xmap((DescribeInstanceRequestArgs.apply _).tupled, DescribeInstanceRequestArgs.unapply(_).get)
//  }
//
//  case class DescribeKeyPairRequestArgs(keyNames: Seq[KeyName], filters: Seq[FilterArgs]) {
//    def toRequest: DescribeKeyPairsRequest = DescribeKeyPairsRequest(keyNames.map(_.value), filters.map(_.toFilter))
//  }
//  object DescribeKeyPairRequestArgs {
//    implicit lazy val arbDescribeKeyPairRequestArgs: Arbitrary[DescribeKeyPairRequestArgs] =
//      Arbitrary {
//        Gen.sized { size ⇒
//          val namesSizedMax = Math.sqrt(size).toInt
//          val filtersSizedMax = Math.pow(size, 0.2).toInt
//          for {
//            nNames ← Gen.choose(0, namesSizedMax)
//            names ← Gen.listOfN(nNames, arbitrary[KeyName])
//            nFilters ← Gen.choose(0, filtersSizedMax)
//            filters ← Gen.listOfN(nFilters, arbitrary[FilterArgs])
//          } yield DescribeKeyPairRequestArgs(names, filters)
//        }
//      }
//
//    implicit lazy val shrinkDescribeKeyPairRequestArgs: Shrink[DescribeKeyPairRequestArgs] =
//      Shrink.xmap((DescribeKeyPairRequestArgs.apply _).tupled, DescribeKeyPairRequestArgs.unapply(_).get)
//  }
}
