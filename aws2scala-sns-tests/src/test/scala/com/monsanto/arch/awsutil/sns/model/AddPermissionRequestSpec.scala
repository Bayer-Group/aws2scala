package com.monsanto.arch.awsutil.sns.model

import com.monsanto.arch.awsutil.sns.model.AwsConverters._
import com.monsanto.arch.awsutil.testkit.SnsScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

import scala.collection.JavaConverters._

class AddPermissionRequestSpec extends FreeSpec {
  "a AddPermissionRequest should" - {
    "be convertible to its AWS equivalent" in {
      forAll { request: AddPermissionRequest ⇒
        request.asAws should have (
          'TopicArn (request.topicArn),
          'Label (request.label),
          'AWSAccountIds (request.accounts.asJava),
          'ActionNames (request.actions.map(_.toString).asJava)
        )
      }
    }
  }
}
