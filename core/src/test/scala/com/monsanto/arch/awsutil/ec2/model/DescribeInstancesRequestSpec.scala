package com.monsanto.arch.awsutil.ec2.model

import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

import scala.collection.JavaConverters._

class DescribeInstancesRequestSpec extends FreeSpec {
  "a DescribeInstancesRequest should" - {
    "convert to an AWS request" in {
      forAll { args: EC2Gen.DescribeInstanceRequestArgs ⇒
        val request = args.toRequest
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
      forAll { arg: EC2Gen.InstanceId ⇒
        val id = arg.value
        DescribeInstancesRequest.withId(id) shouldBe DescribeInstancesRequest(Seq(id), Seq.empty)
      }
    }

    "a method for getting all key pairs matching filters specified" - {
      "as a sequence of Filters" in {
        forAll { arg: Seq[EC2Gen.FilterArgs] ⇒
          val filters = arg.map(_.toFilter)
          DescribeInstancesRequest.filter(filters) shouldBe DescribeInstancesRequest(Seq.empty, filters)
        }
      }

      "as a map" in {
        forAll { arg: Seq[EC2Gen.FilterArgs] ⇒
          val filters = arg.map(_.toFilter)
          val names = filters.map(_.name)
          val mapFilter = filters.map(f ⇒ f.name → f.values).toMap

          whenever(names.distinct == names) {
            val result = DescribeInstancesRequest.filter(mapFilter)
            result.instanceIds shouldBe empty
            result.filters should contain theSameElementsAs filters
          }
        }
      }
    }
  }
}
