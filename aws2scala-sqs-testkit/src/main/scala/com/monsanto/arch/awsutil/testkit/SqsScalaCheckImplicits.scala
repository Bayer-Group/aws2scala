package com.monsanto.arch.awsutil.testkit

import com.monsanto.arch.awsutil.Account
import com.monsanto.arch.awsutil.regions.Region
import com.monsanto.arch.awsutil.sqs.model.QueueArn
import com.monsanto.arch.awsutil.testkit.CoreScalaCheckImplicits._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Shrink}

/** Provides ScalaCheck support for ''aws2scala-sqs'' objects. */
object SqsScalaCheckImplicits {
  implicit lazy val arbQueueArn: Arbitrary[QueueArn] =
    Arbitrary {
      for {
        owner ← arbitrary[Account]
        region ← arbitrary[Region]
        queueName ← SqsGen.queueName
      } yield QueueArn(owner, region, queueName)
    }

  implicit lazy val shrinkQueueArn: Shrink[QueueArn] =
    Shrink { arn ⇒
      Shrink.shrink(arn.account).map(x ⇒ arn.copy(account = x)) append
        Shrink.shrink(arn.region).map(x ⇒ arn.copy(region = x)) append
        Shrink.shrink(arn.name).filter(_.nonEmpty).map(x ⇒ arn.copy(name = x))
    }
}
