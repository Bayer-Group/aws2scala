package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.test.AwsEnumerationBehaviours
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class InstanceSpec extends FreeSpec with AwsEnumerationBehaviours {
  "an Instance should" - {
    "be constructible from its AWS equivalent" in {
      forAll { args: EC2Gen.InstanceArgs ⇒
        Instance.fromAws(args.toAws) shouldBe args.toInstance
      }
    }
  }

  "an Instance.BlockDevice should" - {
    "be constructible from its AWS equivalent" in {
      forAll { args: EC2Gen.InstanceArgs.BlockDeviceArgs ⇒
        Instance.BlockDevice.fromAws(args.toAws) shouldBe args.toBlockDevice
      }
    }
  }

  "the Instance.LifecycleType enumeration" - {
    behave like anAwsEnumeration(Instance.LifecycleType)
  }

  "an Instance.NetworkInterface should" - {
    "be constructible from its AWS equivalent" in {
      forAll { args: EC2Gen.InstanceArgs.NetworkInterfaceArgs ⇒
        Instance.NetworkInterface.fromAws(args.toAws) shouldBe args.toNetworkInterface
      }
    }
  }

  "an Instance.NetworkInterface.Association should" - {
    "be constructible from its AWS equivalent" in {
      forAll { args: EC2Gen.InstanceArgs.NetworkInterfaceArgs.AssociationArgs ⇒
        Instance.NetworkInterface.Association.fromAws(args.toAws) shouldBe args.toAssociation
      }
    }
  }

  "an Instance.NetworkInterface.Attachment should" - {
    "be constructible from its AWS equivalent" in {
      forAll { args: EC2Gen.InstanceArgs.NetworkInterfaceArgs.AttachmentArgs ⇒
        Instance.NetworkInterface.Attachment.fromAws(args.toAws) shouldBe args.toAttachment
      }
    }
  }

  "an Instance.NetworkInterface.PrivateIpAddress should" - {
    "be constructible from its AWS equivalent" in {
      forAll { args: EC2Gen.InstanceArgs.NetworkInterfaceArgs.PrivateIpAddressArgs ⇒
        Instance.NetworkInterface.PrivateIpAddress.fromAws(args.toAws) shouldBe args.toPrivateIpAddress
      }
    }
  }
}
