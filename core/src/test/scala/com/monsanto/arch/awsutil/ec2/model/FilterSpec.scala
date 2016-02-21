package com.monsanto.arch.awsutil.ec2.model

import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

import scala.collection.JavaConverters._

class FilterSpec extends FreeSpec {
  "a Filter instance should" - {
    "create valid AWS filters" in {
      forAll { args: EC2Gen.FilterArgs ⇒
        val filter = args.toFilter
        filter.toAws should have (
          'name (args.name.value),
          'values (args.values.value.map(_.value).asJava)
        )
      }
    }
  }

  "the Filter object should" - {
    "create a sequence of filters from a map" in {
      forAll { filtersArgs: Seq[EC2Gen.FilterArgs] ⇒
        val names = filtersArgs.map(_.name.value)
        whenever(names.distinct == names) {
          val filters = filtersArgs.map(_.toFilter)
          val map =
            filtersArgs.map { args ⇒
              args.name.value → args.values.value.map(_.value)
            }
              .toMap
          val result = Filter.fromMap(map)
          result should contain theSameElementsAs (filters)
        }
      }
    }
  }
}
