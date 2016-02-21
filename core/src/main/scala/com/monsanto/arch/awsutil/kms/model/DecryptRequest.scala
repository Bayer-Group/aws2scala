package com.monsanto.arch.awsutil.kms.model

import java.nio.ByteBuffer

import com.amazonaws.services.kms.model.{DecryptRequest ⇒ AWSDecryptRequest}

import scala.collection.JavaConverters._

/** A rough analogue to AWS’ own `DecryptRequest`.
  *
  * @param ciphertext the ciphertext context to decrypt
  * @param context an encryption context, which must have been used during encryption.  An empty context will be
  *                ignored.
  * @param grantTokens a list of grant tokens.  An empty list will ignored.
  */
case class DecryptRequest(ciphertext: Array[Byte],
                          context: Map[String,String] = Map.empty,
                          grantTokens: Seq[String] = Seq.empty) {
  /** Returns the equivalent AWS request object. */
  def toAws: AWSDecryptRequest = {
    val aws = new AWSDecryptRequest()
      .withCiphertextBlob(ByteBuffer.wrap(ciphertext))
    if (context.nonEmpty) {
      aws.setEncryptionContext(context.asJava)
    }
    if (grantTokens.nonEmpty) {
      aws.setGrantTokens(grantTokens.asJava)
    }
    aws
  }
}
