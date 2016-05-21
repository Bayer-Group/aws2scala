package com.monsanto.arch.awsutil.auth.policy

import com.monsanto.arch.awsutil.converters.CoreConverters._
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ResourceSpec extends FreeSpec {
  "a Resource" - {
    "can be round-tripped via its AWS equivalent" in {
      forAll { resource: Resource ⇒
        resource.asAws.asScala shouldBe resource
      }
    }

    "* will always be AllResources" in {
      Resource("*").asAws.asScala shouldBe theSameInstanceAs (Resource.AllResources)
    }
  }
}
