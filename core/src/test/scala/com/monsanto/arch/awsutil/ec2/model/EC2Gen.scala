package com.monsanto.arch.awsutil.ec2.model

import java.util
import java.util.Date

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.AwsGen
import com.monsanto.arch.awsutil.ec2.model.Instance.LifecycleType
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Shrink.shrink
import org.scalacheck.{Arbitrary, Gen, Shrink}

import scala.collection.JavaConverters._

object EC2Gen {
  case class DescribeInstanceRequestArgs(instanceIds: Seq[InstanceId], filters: Seq[FilterArgs]) {
    def toRequest: DescribeInstancesRequest = DescribeInstancesRequest(instanceIds.map(_.value), filters.map(_.toFilter))
  }
  object DescribeInstanceRequestArgs {
    implicit lazy val arbDescribeInstanceRequestArgs: Arbitrary[DescribeInstanceRequestArgs] =
      Arbitrary {
        Gen.sized { size ⇒
          val namesSizedMax = Math.sqrt(size).toInt
          val filtersSizedMax = Math.pow(size, 0.2).toInt
          for {
            nIds ← Gen.choose(0, namesSizedMax)
            ids ← Gen.listOfN(nIds, arbitrary[InstanceId])
            nFilters ← Gen.choose(0, filtersSizedMax)
            filters ← Gen.listOfN(nFilters, arbitrary[FilterArgs])
          } yield DescribeInstanceRequestArgs(ids, filters)
        }
      }

    implicit lazy val shrinkDescribeInstanceRequestArgs: Shrink[DescribeInstanceRequestArgs] =
      Shrink.xmap((DescribeInstanceRequestArgs.apply _).tupled, DescribeInstanceRequestArgs.unapply(_).get)
  }

  case class DescribeKeyPairRequestArgs(keyNames: Seq[KeyName], filters: Seq[FilterArgs]) {
    def toRequest: DescribeKeyPairsRequest = DescribeKeyPairsRequest(keyNames.map(_.value), filters.map(_.toFilter))
  }
  object DescribeKeyPairRequestArgs {
    implicit lazy val arbDescribeKeyPairRequestArgs: Arbitrary[DescribeKeyPairRequestArgs] =
      Arbitrary {
        Gen.sized { size ⇒
          val namesSizedMax = Math.sqrt(size).toInt
          val filtersSizedMax = Math.pow(size, 0.2).toInt
          for {
            nNames ← Gen.choose(0, namesSizedMax)
            names ← Gen.listOfN(nNames, arbitrary[KeyName])
            nFilters ← Gen.choose(0, filtersSizedMax)
            filters ← Gen.listOfN(nFilters, arbitrary[FilterArgs])
          } yield DescribeKeyPairRequestArgs(names, filters)
        }
      }

    implicit lazy val shrinkDescribeKeyPairRequestArgs: Shrink[DescribeKeyPairRequestArgs] =
      Shrink.xmap((DescribeKeyPairRequestArgs.apply _).tupled, DescribeKeyPairRequestArgs.unapply(_).get)
  }

  case class FilterArgs(name: FilterArgs.Name, values: FilterArgs.Values) {
    def toFilter: Filter = Filter(name.value, values.value.map(_.value))
  }
  object FilterArgs {
    lazy implicit val arbFilterArgs: Arbitrary[FilterArgs] = Arbitrary(Gen.resultOf(FilterArgs.apply _))

    lazy implicit val shrinkFilterArgs: Shrink[FilterArgs] =
      Shrink.xmap((FilterArgs.apply _).tupled, FilterArgs.unapply(_).get)

    case class Name(value: String)
    object Name {
      lazy implicit val arbName: Arbitrary[Name] =
        Arbitrary(AwsGen.stringOf(AwsGen.extendedWordChar, 1, 32).map(Name.apply))
      lazy implicit val shrinkName: Shrink[Name] =
        Shrink(v ⇒ shrink(v.value).filter(_.nonEmpty).map(Name.apply))
    }

    case class Values(value: Seq[Value])
    object Values {
      lazy implicit val arbValues: Arbitrary[Values] =
        Arbitrary {
          Gen.sized { size ⇒
            val sizedMax = Math.sqrt(size).toInt
            for {
              n ← Gen.choose(1, sizedMax)
              values ← Gen.listOfN(n, arbitrary[Value])
            } yield Values(values)
          }
        }

      lazy implicit val shrinkValues: Shrink[Values] =
        Shrink { values ⇒
          shrink(values.value).filter(_.nonEmpty).map(Values.apply)
        }
    }

    case class Value(value: String)
    object Value {
      lazy implicit val arbValue: Arbitrary[Value] =
        Arbitrary(AwsGen.stringOf(AwsGen.extendedWordChar, 1, 64).map(Value.apply))
      lazy implicit val shrinkValue: Shrink[Value] =
        Shrink(v ⇒ shrink(v.value).filter(_.nonEmpty).map(Value.apply))
    }
  }

  case class Fingerprint(value: String)
  object Fingerprint {
    lazy implicit val arbFingerprint: Arbitrary[Fingerprint] =
      Arbitrary(Gen.listOfN(20, hexChar)
        .map { chars ⇒
          val fp = chars.grouped(2).map(_.mkString).mkString(":")
          Fingerprint(fp)
        })
  }

  case class GroupIdentifierArgs(id: GroupIdentifierArgs.GroupId, name: GroupIdentifierArgs.GroupName) {
    def toGroupIdentifier = GroupIdentifier(id.value, name.value)
    def toAws: aws.GroupIdentifier =
      new aws.GroupIdentifier().withGroupId(id.value).withGroupName(name.value)
  }
  object GroupIdentifierArgs {
    implicit lazy val arbGroupIdentifierArgs: Arbitrary[GroupIdentifierArgs] =
      Arbitrary(Gen.resultOf(GroupIdentifierArgs.apply _))

    implicit lazy val shrinkGroupIdentifierArgs: Shrink[GroupIdentifierArgs] =
      Shrink.xmap((GroupIdentifierArgs. apply _).tupled, GroupIdentifierArgs.unapply(_).get)

    case class GroupId(value: String)
    object GroupId {
      implicit lazy val arbGroupId: Arbitrary[GroupId] = Arbitrary(genShortId("sg").map(GroupId.apply))
    }

    case class GroupName(value: String)
    object GroupName {
      val NameChars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ "._-:/()#,@[]+=&;{}!$*".toList

      implicit lazy val arbGroupName: Arbitrary[GroupName] =
        Arbitrary(AwsGen.stringOf(Gen.oneOf(NameChars), 1, 255).map(GroupName.apply))

      implicit lazy val shrinkGroupName: Shrink[GroupName] =
        Shrink { name ⇒
          shrink(name.value).filter(_.nonEmpty).map(GroupName.apply)
        }
    }
  }

  case class IamInstanceProfileArgs(id: AwsGen.IAM.Id, arn: IamInstanceProfileArgs.InstanceProfileArn){
    def toIamInstanceProfile = IamInstanceProfile(id.value, arn.value)
    def toAws = new aws.IamInstanceProfile().withId(id.value).withArn(arn.value)
  }
  object IamInstanceProfileArgs {
    implicit lazy val arbIamInstanceProfileArg: Arbitrary[IamInstanceProfileArgs] =
      Arbitrary(Gen.resultOf(IamInstanceProfileArgs.apply _))
    implicit lazy val shrinkIamInstanceProfileArg: Shrink[IamInstanceProfileArgs] =
      Shrink.xmap((IamInstanceProfileArgs.apply _).tupled, IamInstanceProfileArgs.unapply(_).get)

    case class InstanceProfileArn(account: AwsGen.Account, name: AwsGen.IAM.Name)
      extends AwsGen.Arn(s"arn:aws:iam::$account:instance-profile/$name")
    object InstanceProfileArn {
      implicit lazy val arbInstanceProfileArn: Arbitrary[InstanceProfileArn] =
        Arbitrary(Gen.resultOf(InstanceProfileArn.apply _))
      implicit lazy val shrinkInstanceProfileArn: Shrink[InstanceProfileArn] =
        Shrink.xmap((InstanceProfileArn.apply _).tupled, InstanceProfileArn.unapply(_).get)
    }
  }

  case class Index(value: Int)
  object Index {
    implicit lazy val arbIndex: Arbitrary[Index] = Arbitrary(Gen.posNum[Int].map(Index.apply))
    implicit lazy val shrinkIndex: Shrink[Index] =
      Shrink(index ⇒ shrink(index.value).filter(_ >= 0).map(Index.apply))
  }

  case class InstanceArgs(id: InstanceId,
                          imageId: String,
                          state: InstanceStateArgs,
                          privateIp: InstanceArgs.PrivateIp,
                          publicIp: Option[InstanceArgs.PublicIp],
                          stateReason: Option[StateReasonArgs],
                          keyName: Option[KeyName],
                          amiLaunchIndex: Index,
                          productCodes: Seq[ProductCodeArgs],
                          instanceType: InstanceType,
                          launchTime: Date,
                          placement: PlacementArgs,
                          kernelId: Option[InstanceArgs.KernelId],
                          ramdiskId: Option[InstanceArgs.RamdiskId],
                          platform: Option[Platform],
                          monitoring: MonitoringArgs,
                          subnetId: Option[InstanceArgs.SubnetId],
                          vpcId: Option[InstanceArgs.VpcId],
                          architecture: Architecture,
                          rootDevice: InstanceArgs.RootDevice,
                          blockDeviceMapping: InstanceArgs.BlockDeviceMappingArgs,
                          virtualizationType: VirtualizationType,
                          lifecycleType: Option[Instance.LifecycleType],
                          spotInstanceRequestId: Option[InstanceArgs.SpotInstanceRequestId],
                          clientToken: Option[InstanceArgs.ClientToken],
                          tags: InstanceArgs.Tags,
                          securityGroups: InstanceArgs.SecurityGroups,
                          sourceDestCheck: Option[Boolean],
                          hypervisor: HypervisorType,
                          networkInterfaces: Seq[InstanceArgs.NetworkInterfaceArgs],
                          iamInstanceProfile: Option[IamInstanceProfileArgs],
                          ebsOptimized: Boolean,
                          sriovNetSupport: Option[InstanceArgs.SriovNetSupport]) {
    def toInstance: Instance =
      Instance(
        id.value,
        imageId,
        state.toInstanceState,
        privateIp.name,
        publicIp.map(_.name),
        stateReason.map(_.message),
        keyName.map(_.value),
        amiLaunchIndex.value,
        productCodes.map(_.toProductCode),
        instanceType,
        launchTime,
        placement.toPlacement,
        kernelId.map(_.value),
        ramdiskId.map(_.value),
        platform,
        monitoring.toMonitoring,
        subnetId.map(_.value),
        vpcId.map(_.value),
        privateIp.address,
        publicIp.map(_.address),
        stateReason.map(_.toStateReason),
        architecture,
        rootDevice.deviceType,
        rootDevice.name,
        blockDeviceMapping.toMap,
        virtualizationType,
        lifecycleType,
        spotInstanceRequestId.map(_.value),
        clientToken.map(_.value),
        tags.toMap,
        securityGroups.toSecurityGroups,
        sourceDestCheck,
        hypervisor,
        networkInterfaces.map(_.toNetworkInterface),
        iamInstanceProfile.map(_.toIamInstanceProfile),
        ebsOptimized,
        sriovNetSupport.map(_.value))

    def toAws: aws.Instance = {
      val instance = new aws.Instance()
      instance.setInstanceId(id.value)
      instance.setImageId(imageId)
      instance.setState(state.toAws)
      instance.setPrivateDnsName(privateIp.name)
      instance.setPublicDnsName(publicIp.map(_.name).getOrElse(""))
      instance.setStateTransitionReason(stateReason.map(_.message).getOrElse(""))
      keyName.foreach(k ⇒ instance.setKeyName(k.value))
      instance.setAmiLaunchIndex(amiLaunchIndex.value)
      instance.setProductCodes(productCodes.map(_.toAws).asJavaCollection)
      instance.setInstanceType(instanceType.toAws)
      instance.setLaunchTime(launchTime)
      instance.setPlacement(placement.toAws)
      kernelId.foreach(k ⇒ instance.setKernelId(k.value))
      ramdiskId.foreach(r ⇒ instance.setRamdiskId(r.value))
      platform.foreach(p ⇒ instance.setPlatform(p.toAws))
      instance.setMonitoring(monitoring.toAws)
      subnetId.foreach(s ⇒ instance.setSubnetId(s.value))
      vpcId.foreach(v ⇒ instance.setVpcId(v.value))
      instance.setPrivateIpAddress(privateIp.address)
      publicIp.foreach(ip ⇒ instance.setPublicIpAddress(ip.address))
      stateReason.foreach(r ⇒ instance.setStateReason(r.toAws))
      instance.setArchitecture(architecture.toAws)
      instance.setRootDeviceType(rootDevice.deviceType.toAws)
      rootDevice.name.foreach(n ⇒ instance.setRootDeviceName(n))
      instance.setBlockDeviceMappings(blockDeviceMapping.toAws)
      instance.setVirtualizationType(virtualizationType.toAws)
      lifecycleType.foreach(t ⇒ instance.setInstanceLifecycle(t.toAws))
      spotInstanceRequestId.foreach(id ⇒ instance.setSpotInstanceRequestId(id.value))
      instance.setClientToken(clientToken.map(_.value).getOrElse(""))
      if (tags.values.nonEmpty) instance.setTags(tags.toAws)
      if (securityGroups.values.nonEmpty) instance.setSecurityGroups(securityGroups.toAws)
      sourceDestCheck.foreach(c ⇒ instance.setSourceDestCheck(java.lang.Boolean.valueOf(c)))
      instance.setHypervisor(hypervisor.toAws)
      instance.setNetworkInterfaces(networkInterfaces.map(_.toAws).asJavaCollection)
      iamInstanceProfile.foreach(p ⇒ instance.setIamInstanceProfile(p.toAws))
      instance.setEbsOptimized(ebsOptimized)
      sriovNetSupport.foreach(s ⇒ instance.setSriovNetSupport(s.value))
      instance
    }
  }
  object InstanceArgs {
    implicit lazy val arbArchitecture: Arbitrary[Architecture] = Arbitrary(Gen.oneOf(Architecture.values))

    implicit lazy val arbHypervisorType: Arbitrary[HypervisorType] = Arbitrary(Gen.oneOf(HypervisorType.values))

    implicit lazy val arbInstanceLifecycleType: Arbitrary[Instance.LifecycleType] =
      Arbitrary(Gen.oneOf(Instance.LifecycleType.values))

    implicit lazy val arbInstanceType: Arbitrary[InstanceType] = Arbitrary(Gen.oneOf(InstanceType.values))

    implicit lazy val arbInstanceArgs: Arbitrary[InstanceArgs] =
      Arbitrary {
        for {
          id ← arbitrary[InstanceId]
          imageId ← genShortId("ami")
          state ← arbitrary[InstanceStateArgs]
          privateIp ← arbitrary[PrivateIp]
          publicIp ← arbitrary[Option[PublicIp]]
          stateReason ← arbitrary[Option[StateReasonArgs]]
          keyName ← arbitrary[Option[KeyName]]
          amiLaunchIndex ← arbitrary[Index]
          productCodes ← arbitrary[Seq[ProductCodeArgs]]
          instanceType ← arbitrary[InstanceType]
          launchTime ← arbitrary[Date]
          placement ← arbitrary[PlacementArgs]
          kernelId ← arbitrary[Option[KernelId]]
          ramdiskId ← arbitrary[Option[RamdiskId]]
          platform ← arbitrary[Option[Platform]]
          subnetId ← arbitrary[Option[SubnetId]]
          vpcId ← arbitrary[Option[VpcId]]
          monitoring ← arbitrary[MonitoringArgs]
          architecture ← arbitrary[Architecture]
          rootDevice ← arbitrary[RootDevice]
          blockDeviceMapping ← arbitrary[BlockDeviceMappingArgs]
          lifecycleType ← arbitrary[Option[LifecycleType]]
          virtualizationType ← arbitrary[VirtualizationType]
          spotInstanceRequestId ← arbitrary[Option[SpotInstanceRequestId]]
          clientToken ← arbitrary[Option[ClientToken]]
          tags ← arbitrary[Tags]
          securityGroups ← arbitrary[SecurityGroups]
          sourceDestCheck ← arbitrary[Option[Boolean]]
          hypervisor ← arbitrary[HypervisorType]
          networkInterfaces ← arbitrary[Seq[NetworkInterfaceArgs]]
          iamInstanceProfile ← arbitrary[Option[IamInstanceProfileArgs]]
          ebsOptimized ← arbitrary[Boolean]
          sriovNetSupport ← arbitrary[Option[SriovNetSupport]]
        } yield InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex,
          productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId,
          vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType,
          spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces,
          iamInstanceProfile, ebsOptimized, sriovNetSupport)
      }
    implicit lazy val shrinkInstanceArgs: Shrink[InstanceArgs] =
      Shrink { instance ⇒
        import instance._
        shrink(privateIp).map(InstanceArgs(id, imageId, state, _, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(publicIp).map(InstanceArgs(id, imageId, state, privateIp, _, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(stateReason).map(InstanceArgs(id, imageId, state, privateIp, publicIp, _, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(keyName).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, _, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(amiLaunchIndex).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, _, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(productCodes).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, _, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(launchTime).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, _, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(placement).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, _, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(kernelId).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, _, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(ramdiskId).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, _, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(platform).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, _, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(monitoring).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, _, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(subnetId).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, _, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(vpcId).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, _, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(blockDeviceMapping).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, _, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(lifecycleType).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, _, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(spotInstanceRequestId).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, _, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(clientToken).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, _, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(tags).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, _, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(securityGroups).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, _, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(sourceDestCheck).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, _, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(networkInterfaces).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, _, iamInstanceProfile, ebsOptimized, sriovNetSupport)) append
        shrink(iamInstanceProfile).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, _, ebsOptimized, sriovNetSupport)) append
        shrink(ebsOptimized).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, _, sriovNetSupport)) append
        shrink(sriovNetSupport).map(InstanceArgs(id, imageId, state, privateIp, publicIp, stateReason, keyName, amiLaunchIndex, productCodes, instanceType, launchTime, placement, kernelId, ramdiskId, platform, monitoring, subnetId, vpcId, architecture, rootDevice, blockDeviceMapping, virtualizationType, lifecycleType, spotInstanceRequestId, clientToken, tags, securityGroups, sourceDestCheck, hypervisor, networkInterfaces, iamInstanceProfile, ebsOptimized, _))
      }

    case class BlockDeviceMappingArgs(value: Map[BlockDeviceMappingArgs.DeviceName, BlockDeviceArgs]) {
      def toMap: Map[String, Instance.BlockDevice] = value.map(entry ⇒ entry._1.value → entry._2.toBlockDevice)
      def toAws: util.Collection[aws.InstanceBlockDeviceMapping] =
        value.map { entry ⇒
          new aws.InstanceBlockDeviceMapping()
            .withDeviceName(entry._1.value)
            .withEbs(entry._2.toAws)
        }.asJavaCollection
    }
    object BlockDeviceMappingArgs {
      implicit lazy val arbBlockDeviceMappingArgs: Arbitrary[BlockDeviceMappingArgs] = {
        val k = Math.log(10) / 100
        Arbitrary {
          Gen.sized { size ⇒
            val sizedMax = Math.exp(k * size).toInt
            for {
              n ← Gen.choose(0, sizedMax)
              mappings ← Gen.mapOfN(n, arbitrary[(BlockDeviceMappingArgs.DeviceName, BlockDeviceArgs)])
            } yield BlockDeviceMappingArgs(mappings)
          }
        }
      }
      implicit lazy val shrinkBlockDeviceMappingArgs: Shrink[BlockDeviceMappingArgs] =
        Shrink.xmap(BlockDeviceMappingArgs.apply, BlockDeviceMappingArgs.unapply(_).get)

      case class DeviceName(value: String)
      object DeviceName {
        implicit lazy val arbDeviceName: Arbitrary[DeviceName] = {
          val xvdX = Gen.alphaLowerChar.map(c ⇒ s"xvd$c")
          val xvdXY =
            for {
              x ← Gen.oneOf('b', 'c')
              y ← Gen.alphaLowerChar
            } yield s"xvd$x$y"
          val sda1 = Gen.const("/dev/sda1")
          val sdX = Gen.alphaLowerChar.map(x ⇒ s"/dev/sd$x")
          val XdYN =
            for {
              x ← Gen.oneOf('s', 'h')
              y ← Gen.alphaLowerChar
              n ← Gen.choose(1,15)
            } yield s"/dev/${x}d$y$n"
          Arbitrary(Gen.oneOf(xvdX, xvdXY, sda1, sdX, XdYN).map(DeviceName.apply))
        }
      }
    }

    case class ClientToken(value: String)
    object ClientToken {
      implicit lazy val arbClientToken: Arbitrary[ClientToken] =
        Arbitrary(AwsGen.stringOf(asciiChar, 1, 64).map(ClientToken.apply))
      implicit lazy val shrinkClientToken: Shrink[ClientToken] =
        Shrink { clientToken ⇒
          shrink(clientToken.value).filter(_.nonEmpty).map(ClientToken.apply)
        }
    }

    case class BlockDeviceArgs(id: BlockDeviceArgs.VolumeId,
                               status: AttachmentStatus,
                               deleteOnTermination: Boolean,
                               attachTime: Date) {
      def toBlockDevice = Instance.BlockDevice(id.value, status, deleteOnTermination, attachTime)
      def toAws: aws.EbsInstanceBlockDevice =
        new aws.EbsInstanceBlockDevice()
          .withVolumeId(id.value)
          .withStatus(status.toAws)
          .withDeleteOnTermination(deleteOnTermination)
          .withAttachTime(attachTime)
    }
    object BlockDeviceArgs {
      implicit lazy val arbEbsInstanceBlockDeviceArgs: Arbitrary[BlockDeviceArgs] =
        Arbitrary(Gen.resultOf(BlockDeviceArgs.apply _))

      case class VolumeId(value: String)
      object VolumeId {
        implicit lazy val arbVolumeId: Arbitrary[VolumeId] = Arbitrary(genId("vol").map(VolumeId.apply))
      }
    }

    case class KernelId(value: String)
    object KernelId {
      implicit lazy val arbKernelId: Arbitrary[KernelId] = Arbitrary(genId("aki").map(KernelId.apply))
    }

    case class NetworkInterfaceArgs(id: NetworkInterfaceArgs.NetworkInterfaceId,
                                    subnetId: SubnetId,
                                    vpcId: VpcId,
                                    description: Option[NetworkInterfaceArgs.Description],
                                    owner: AwsGen.Account,
                                    status: NetworkInterfaceStatus,
                                    macAddress: NetworkInterfaceArgs.MacAddress,
                                    privateIp: PrivateIp,
                                    sourceDestCheck: Boolean,
                                    groups: NetworkInterfaceArgs.Groups,
                                    attachment: NetworkInterfaceArgs.AttachmentArgs,
                                    association: Option[NetworkInterfaceArgs.AssociationArgs],
                                    privateIpAddresses: NetworkInterfaceArgs.PrivateIpAddresses) {
      def toNetworkInterface =
        Instance.NetworkInterface(
          id.value,
          subnetId.value,
          vpcId.value,
          description.map(_.value),
          owner.value,
          status,
          macAddress.value,
          privateIp.address,
          privateIp.maybeName,
          sourceDestCheck,
          groups.values.map(_.toGroupIdentifier),
          attachment.toAttachment,
          association.map(_.toAssociation),
          privateIpAddresses.values.map(_.toPrivateIpAddress))
      def toAws = {
        val interface = new aws.InstanceNetworkInterface
        interface.setNetworkInterfaceId(id.value)
        interface.setSubnetId(subnetId.value)
        interface.setVpcId(vpcId.value)
        interface.setDescription(description.map(_.value).getOrElse(""))
        interface.setOwnerId(owner.value)
        interface.setStatus(status.toAws)
        interface.setMacAddress(macAddress.value)
        interface.setPrivateIpAddress(privateIp.address)
        privateIp.maybeName.foreach(n ⇒ interface.setPrivateDnsName(n))
        interface.setSourceDestCheck(sourceDestCheck)
        interface.setGroups(groups.values.map(_.toAws).asJavaCollection)
        interface.setAttachment(attachment.toAws)
        association.foreach(a ⇒ interface.setAssociation(a.toAws))
        interface.setPrivateIpAddresses(privateIpAddresses.values.map(_.toAws).asJavaCollection)
        interface
      }
    }
    object NetworkInterfaceArgs {
      implicit lazy val arbNetworkInterfaceStatus: Arbitrary[NetworkInterfaceStatus] =
        Arbitrary(Gen.oneOf(NetworkInterfaceStatus.values))

      implicit lazy val arbNetworkInterfaceArgs: Arbitrary[NetworkInterfaceArgs] =
        Arbitrary(Gen.resultOf(NetworkInterfaceArgs.apply _))

      implicit lazy val shrinkNetworkInterfaceArgs: Shrink[NetworkInterfaceArgs] =
        Shrink.xmap((NetworkInterfaceArgs.apply _).tupled, NetworkInterfaceArgs.unapply(_).get)

      implicit def shrinkTuple10[
        T1:Shrink, T2:Shrink, T3:Shrink, T4:Shrink, T5:Shrink, T6:Shrink, T7:Shrink, T8:Shrink, T9:Shrink, T10:Shrink,
        T11:Shrink
      ]: Shrink[(T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11)] =
        Shrink { case (t1,t2,t3,t4,t5,t6,t7,t8,t9,t10,t11) =>
          shrink( t1).map((_, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11)) append
          shrink( t2).map((t1, _, t3, t4, t5, t6, t7, t8, t9, t10, t11)) append
          shrink( t3).map((t1, t2, _, t4, t5, t6, t7, t8, t9, t10, t11)) append
          shrink( t4).map((t1, t2, t3, _, t5, t6, t7, t8, t9, t10, t11)) append
          shrink( t5).map((t1, t2, t3, t4, _, t6, t7, t8, t9, t10, t11)) append
          shrink( t6).map((t1, t2, t3, t4, t5, _, t7, t8, t9, t10, t11)) append
          shrink( t7).map((t1, t2, t3, t4, t5, t6, _, t8, t9, t10, t11)) append
          shrink( t8).map((t1, t2, t3, t4, t5, t6, t7, _, t9, t10, t11)) append
          shrink( t9).map((t1, t2, t3, t4, t5, t6, t7, t8, _, t10, t11)) append
          shrink(t10).map((t1, t2, t3, t4, t5, t6, t7, t8, t9,  _, t11)) append
          shrink(t11).map((t1, t2, t3, t4, t5, t6, t7, t8, t9, t10,  _))
        }

      case class AssociationArgs(ipOwner: AssociationArgs.Owner,
                                 ip: PublicIp) {
        def toAssociation = Instance.NetworkInterface.Association(ipOwner.value, ip.maybeName, ip.address)

        def toAws = new aws.InstanceNetworkInterfaceAssociation()
          .withIpOwnerId(ipOwner.value)
          .withPublicDnsName(ip.maybeName.getOrElse(""))
          .withPublicIp(ip.address)
      }
      object AssociationArgs {
        implicit lazy val arbInstanceNetworkInterfaceAssociationArgs: Arbitrary[AssociationArgs] =
          Arbitrary(Gen.resultOf(AssociationArgs.apply _))

        case class Owner(value: String)
        object Owner {
          implicit lazy val arbOwner: Arbitrary[Owner] =
            Arbitrary {
              Gen.frequency(1 → Gen.const("AWS"), 9 → arbitrary[AwsGen.Account].map(_.value)).map(Owner.apply)
            }
        }
      }

      case class AttachmentArgs(id: AttachmentArgs.AttachmentArgsId,
                                attachTime: Date,
                                deleteOnTermination: Boolean,
                                deviceIndex: Index,
                                status: AttachmentStatus) {
        def toAttachment =
          Instance.NetworkInterface.Attachment(
            id.value,
            attachTime,
            deleteOnTermination,
            deviceIndex.value,
            status)
        def toAws = new aws.InstanceNetworkInterfaceAttachment()
          .withAttachmentId(id.value)
          .withAttachTime(attachTime)
          .withDeleteOnTermination(deleteOnTermination)
          .withDeviceIndex(deviceIndex.value)
          .withStatus(status.toAws)
      }
      object AttachmentArgs {
        implicit lazy val arbAttachmentArgs: Arbitrary[AttachmentArgs] =
          Arbitrary(Gen.resultOf(AttachmentArgs.apply _))

        implicit lazy val shrinkAttachmentArgs: Shrink[AttachmentArgs] =
          Shrink.xmap((AttachmentArgs.apply _).tupled, AttachmentArgs.unapply(_).get)

        case class AttachmentArgsId(value: String)
        object AttachmentArgsId {
          implicit lazy val arbAttachmentArgsId: Arbitrary[AttachmentArgsId] =
            Arbitrary(genShortId("eni-attach").map(AttachmentArgsId.apply))
        }
      }

      case class Description(value: String)
      object Description {
        implicit lazy val arbDescription: Arbitrary[Description] =
          Arbitrary(AwsGen.stringOf(asciiChar, 1, 255).map(Description.apply))
        implicit lazy val shrinkDescription: Shrink[Description] =
          Shrink { d ⇒
            shrink(d.value).filter(_.nonEmpty).map(Description.apply)
          }
      }

      case class Groups(values: Seq[GroupIdentifierArgs])
      object Groups {
        implicit lazy val arbGroups: Arbitrary[Groups] =
          Arbitrary {
            Gen.nonEmptyListOf(arbitrary[GroupIdentifierArgs]).map(Groups.apply)
          }

        implicit lazy val shrinkGroups: Shrink[Groups] =
          Shrink { g ⇒
            shrink(g.values).filter(_.nonEmpty).map(Groups.apply)
          }
      }

      case class MacAddress(value: String)
      object MacAddress {
        implicit lazy val arbMacAddress: Arbitrary[MacAddress] =
          Arbitrary {
            Gen.listOfN(12, hexChar).map(chars ⇒ MacAddress(chars.grouped(2).map(_.mkString).mkString(":")))
          }
      }

      case class NetworkInterfaceId(value: String)
      object NetworkInterfaceId {
        implicit lazy val arbNetworkInterfaceId: Arbitrary[NetworkInterfaceId] =
          Arbitrary(genShortId("eni").map(NetworkInterfaceId.apply))
      }

      case class PrivateIpAddressArgs(privateIp: PrivateIp,
                                      primary: Boolean,
                                      association: Option[AssociationArgs]) {
        def toPrivateIpAddress =
          Instance.NetworkInterface.PrivateIpAddress(
            privateIp.address,
            privateIp.maybeName,
            primary,
            association.map(_.toAssociation))
        def toAws = {
          val addr = new aws.InstancePrivateIpAddress()
          addr.setPrivateIpAddress(privateIp.address)
          addr.setPrimary(primary)
          privateIp.maybeName.foreach(n ⇒ addr.setPrivateDnsName(n))
          association.foreach(a ⇒ addr.setAssociation(a.toAws))
          addr
        }
      }
      object PrivateIpAddressArgs {
        implicit lazy val arbPrivateIpAddressArgs: Arbitrary[PrivateIpAddressArgs] =
          Arbitrary(Gen.resultOf(PrivateIpAddressArgs.apply _))

        implicit lazy val shrinkPrivateIpAddress: Shrink[PrivateIpAddressArgs] =
          Shrink.xmap((PrivateIpAddressArgs.apply _).tupled, PrivateIpAddressArgs.unapply(_).get)
      }

      case class PrivateIpAddresses(values: Seq[PrivateIpAddressArgs])
      object PrivateIpAddresses {
        //noinspection NameBooleanParameters
        implicit lazy val arbPrivateIpAddresses: Arbitrary[PrivateIpAddresses] = {
          val k = Math.log(4) / 100

          Arbitrary {
            Gen.sized { size ⇒
              val sizedMax = Math.exp(k * size).toInt
              for {
                n ← Gen.choose(0, sizedMax)
                first ← Gen.resultOf(PrivateIpAddressArgs(_: PrivateIp, true, _: Option[AssociationArgs]))
                rest ← Gen.listOfN(n, Gen.resultOf(PrivateIpAddressArgs(_: PrivateIp, false, _: Option[AssociationArgs])))
              } yield PrivateIpAddresses(first :: rest)
            }
          }
        }

        implicit lazy val shrinkPrivateIpAddresses: Shrink[PrivateIpAddresses] =
          Shrink { addresses ⇒
            shrink(addresses.values).filter(_.nonEmpty).map(PrivateIpAddresses.apply)
          }
      }
    }

    case class PrivateIp(address: String, name: String, maybeName: Option[String])
    object PrivateIp {
      implicit lazy val arbPrivateIp: Arbitrary[PrivateIp] =
        Arbitrary(genAddress("ip-", ".ec2.internal").map((PrivateIp.apply _).tupled))

      implicit lazy val shrinkPrivateIp: Shrink[PrivateIp] = {
        Shrink { ip ⇒
          ip.maybeName match {
            case Some(_) ⇒ PrivateIp(ip.address, ip.name, None) #:: Stream.empty
            case None ⇒ Stream.empty
          }
        }
      }
    }

    case class PublicIp(address: String, name: String, maybeName: Option[String])
    object PublicIp {
      implicit lazy val arbPublicIp: Arbitrary[PublicIp] =
        Arbitrary(genAddress("ec2-", ".compute-1.amazonaws.com").map((PublicIp.apply _).tupled))

      implicit lazy val shrinkPublicIp: Shrink[PublicIp] = {
        Shrink { ip ⇒
          ip.maybeName match {
            case Some(_) ⇒ PublicIp(ip.address, ip.name, None) #:: Stream.empty
            case None ⇒ Stream.empty
          }
        }
      }
    }

    case class RamdiskId(value: String)
    object RamdiskId {
      implicit lazy val arbRamdiskId: Arbitrary[RamdiskId] = Arbitrary(genShortId("ari").map(RamdiskId.apply))
    }

    case class RootDevice(deviceType: DeviceType, name: Option[String])
    object RootDevice {
      implicit lazy val arbRootDevice: Arbitrary[RootDevice] = {
        val instanceStore = Gen.const(RootDevice(DeviceType.InstanceStore, None))
        val ebs = Gen.oneOf("/dev/sda1", "/dev/xvda").map(name ⇒ RootDevice(DeviceType.Ebs, Some(name)))
        Arbitrary {
          Gen.oneOf(instanceStore, ebs)
        }
      }
    }

    case class SecurityGroups(values: Seq[GroupIdentifierArgs]) {
      def toSecurityGroups = values.map(_.toGroupIdentifier)
      def toAws = values.map(_.toAws).asJavaCollection
    }
    object SecurityGroups {
      implicit lazy val arbSecurityGroups: Arbitrary[SecurityGroups] =
        Arbitrary(Gen.nonEmptyListOf(arbitrary[GroupIdentifierArgs]).map(SecurityGroups.apply))

      implicit lazy val shrinkSecurityGroups: Shrink[SecurityGroups] =
        Shrink { groups ⇒
          shrink(groups.values).filter(_.nonEmpty).map(SecurityGroups.apply)
        }
    }

    case class SpotInstanceRequestId(value: String)
    object SpotInstanceRequestId {
      implicit lazy val arbSpotInstanceRequestId: Arbitrary[SpotInstanceRequestId] =
        Arbitrary(genShortId("sir").map(SpotInstanceRequestId.apply))
    }

    case class SriovNetSupport(value: String)
    object SriovNetSupport {
      implicit lazy val arbSriovNetSupport: Arbitrary[SriovNetSupport] =
        Arbitrary(Gen.const(SriovNetSupport("simple")))
    }

    case class SubnetId(value: String)
    object SubnetId {
      implicit lazy val arbSubnetId: Arbitrary[SubnetId] = Arbitrary(genShortId("subnet").map(SubnetId.apply))
    }

    case class Tags(values: Seq[TagArgs]) {
      def toAws: util.Collection[aws.Tag] = values.map(_.toAws).asJavaCollection
      def toMap: Map[String,String] = Tag.toMap(values.map(_.toTag))
    }
    object Tags {
      private def uniqueKeys(tags: Seq[TagArgs]) = {
        val keys = tags.map(_.key)
        keys.distinct == keys
      }

      implicit lazy val arbTags: Arbitrary[Tags] =
        Arbitrary(arbitrary[Seq[TagArgs]].filter(uniqueKeys).map(Tags.apply))

      implicit lazy val shrinkTags: Shrink[Tags] =
        Shrink(tags ⇒ shrink(tags.values).filter(uniqueKeys).map(Tags.apply))
    }

    case class VpcId(value: String)
    object VpcId {
      implicit lazy val arbVpcId: Arbitrary[VpcId] = Arbitrary(genShortId("vpc").map(VpcId.apply))
    }
  }

  case class InstanceId(value: String)
  object InstanceId {
    implicit lazy val arbInstanceId: Arbitrary[InstanceId] = Arbitrary(genId("i").map(InstanceId.apply))
  }

  case class InstanceStateArgs(name: InstanceState.Name) {
    val code = {
      name match {
        case InstanceState.Name.Pending ⇒ 0
        case InstanceState.Name.Running ⇒ 16
        case InstanceState.Name.ShuttingDown ⇒ 32
        case InstanceState.Name.Terminated ⇒ 48
        case InstanceState.Name.Stopping ⇒ 64
        case InstanceState.Name.Stopped ⇒ 80
      }
    }
    def toInstanceState = InstanceState(name, code)
    def toAws = new aws.InstanceState().withCode(code).withName(name.toAws)
  }
  object InstanceStateArgs {
    implicit lazy val arbStateName: Arbitrary[InstanceState.Name] = Arbitrary(Gen.oneOf(InstanceState.Name.values))
    implicit lazy val arbStateArgs: Arbitrary[InstanceStateArgs] = Arbitrary(Gen.resultOf(InstanceStateArgs.apply _))
  }

  case class KeyName(value: String)
  object KeyName {
    lazy implicit val arbKeyName: Arbitrary[KeyName] =
      Arbitrary(AwsGen.stringOf(AwsGen.extendedWordChar, 1, 255).map(KeyName(_)))

    lazy implicit val shrinkKeyName: Shrink[KeyName] =
      Shrink { name ⇒
        shrink(name.value).filter(_.nonEmpty).map(KeyName.apply)
      }
  }

  case class KeyPairArgs(name: KeyName, fingerprint: Fingerprint, key: KeyPairArgs.Key) {
    def toKeyPair: KeyPair = KeyPair(name.value, fingerprint.value, key.value)
  }
  object KeyPairArgs {
    lazy implicit val arbKeyPairArgs: Arbitrary[KeyPairArgs] = Arbitrary(Gen.resultOf(KeyPairArgs.apply _))

    lazy implicit val shrinkKeyPairArgs: Shrink[KeyPairArgs] =
      Shrink.xmap((KeyPairArgs.apply _).tupled, KeyPairArgs.unapply(_).get)

    case class Key(rawBase64: String) {
      def value: String = {
        val filler = rawBase64.length % 4 match {
          case 0 ⇒ ""
          case 2 ⇒ "=="
          case 3 ⇒ "="
        }
        val fullBase64 = rawBase64 + filler
        fullBase64.grouped(76).mkString("-----BEGIN RSA PRIVATE KEY-----\n","\n","\n------END RSA PRIVATE KEY-----")
      }
    }
    object Key {
      lazy implicit val arbKey: Arbitrary[Key] =
        Arbitrary {
          AwsGen.stringOf(base64char, 2, 1024)
            .retryUntil(s ⇒ (s.length % 4) != 1)
            .map(Key(_))
        }

      lazy implicit val shrinkKey: Shrink[Key] =
        Shrink { key ⇒
          shrink(key.rawBase64)
            .filter { data ⇒
              data.length >= 2 && ((data.length % 4) != 1)
            }
            .map(Key(_))
        }
    }
  }

  case class KeyPairInfoArgs(name: KeyName, fingerprint: Fingerprint) {
    def toKeyPairInfo: KeyPairInfo = KeyPairInfo(name.value, fingerprint.value)
  }
  object KeyPairInfoArgs {
    lazy implicit val arbKeyPairInfoArgs: Arbitrary[KeyPairInfoArgs] =
      Arbitrary(Gen.resultOf(KeyPairInfoArgs.apply _))

    lazy implicit val shrinkKeyPairInfoArgs: Shrink[KeyPairInfoArgs] =
      Shrink.xmap((KeyPairInfoArgs.apply _).tupled, KeyPairInfoArgs.unapply(_).get)
  }

  case class MonitoringArgs(state: Monitoring.State) {
    def toMonitoring = Monitoring(state)
    def toAws = new aws.Monitoring().withState(state.toAws)
  }
  object MonitoringArgs {
    implicit lazy val arbMonitoringState: Arbitrary[Monitoring.State] = Arbitrary(Gen.oneOf(Monitoring.State.values))
    implicit lazy val arbMonitoringArgs: Arbitrary[MonitoringArgs] = Arbitrary(Gen.resultOf(MonitoringArgs.apply _))
  }

  case class PlacementArgs(availabilityZone: PlacementArgs.AvailabilityZone,
                           groupName: Option[PlacementArgs.GroupName],
                           tenancy: Option[Tenancy],
                           hostId: Option[PlacementArgs.HostId],
                           affinity: Option[Affinity]) {
    def toPlacement =
      Placement(
        availabilityZone.value,
        groupName.map(_.value),
        tenancy,
        hostId.map(_.value),
        affinity
      )
    def toAws = {
      val placement = new aws.Placement()
      placement.setAvailabilityZone(availabilityZone.value)
      placement.setGroupName(groupName.map(_.value).getOrElse(""))
      tenancy.foreach(t ⇒ placement.setTenancy(t.toAws))
      hostId.foreach(h ⇒ placement.setHostId(h.value))
      affinity.foreach(a ⇒ placement.setAffinity(a.toString))
      placement

    }
  }
  object PlacementArgs {
    implicit lazy val arbPlacementArgs: Arbitrary[PlacementArgs] = Arbitrary(Gen.resultOf(PlacementArgs.apply _))

    implicit lazy val shrinkPlacementArgs: Shrink[PlacementArgs] =
      Shrink.xmap((PlacementArgs.apply _).tupled, PlacementArgs.unapply(_).get)

    case class AvailabilityZone(value: String)
    object AvailabilityZone {
      private val Regions = Seq("eu-west-1", "ap-southeast-1", "ap-southeast-2", "eu-central-1", "ap-northeast-2",
        "ap-northeast-1", "us-east-1", "sa-east-1", "us-west-1", "us-west-2")

      implicit lazy val arbAvailabilityZone: Arbitrary[AvailabilityZone] =
        Arbitrary {
          for {
            region ← Gen.oneOf(Regions)
            zone ← Gen.oneOf('a' to 'e')
          } yield AvailabilityZone(s"$region$zone")
        }
    }

    case class GroupName(value: String)
    object GroupName {
      implicit lazy val arbGroupName: Arbitrary[GroupName] =
        Arbitrary(AwsGen.stringOf(asciiChar, 1, 255).map(GroupName.apply))

      implicit lazy val shrinkGroupName: Shrink[GroupName] =
        Shrink { groupName ⇒
          shrink(groupName.value).filter(_.nonEmpty).map(GroupName.apply)
        }
    }

    case class HostId(value: String)
    object HostId {
      implicit lazy val arbHostId: Arbitrary[HostId] = Arbitrary(genId("h").map(HostId.apply))
    }
  }

  case class ProductCodeArgs(id: ProductCodeArgs.ProductCodeId, `type`: ProductCode.Type) {
    def toProductCode = ProductCode(id.value, `type`)
    def toAws = new aws.ProductCode().withProductCodeId(id.value).withProductCodeType(`type`.toAws)
  }
  object ProductCodeArgs {
    implicit lazy val arbProductCodeType: Arbitrary[ProductCode.Type] = Arbitrary(Gen.oneOf(ProductCode.Type.values))

    implicit lazy val arbProductCodeArgs: Arbitrary[ProductCodeArgs] = Arbitrary(Gen.resultOf(ProductCodeArgs.apply _))

    implicit lazy val shrinkProductCodeArgs: Shrink[ProductCodeArgs] =
      Shrink.xmap((ProductCodeArgs.apply _).tupled, ProductCodeArgs.unapply(_).get)

    case class ProductCodeId(value: String)
    object ProductCodeId {
      implicit lazy val arbProductCodeId: Arbitrary[ProductCodeId] =
        Arbitrary(AwsGen.stringOf(Gen.alphaNumChar, 24, 32).map(ProductCodeId.apply))
      implicit lazy val shrinkProductCodeId: Shrink[ProductCodeId] =
        Shrink { id ⇒
          shrink(id.value).filter(_.length >= 24).map(ProductCodeId.apply)
        }
    }
  }

  case class ReservationArgs(id: ReservationArgs.ReservationId,
                             owner: AwsGen.Account,
                             requester: Option[AwsGen.Account],
                             groups: Seq[GroupIdentifierArgs],
                             instances: ReservationArgs.Instances) {
    def toReservation: Reservation =
      Reservation(id.value, owner.value, requester.map(_.value), groups.map(_.toGroupIdentifier),
        instances.values.map(_.toInstance))

    def toAws: aws.Reservation = {
      val reservation = new aws.Reservation()
      reservation.setReservationId(id.value)
      reservation.setOwnerId(owner.value)
      requester.foreach(id ⇒ reservation.setRequesterId(id.value))
      reservation.setGroups(groups.map(_.toAws).asJavaCollection)
      reservation.setInstances(instances.toAws)
      reservation
    }
  }
  object ReservationArgs {
    implicit lazy val arbReservationArgs: Arbitrary[ReservationArgs] =
      Arbitrary(Gen.resultOf(ReservationArgs.apply _))

    implicit lazy val shrinkReservationArgs: Shrink[ReservationArgs] =
      Shrink.xmap((ReservationArgs.apply _).tupled, ReservationArgs.unapply(_).get)

    case class Instances(values: Seq[InstanceArgs]) {
      def toAws: util.Collection[aws.Instance] = values.map(_.toAws).asJavaCollection
    }
    object Instances {
      implicit lazy val arbInstances: Arbitrary[Instances] =
        Arbitrary {
          Gen.nonEmptyListOf(arbitrary[InstanceArgs]).map(Instances.apply)
        }

      implicit lazy val shrinkInstances: Shrink[Instances] =
        Shrink { instances ⇒
          shrink(instances.values).filter(_.nonEmpty).map(Instances.apply)
        }
    }

    case class ReservationId(value: String)
    object ReservationId {
      implicit lazy val arbReservationId: Arbitrary[ReservationId] =
        Arbitrary {
          for {
            size ← Gen.oneOf(8,17)
            id ← Gen.listOfN(size, hexChar).map(_.mkString)
          } yield ReservationId(s"r-$id")
        }
    }
  }

  case class StateReasonArgs(code: String, message: String) {
    def toStateReason = StateReason(code, message)
    def toAws = new aws.StateReason().withCode(code).withMessage(message)
  }
  object StateReasonArgs {
    implicit lazy val arbStateReasonArgs: Arbitrary[StateReasonArgs] =
      Arbitrary {
        Gen.oneOf(
          StateReasonArgs("Server.SpotInstanceTermination",
            "Server.SpotInstanceTermination: A Spot instance was terminated due to an increase in the market price."),
          StateReasonArgs("Server.InternalError",
            "Server.InternalError: An internal error occurred during instance launch, resulting in termination."),
          StateReasonArgs("Server.InsufficientInstanceCapacity",
            "Server.InsufficientInstanceCapacity: There was insufficient instance capacity to satisfy the launch request."),
          StateReasonArgs("Client.InternalError",
            "Client.InternalError: A client error caused the instance to terminate on launch"),
          StateReasonArgs("Client.InstanceInitiatedShutdown",
            "Client.InstanceInitiatedShutdown: The instance was shut down using the shutdown -h command from the instance."),
          StateReasonArgs("Client.UserInitiatedShutdown",
            "Client.UserInitiatedShutdown: The instance was shut down using the Amazon EC2 API"),
          StateReasonArgs("Client.VolumeLimitExceeded",
            "Client.VolumeLimitExceeded: The limit on the number of EBS volumes or total storage was exceeded. " +
              "Decrease usage or request an increase in your limits."),
          StateReasonArgs("Client.InvalidSnapshot.NotFound",
            "Client.InvalidSnapshot.NotFound: The specified snapshot was not found."),
          StateReasonArgs("Server.ScheduledStop",
            "Server.ScheduledStop: Stopped due to scheduled retirement")
        )
      }
  }

  case class TagArgs(key: String, value: String) {
    def toTag = Tag(key, value)
    def toAws = new aws.Tag().withKey(key).withValue(value)
  }
  object TagArgs {
    implicit lazy val arbTagArgs: Arbitrary[TagArgs] =
      Arbitrary {
        for {
          key ← AwsGen.stringOf(asciiChar, 1, 127)
          value ← AwsGen.stringOf(asciiChar, 1, 255)
        } yield TagArgs(key, value)
      }

    implicit lazy val shrinkTagArgs: Shrink[TagArgs] =
      Shrink { tag ⇒
        shrink(tag.key).filter(_.nonEmpty).map(TagArgs(_, tag.value)) append
        shrink(tag.value).filter(_.nonEmpty).map(TagArgs(tag.key, _))
      }
  }

  implicit lazy val arbAffinity: Arbitrary[Affinity] = Arbitrary(Gen.oneOf(Affinity.values))

  implicit lazy val arbAttachmentStatus: Arbitrary[AttachmentStatus] = Arbitrary(Gen.oneOf(AttachmentStatus.values))

  implicit lazy val arbPlatform: Arbitrary[Platform] = Arbitrary(Gen.oneOf(Platform.values))

  implicit lazy val arbTenancy: Arbitrary[Tenancy] = Arbitrary(Gen.oneOf(Tenancy.values))

  implicit lazy val arbVirtualizationType: Arbitrary[VirtualizationType] = Arbitrary(Gen.oneOf(VirtualizationType.values))

  private def genAddress(prefix: String, suffix: String): Gen[(String, String, Option[String])] =
    for {
      octets ← Gen.listOfN(4, Gen.choose(1, 254))
      hasName ← Gen.frequency(4 → Gen.const(true), 1 → Gen.const(false))
    } yield {
      val address = octets.mkString(".")
      val dnsName = octets.mkString(prefix, "-", suffix)
      (address, dnsName, if (hasName) Some(dnsName) else None)
    }


  private val hexChar = Gen.oneOf(('0' to '9') ++ ('a' to 'f'))
  private val base64char = Gen.oneOf(('A' to 'Z') ++ ('a' to 'z') ++ ('0' to '9') :+ '+' :+ '/')
  private val asciiChar = Gen.oneOf(0x20 to 0x7e).map(_.toChar)
  private def genShortId(prefix: String): Gen[String] =
    Gen.listOfN(8, hexChar).map(chars ⇒ s"$prefix-${chars.mkString}")
  private def genId(prefix: String): Gen[String] =
    for {
      size ← Gen.oneOf(8,17)
      id ← Gen.listOfN(size, hexChar).map(_.mkString)
    } yield s"$prefix-$id"
}
