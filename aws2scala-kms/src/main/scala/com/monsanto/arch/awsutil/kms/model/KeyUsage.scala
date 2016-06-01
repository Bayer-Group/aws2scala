package com.monsanto.arch.awsutil.kms.model

/** Parent class for all key usage types.
  *
  * @param name the unique name of the key usage
  */
sealed abstract class KeyUsage(val name: String)

object KeyUsage {
  /** The key may be used encryption and decryption. */
  case object EncryptDecrypt extends KeyUsage("ENCRYPT_DECRYPT")

  /** All valid values for the enumeration. */
  val values: Seq[KeyUsage] = Seq(EncryptDecrypt)

  /** Utility for building/extracting a `KeyUsage` instance from an identifier string. */
  object fromName {
    /** Returns a `KeyUsage` instance from a string containing its identifier. */
    def apply(name: String): KeyUsage =
      unapply(name).getOrElse(throw new IllegalArgumentException(s"’$name‘ is not a valid key usage."))

    /** Extracts a `KeyUsage` instance from a string containing its identifier. */
    def unapply(name: String): Option[KeyUsage] = values.find(_.name == name)
  }
}
