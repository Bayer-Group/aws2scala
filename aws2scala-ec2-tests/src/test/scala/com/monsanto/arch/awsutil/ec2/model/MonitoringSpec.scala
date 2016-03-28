package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.ec2.model.AwsConverters._
import com.monsanto.arch.awsutil.test_support.AwsEnumerationBehaviours
import com.monsanto.arch.awsutil.testkit.Ec2ScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class MonitoringSpec extends FreeSpec with AwsEnumerationBehaviours {
  "a Monitoring should" - {
    "be constructible from its AWS equivalent" in {
      forAll { monitoring: Monitoring ⇒
        Monitoring.fromAws(monitoring.toAws) shouldBe monitoring
      }
    }
  }

  "the Monitoring.State enumeration" - {
    behave like anAwsEnumeration(Monitoring.State)
  }
}
