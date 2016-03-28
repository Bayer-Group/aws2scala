package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.ec2.model.AwsConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import com.monsanto.arch.awsutil.testkit.Ec2ScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class InstanceSpec extends FreeSpec with AwsEnumerationBehaviours {
  "an Instance should" - {
    "be constructible from its AWS equivalent" in {
      forAll { instance: Instance ⇒
        Instance.fromAws(instance.toAws) shouldBe instance
      }
    }
  }

  "an Instance.BlockDevice should" - {
    "be constructible from its AWS equivalent" in {
      forAll { blockDevice: Instance.BlockDevice ⇒
        Instance.BlockDevice.fromAws(blockDevice.toAws) shouldBe blockDevice
      }
    }
  }

  "the Instance.LifecycleType enumeration" - {
    behave like anAwsEnumeration(Instance.LifecycleType)
  }

  "an Instance.NetworkInterface should" - {
    "be constructible from its AWS equivalent" in {
      forAll { networkInterface: Instance.NetworkInterface ⇒
        Instance.NetworkInterface.fromAws(networkInterface.toAws) shouldBe networkInterface
      }
    }
  }

  "an Instance.NetworkInterface.Association should" - {
    "be constructible from its AWS equivalent" in {
      forAll { association: Instance.NetworkInterface.Association ⇒
        val awsAssociation = association.toAws

        Instance.NetworkInterface.Association.fromAws(awsAssociation) shouldBe association
      }
    }
  }

  "an Instance.NetworkInterface.Attachment should" - {
    "be constructible from its AWS equivalent" in {
      forAll { attachment: Instance.NetworkInterface.Attachment ⇒
        Instance.NetworkInterface.Attachment.fromAws(attachment.toAws) shouldBe attachment
      }
    }
  }

  "an Instance.NetworkInterface.PrivateIpAddress should" - {
    "be constructible from its AWS equivalent" in {
      forAll { ipAddress: Instance.NetworkInterface.PrivateIpAddress ⇒
        val awsIpAddress = ipAddress.toAws

        Instance.NetworkInterface.PrivateIpAddress.fromAws(awsIpAddress) shouldBe ipAddress
      }
    }
  }
}
