package com.monsanto.arch.awsutil.ec2.model

import com.monsanto.arch.awsutil.ec2.model.AwsConverters._
import com.monsanto.arch.awsutil.testkit.Ec2ScalaCheckImplicits._
import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ReservationSpec extends FreeSpec {
  "a Reservation should" - {
    "be constructible from its AWS equivalent" in {
      forAll { reservation: Reservation ⇒
        Reservation.fromAws(reservation.toAws) shouldBe reservation
      }
    }
  }
}
