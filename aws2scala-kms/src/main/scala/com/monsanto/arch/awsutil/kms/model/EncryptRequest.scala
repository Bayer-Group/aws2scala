package com.monsanto.arch.awsutil.kms.model

import java.nio.ByteBuffer

import com.amazonaws.services.kms.model.{EncryptRequest ⇒ AWSEncryptRequest}
import com.monsanto.arch.awsutil.kms.asKeyIdentifier

import scala.collection.JavaConverters._

/** A rough analogue to AWS’ own `EncryptRequest`.
  *
  * @param idOrAlias may be a key UUID, key or alias ARN, a full alias (including the `alias/` prefix), or a simple
  *                  alias (to which `alias/` will be prepended)
  * @param plaintext the plaintext context to encrypt
  * @param context an encryption context, which must be used for later decryption.  An empty context will be ignored.
  * @param grantTokens a list of grant tokens.  An empty list will ignored.
  */
case class EncryptRequest(idOrAlias: String,
                          plaintext: Array[Byte],
                          context: Map[String,String] = Map.empty,
                          grantTokens: Seq[String] = Seq.empty) {
  /** Returns the equivalent AWS request object. */
  def toAws: AWSEncryptRequest = {
    val aws = new AWSEncryptRequest()
      .withKeyId(asKeyIdentifier(idOrAlias))
      .withPlaintext(ByteBuffer.wrap(plaintext))
    if (context.nonEmpty) {
      aws.setEncryptionContext(context.asJava)
    }
    if (grantTokens.nonEmpty) {
      aws.setGrantTokens(grantTokens.asJava)
    }
    aws
  }
}
