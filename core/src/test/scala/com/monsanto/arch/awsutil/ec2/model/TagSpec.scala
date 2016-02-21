package com.monsanto.arch.awsutil.ec2.model

import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class TagSpec extends FreeSpec {
  "a Tag should" - {
    "be constructible from its AWS equivalent" in {
      forAll { args: EC2Gen.TagArgs ⇒
        Tag.fromAws(args.toAws) shouldBe args.toTag
      }
    }

    "be convertible to its AWS equivalent" in {
      forAll { args: EC2Gen.TagArgs ⇒
        args.toTag.toAws shouldBe args.toAws
      }
    }
  }

  "the Tag object should" - {
    "create a tag map from a sequence of Tags" in {
      forAll { args: Seq[EC2Gen.TagArgs] ⇒
        val keys = args.map(_.key)
        whenever(keys.distinct == keys) {
          val map = args.map(arg ⇒ arg.key → arg.value).toMap
          Tag.toMap(args.map(_.toTag)) shouldBe map
        }
      }
    }

    "create a sequence of Tags from a tag map" in {
      forAll { args: Seq[EC2Gen.TagArgs] ⇒
        val keys = args.map(_.key)
        whenever(keys.distinct == keys) {
          val map = args.map(arg ⇒ arg.key → arg.value).toMap
          Tag.fromMap(map) should contain theSameElementsAs args.map(_.toTag)
        }
      }
    }
  }
}
