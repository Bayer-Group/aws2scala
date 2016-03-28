package com.monsanto.arch.awsutil.testkit

import java.io.StringWriter
import java.security.{KeyPairGenerator, Security}

import com.monsanto.arch.awsutil.ec2.model.Filter
import com.monsanto.arch.awsutil.testkit.Ec2ScalaCheckImplicits._
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

object Ec2Gen {
  val filterSeq: Gen[Seq[Filter]] =
    Gen.sized { size ⇒
      val maxSize = Math.sqrt(size).toInt
      for {
        n ← Gen.choose(0, maxSize)
        filters ← Gen.listOfN(n, arbitrary[Filter])
          .retryUntil { filters ⇒
            val names = filters.map(_.name)
            names.distinct == names
          }
      } yield filters
    }

  val keyFingerprint: Gen[String] =
    Gen.listOfN(20, UtilGen.lowerHexChar)
      .map(chars ⇒ chars.grouped(2).map(_.mkString).mkString(":"))

  val keyName: Gen[String] = UtilGen.stringOf(UtilGen.asciiChar, 1, 255).suchThat(_.nonEmpty)

  val privateKey: Gen[String] = {
    Security.addProvider(new BouncyCastleProvider)
    val generator = KeyPairGenerator.getInstance("RSA", "BC")
    generator.initialize(512)

    Gen.delay {
      val pair = generator.genKeyPair()
      val out = new StringWriter()
      val pemWriter = new JcaPEMWriter(out)
      pemWriter.writeObject(pair.getPrivate)
      pemWriter.close()
      Gen.const(out.toString)
    }
  }

  val groupIdentifierId: Gen[String] = shortId("sg")

  val hostId: Gen[String] = shortId("h")

  val imageId: Gen[String] = id("ami")

  val instanceId: Gen[String] = id("i")

  val instanceNetworkInterfaceId: Gen[String] = shortId("eni")

  val instanceNetworkInterfaceAttachmentId: Gen[String] = shortId("emi-attach")

  val kernelId: Gen[String] = id("aki")

  val ramdiskId: Gen[String] = shortId("ari")

  val spotInstanceRequestId: Gen[String] = id("sir")

  val subnetId: Gen[String] = id("subnet")

  val volumeId: Gen[String] = id("vol")

  val vpcId: Gen[String] = id("vpc")

  private def shortId(prefix: String): Gen[String] =
    Gen.listOfN(8, UtilGen.lowerHexChar).map(chars ⇒ s"$prefix-${chars.mkString}")

  private def id(prefix: String): Gen[String] =
    for {
      size ← Gen.oneOf(8,17)
      id ← Gen.listOfN(size, UtilGen.lowerHexChar).map(_.mkString)
    } yield s"$prefix-$id"
}
