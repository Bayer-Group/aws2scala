package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.testkit.Ec2ScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class TagSpec extends FreeSpec {
  "a Tag should" - {
    "be constructible from its AWS equivalent" in {
      forAll { tag: Tag ⇒
        val awsTag = new aws.Tag()
          .withKey(tag.key)
          .withValue(tag.value)
        Tag.fromAws(awsTag) shouldBe tag
      }
    }

    "be convertible to its AWS equivalent" in {
      forAll { tag: Tag ⇒
        tag.toAws should have (
          'key (tag.key),
          'value (tag.value)
        )
      }
    }
  }

  "the Tag object should" - {
    def asMap(tags: Seq[Tag]): Map[String,String] = tags.map(t ⇒ t.key → t.value).toMap
    "create a tag map from a sequence of Tags" in {
      forAll(minSize(20)) { tags: Seq[Tag] ⇒
        val map = asMap(tags)
        Tag.toMap(tags) shouldBe map
      }
    }

    "create a sequence of Tags from a tag map" in {
      forAll { tags: Seq[Tag] ⇒
        val map = asMap(tags)
        Tag.fromMap(map) should contain theSameElementsAs tags
      }
    }
  }
}
