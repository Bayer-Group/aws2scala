package com.monsanto.arch.awsutil.testkit

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

/** Contains handy generators for ''aws2scala-kms''. */
object KmsGen {
  /** Generates a key alias. */
  val keyAlias: Gen[String] = {
    val aliasChars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') :+ ':' :+ '/' :+ '_' :+ '-'
    val aliasGen =
      for {
        alias ← UtilGen.stringOf(Gen.oneOf(aliasChars), 1, 250)
        addPrefix ← arbitrary[Boolean]
      } yield if (addPrefix) s"alias/$alias" else alias
    aliasGen.suchThat(_.nonEmpty)
  }
}
