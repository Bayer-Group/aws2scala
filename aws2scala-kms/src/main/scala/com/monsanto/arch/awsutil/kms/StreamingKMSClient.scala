package com.monsanto.arch.awsutil.kms

import java.util.Date

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import com.monsanto.arch.awsutil.StreamingAwsClient
import com.monsanto.arch.awsutil.kms.model._

/** Akka flow-based client for Amazon’s key management service.
  *
  * @author Daniel Solano Gómez
  */
trait StreamingKMSClient extends StreamingAwsClient {
  /** An Akka flow that schedules a key for deletion (with the given pending window in days) and emits the scheduled
    * deletion date.
    */
  def keyDeletionScheduler: Flow[(String, Int), Date, NotUsed]

  /** An Akka flow that requests cancellation of key deletion given a key ID, emitting the key ID. */
  def keyDeletionCanceller: Flow[String, String, NotUsed]

  /** An Akka flow that enables a key given an ID, emitting the ID of the enabled key. */
  def keyEnabler: Flow[String, String, NotUsed]

  /** An Akka flow that enables a key given an ID, emitting the ID of the enabled key. */
  def keyDisabler: Flow[String, String, NotUsed]

  /** An Akka flow that given a key identifier or alias will describe the key, if found.  If the input does not parse
    * as a UUID, start with `arn:`, or start with `alias/`, then it will be prepended with `alias/`.
    */
  def keyDescriber: Flow[String, Option[KeyMetadata], NotUsed]

  /** An Akka flow that given a key identifier and an alias name (not including the `alias/` prefix) will create a key
    * alias.  Emits the alias name given.
    */
  def aliasCreator: Flow[(String, String), String, NotUsed]

  /** An Akka flow that given a key creation request will create the key and the corresponding alias. Emits the
    * resulting key metadata. */
  def keyWithAliasCreator: Flow[CreateKeyRequest, KeyMetadata, NotUsed]

  /** An Akka source that will list all keys and their corresponding aliases. */
  def lister: Source[ListEntry, NotUsed]

  /** An Akka flow that will generate a new data key given a request. */
  def dataKeyGenerator: Flow[GenerateDataKeyRequest, DataKey, NotUsed]

  /** An Akka flow that will emit ciphertext for each encryption request. */
  def encryptor: Flow[EncryptRequest, Array[Byte], NotUsed]

  /** An Akka flow that will emit plaintext for each decryption request. */
  def decryptor: Flow[DecryptRequest, Array[Byte], NotUsed]
}
