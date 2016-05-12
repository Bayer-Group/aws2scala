package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class PathSpec extends FreeSpec {
  "a Path should" - {
    "be round-trippable via its string representation" in {
      forAll { path: Path ⇒
        Path.fromString.unapply(path.pathString) shouldBe Some(path)
      }
    }
  }
}
