package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class PathSpec extends FreeSpec {
  "a Path should" - {
    "be round-trippable via its string representation" in {
      forAll { path: Path ⇒
        Path.fromPathString(path.pathString) shouldBe path
      }
    }

    "should not parse invalid path strings" in {
      forAll { pathString: String ⇒
        whenever(!pathString.matches("^(\u002F)|(\u002F[\u0021-\u007F]+\u002F)$")) {
          an [IllegalArgumentException] shouldBe thrownBy {
            Path.fromPathString(pathString)
          }
        }
      }
    }
  }
}
