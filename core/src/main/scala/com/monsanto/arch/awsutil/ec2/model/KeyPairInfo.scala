package com.monsanto.arch.awsutil.ec2.model

import com.amazonaws.services.ec2.model.{KeyPairInfo ⇒ AwsKeyPairInfo}

/** Describes a key pair, including the private key.
  *
  * @param name the name of the key pair
  * @param fingerprint the SHA-1 digest of the DER-encoded private key
  */
case class KeyPairInfo private[ec2] (name: String, fingerprint: String)

object KeyPairInfo {
  /** Constructs a `KeyPairInfo` instance from an AWS `KeyPairInfo`. */
  private[ec2] def fromAws(aws: AwsKeyPairInfo): KeyPairInfo = KeyPairInfo(aws.getKeyName, aws.getKeyFingerprint)
}
