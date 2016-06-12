package com.monsanto.arch.awsutil.identitymanagement.model

import akka.Done
import akka.stream.Materializer
import com.monsanto.arch.awsutil.converters.IamConverters._
import com.monsanto.arch.awsutil.identitymanagement.AsyncIdentityManagementClient
import com.monsanto.arch.awsutil.test_support.AdaptableScalaFutures._
import com.monsanto.arch.awsutil.test_support.Materialised
import com.monsanto.arch.awsutil.testkit.IamScalaCheckImplicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

import scala.concurrent.Future

class ManagedPolicyVersionSpec extends FreeSpec with Materialised with MockFactory {
  "a ManagedPolicyVersion should" - {
    "round-trip via its AWS equivalent" in {
      forAll { policyVersion: ManagedPolicyVersion ⇒
        policyVersion.asAws.asScala(policyVersion.policyArn) shouldBe policyVersion
      }
    }

    "get itself" - {
      forAll { (version1: ManagedPolicyVersion, version2: ManagedPolicyVersion) ⇒
        implicit val client = mock[AsyncIdentityManagementClient]("client")

        (client.getPolicyVersion(_: PolicyArn, _: String)(_: Materializer))
          .expects(version1.policyArn, version1.versionId, materialiser)
          .returning(Future.successful(version2))

        val result = version1.refresh().futureValue
        result shouldBe version2
      }
    }

    "set itself the default version" - {
      forAll { version: ManagedPolicyVersion ⇒
        implicit val client = mock[AsyncIdentityManagementClient]("client")

        (client.setDefaultPolicyVersion(_: PolicyArn, _: String)(_: Materializer))
          .expects(version.policyArn, version.versionId, materialiser)
          .returning(Future.successful(Done))

        val result = version.setAsDefault().futureValue
        result shouldBe Done
      }
    }

    "delete itself" - {
      forAll { version: ManagedPolicyVersion ⇒
        implicit val client = mock[AsyncIdentityManagementClient]("client")

        (client.deletePolicyVersion(_: PolicyArn, _: String)(_: Materializer))
          .expects(version.policyArn, version.versionId, materialiser)
          .returning(Future.successful(Done))

        val result = version.delete().futureValue
        result shouldBe Done
      }
    }
  }
}
