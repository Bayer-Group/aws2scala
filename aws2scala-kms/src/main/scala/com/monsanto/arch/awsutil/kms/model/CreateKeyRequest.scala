package com.monsanto.arch.awsutil.kms.model

import com.amazonaws.services.kms.model.{CreateKeyRequest ⇒ AWSCreateKeyRequest}

/** A rough analogue to AWS‘ own `CreateKeyRequest`, with one major distinction: it includes an alias.
  *
  * @param alias the name to use as the alias for the key
  * @param description an optional description for the new key
  * @param keyUsage the type of use for the key
  * @param policy an optional policy to apply to the key
  */
case class CreateKeyRequest(alias: String,
                            description: Option[String] = None,
                            keyUsage: KeyUsage = KeyUsage.EncryptDecrypt,
                            policy: Option[String] = None) {
  /** Returns an AWS `CreateKeyRequest` object corresponding to this request. */
  def toAws: AWSCreateKeyRequest = {
    val request = new AWSCreateKeyRequest
    request.setKeyUsage(keyUsage.toAws)
    description.foreach(d ⇒ request.setDescription(d))
    policy.foreach(p ⇒ request.setPolicy(p))
    request
  }
}
