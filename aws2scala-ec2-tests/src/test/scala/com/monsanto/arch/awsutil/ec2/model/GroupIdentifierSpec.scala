package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.{model ⇒ aws}
import com.monsanto.arch.awsutil.testkit.Ec2ScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class GroupIdentifierSpec extends FreeSpec {
  "a GroupIdentifier should" - {
    "be constructible from its AWS equivalent" in {
      forAll { groupId: GroupIdentifier ⇒
        val awsId = new aws.GroupIdentifier()
          .withGroupId(groupId.id)
          .withGroupName(groupId.name)
        GroupIdentifier.fromAws(awsId) shouldBe groupId
      }
    }
  }
}
