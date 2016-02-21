package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.test.AwsEnumerationBehaviours
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class MonitoringSpec extends FreeSpec with AwsEnumerationBehaviours {
  "a Monitoring should" - {
    "be constructible from its AWS equivalent" in {
      forAll { args: EC2Gen.MonitoringArgs ⇒
        Monitoring.fromAws(args.toAws) shouldBe args.toMonitoring
      }
    }
  }

  "the Monitoring.State enumeration" - {
    behave like anAwsEnumeration(Monitoring.State)
  }
}
