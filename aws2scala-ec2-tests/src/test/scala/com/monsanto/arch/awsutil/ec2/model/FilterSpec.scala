package com.monsanto.arch.awsutil.ec2.model

import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import com.monsanto.arch.awsutil.testkit.Ec2ScalaCheckImplicits._

import scala.collection.JavaConverters._

class FilterSpec extends FreeSpec {
  "a Filter instance should" - {
    "create valid AWS filters" in {
      forAll { filter: Filter ⇒
        filter.toAws should have (
          'name (filter.name),
          'values (filter.values.asJava)
        )
      }
    }
  }

  "the Filter object should" - {
    "create a sequence of filters from a map" in {
      forAll { filters: Seq[Filter] ⇒
        val map = filters.map(filter ⇒ filter.name → filter.values) .toMap
        val result = Filter.fromMap(map)
        result should contain theSameElementsAs filters
      }
    }
  }
}
