package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.model.{KeyPair ⇒ AwsKeyPair}

/** Describes a key pair, including the private key.
  *
  * @param name the name of the key pair
  * @param fingerprint the SHA-1 digest of the DER-encoded private key
  * @param key an unencrypted PEM-encoded RSA private key
  */
case class KeyPair private[ec2] (name: String, fingerprint: String, key: String)

object KeyPair {
  /** Constructs a `KeyPair` instance from an AWS `KeyPair`. */
  private[ec2] def fromAws(aws: AwsKeyPair): KeyPair = KeyPair(aws.getKeyName, aws.getKeyFingerprint, aws.getKeyMaterial)
}
