package com.monsanto.arch.awsutil.kms

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.monsanto.arch.awsutil.kms.model._

import scala.concurrent.Future

private[awsutil] class DefaultAsyncKMSClient(streamingClient: StreamingKMSClient) extends AsyncKMSClient {
  override def createKey(alias: String)(implicit m: Materializer): Future[KeyMetadata] =
    createKey(CreateKeyWithAliasRequest(alias, None, None, KeyUsage.EncryptDecrypt))

  override def createKey(alias: String, description: String)(implicit m: Materializer): Future[KeyMetadata] =
    createKey(CreateKeyWithAliasRequest(alias, None, Some(description), KeyUsage.EncryptDecrypt))

  override def createKey(createKeyRequest: CreateKeyWithAliasRequest)(implicit m: Materializer): Future[KeyMetadata] =
    Source.single(createKeyRequest)
      .via(streamingClient.keyWithAliasCreator)
      .runWith(Sink.head)

  override def scheduleKeyDeletion(keyId: String, pendingWindowDays: Int)(implicit m: Materializer) =
    Source.single((keyId, pendingWindowDays))
      .via(streamingClient.keyDeletionScheduler)
      .runWith(Sink.head)

  override def cancelKeyDeletion(keyId: String)(implicit m: Materializer) =
    Source.single(keyId)
      .via(streamingClient.keyDeletionCanceller)
      .runWith(Sink.ignore)

  override def enableKey(id: String)(implicit m: Materializer) =
    Source.single(id)
      .via(streamingClient.keyEnabler)
      .runWith(Sink.ignore)

  override def disableKey(id: String)(implicit m: Materializer) =
    Source.single(id)
      .via(streamingClient.keyDisabler)
      .runWith(Sink.ignore)

  override def describeKey(idOrAlis: String)(implicit m: Materializer) =
    Source.single(idOrAlis)
      .via(streamingClient.keyDescriber)
      .runWith(Sink.head)

  override def listKeys()(implicit m: Materializer) = streamingClient.lister.runWith(Sink.seq)

  override def generateDataKey(idOrAlias: String)(implicit m: Materializer) =
    generateDataKey(GenerateDataKeyRequest(idOrAlias))

  override def generateDataKey(idOrAlias: String, context: Map[String, String])(implicit m: Materializer) =
    generateDataKey(GenerateDataKeyRequest(idOrAlias, context))

  override def generateDataKey(request: GenerateDataKeyRequest)(implicit m: Materializer) =
    Source.single(request)
      .via(streamingClient.dataKeyGenerator)
      .runWith(Sink.head)

  override def encrypt(idOrAlias: String, plaintext: Array[Byte])(implicit m: Materializer) =
    encrypt(EncryptRequest(idOrAlias, plaintext))

  override def encrypt(idOrAlias: String, plaintext: Array[Byte], context: Map[String, String])
                      (implicit m: Materializer) =
    encrypt(EncryptRequest(idOrAlias, plaintext, context))

  override def encrypt(request: EncryptRequest)(implicit m: Materializer) =
    Source.single(request)
      .via(streamingClient.encryptor)
      .runWith(Sink.head)

  override def decrypt(ciphertext: Array[Byte])(implicit m: Materializer) = decrypt(DecryptRequest(ciphertext))

  override def decrypt(ciphertext: Array[Byte], context: Map[String, String])(implicit m: Materializer) =
    decrypt(DecryptRequest(ciphertext, context))

  override def decrypt(request: DecryptRequest)(implicit m: Materializer) =
    Source.single(request)
      .via(streamingClient.decryptor)
      .runWith(Sink.head)
}
