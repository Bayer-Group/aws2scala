package com.monsanto.arch.awsutil.ec2.model

import org.scalatest.FreeSpec
import org.scalatest.Matchers._
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class ReservationSpec extends FreeSpec {
  "a Reservation should" - {
    "be constructible from its AWS equivalent" in {
      forAll(maxSize(25)) { args: EC2Gen.ReservationArgs ⇒
        Reservation.fromAws(args.toAws) shouldBe args.toReservation
      }
    }
  }
}
