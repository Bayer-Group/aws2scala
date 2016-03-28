package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.testkit.Ec2Gen
import com.monsanto.arch.awsutil.testkit.Ec2ScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

import scala.collection.JavaConverters._

class DescribeInstancesRequestSpec extends FreeSpec {
  "a DescribeInstancesRequest should" - {
    "convert to an AWS request" in {
      forAll { request: DescribeInstancesRequest ⇒
        val aws = request.toAws

        aws should have (
          'instanceIds (request.instanceIds.asJava),
          'filters (request.filters.map(_.toAws).asJava)
        )
      }
    }
  }

  "the DescribeInstancesRequest object provides" - {
    "a handy value for describing all instances" in {
      val request = DescribeInstancesRequest.allInstances

      request shouldBe DescribeInstancesRequest(Seq.empty, Seq.empty)
    }

    "a method for getting a particular instance with a given ID" in {
      forAll(Ec2Gen.instanceId) { id ⇒
        DescribeInstancesRequest.withId(id) shouldBe DescribeInstancesRequest(Seq(id), Seq.empty)
      }
    }

    "a method for getting all key pairs matching filters specified" - {
      "as a sequence of Filters" in {
        forAll { filters: Seq[Filter] ⇒
          DescribeInstancesRequest.filter(filters) shouldBe DescribeInstancesRequest(Seq.empty, filters)
        }
      }

      "as a map" in {
        forAll { filters: Seq[Filter] ⇒
          val mapFilter = filters.map(f ⇒ f.name → f.values).toMap

          val result = DescribeInstancesRequest.filter(mapFilter)
          result.instanceIds shouldBe empty
          result.filters should contain theSameElementsAs filters
        }
      }
    }
  }
}
