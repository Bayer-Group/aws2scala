package com.monsanto.arch.awsutil.kms.model

import java.util.{Collection ⇒ JCollection, Map ⇒ JMap}

import com.amazonaws.services.kms.model.{DataKeySpec ⇒ AWSDataKeySpec, GenerateDataKeyRequest ⇒ AWSGenerateDataKeyRequest, GenerateDataKeyWithoutPlaintextRequest}
import com.monsanto.arch.awsutil.kms.asKeyIdentifier
import com.monsanto.arch.awsutil.kms.model.GenerateDataKeyRequest.AWSKeyRequestLike

import scala.collection.JavaConverters._

/** A rough analogue to AWS’s own `GenerateDataKeyRequest`.  Note that whether or not to return the generated key in
  * plaintext is controlled by the `includePlaintext` parameter.  Note that there is no support for specifying the
  * number of bytes as Amazon recommends using the keyspec instead.
  *
  * @param idOrAlias a unique identifier for the master key, which may be a key ARN, alias ARN, key GUID, or an alias
  *                  name
  * @param keySpec defines the encryption algorithm and key size for which to generate a data key
  * @param context additional data to be authenticated during encryption or decryption.  If empty, it is ignored.
  * @param grantTokens a list of grant tokens.  If empty, it is ignored.
  * @param includePlaintext whether or not to include the plaintext of the data key
  */
case class GenerateDataKeyRequest(idOrAlias: String,
                                  context: Map[String,String] = Map.empty,
                                  keySpec: DataKeySpec = DataKeySpec.Aes256,
                                  grantTokens: Seq[String] = Seq.empty,
                                  includePlaintext: Boolean = false) {
  def toAws[T: AWSKeyRequestLike]: T = {
    val helper = implicitly[AWSKeyRequestLike[T]]
    val aws = helper.newRequest(asKeyIdentifier(idOrAlias), keySpec.toAws)
    if (context.nonEmpty) {
      helper.setContext(aws, context.asJava)
    }
    if (grantTokens.nonEmpty) {
      helper.setGrantTokens(aws, grantTokens.asJava)
    }
    aws
  }
}

object GenerateDataKeyRequest {
  trait AWSKeyRequestLike[T] {
    def newRequest(keyId: String, keySpec: AWSDataKeySpec): T
    def setContext(request: T, context: JMap[String,String]): Unit
    def setGrantTokens(request: T, grantTokens: JCollection[String]): Unit
  }

  object AWSKeyRequestLike {
    implicit val withoutPlaintext = new AWSKeyRequestLike[GenerateDataKeyWithoutPlaintextRequest] {
      override def newRequest(keyId: String, keySpec: AWSDataKeySpec) =
        new GenerateDataKeyWithoutPlaintextRequest()
          .withKeyId(keyId)
          .withKeySpec(keySpec)

      override def setContext(request: GenerateDataKeyWithoutPlaintextRequest, encryptionContext: JMap[String, String]) =
        request.setEncryptionContext(encryptionContext)

      override def setGrantTokens(request: GenerateDataKeyWithoutPlaintextRequest, grantTokens: JCollection[String]) =
        request.setGrantTokens(grantTokens)
    }

    implicit val withPlaintext = new AWSKeyRequestLike[AWSGenerateDataKeyRequest] {
      override def newRequest(keyId: String, keySpec: AWSDataKeySpec) =
        new AWSGenerateDataKeyRequest()
          .withKeyId(keyId)
          .withKeySpec(keySpec)

      override def setContext(request: AWSGenerateDataKeyRequest, encryptionContext: JMap[String, String]) =
        request.setEncryptionContext(encryptionContext)

      override def setGrantTokens(request: AWSGenerateDataKeyRequest, grantTokens: JCollection[String]) =
        request.setGrantTokens(grantTokens)
    }
  }
}
