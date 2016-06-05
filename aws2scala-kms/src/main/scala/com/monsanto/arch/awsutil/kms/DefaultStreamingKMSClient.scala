package com.monsanto.arch.awsutil.kms

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl._
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.kms.{AWSKMSAsync, model ⇒ aws}
import com.monsanto.arch.awsutil.converters.KmsConverters._
import com.monsanto.arch.awsutil.kms.model._
import com.monsanto.arch.awsutil.{AWSFlow, AWSFlowAdapter}

import scala.collection.JavaConverters._

private[awsutil] class DefaultStreamingKMSClient(kms: AWSKMSAsync) extends StreamingKMSClient {
  private val keyCreator =
    Flow[CreateKeyWithAliasRequest]
      .map(_.asAws)
      .via[aws.CreateKeyResult, NotUsed](AWSFlow.simple(kms.createKeyAsync))
      .map(result ⇒ result.getKeyMetadata.asScala)
      .named("KMS.keyCreator")

  override val keyDeletionScheduler =
    Flow[(String, Int)]
      .map { args ⇒
        new aws.ScheduleKeyDeletionRequest()
          .withKeyId(args._1)
          .withPendingWindowInDays(args._2)
      }
      .via[aws.ScheduleKeyDeletionResult, NotUsed](AWSFlow.simple(kms.scheduleKeyDeletionAsync))
      .map(_.getDeletionDate)
      .named("KMS.keyDeletionScheduler")

  override val keyDeletionCanceller =
    Flow[String]
      .map(id ⇒ new aws.CancelKeyDeletionRequest().withKeyId(id))
      .via[aws.CancelKeyDeletionResult, NotUsed](AWSFlow.simple(kms.cancelKeyDeletionAsync))
      .map(_.getKeyId)
      .named("KMS.keyDeletionCanceller")

  /** An Akka flow that enables a key given an ID, emitting the ID of the enabled key. */
  override val keyEnabler =
    Flow[String]
      .map(id ⇒ new aws.EnableKeyRequest().withKeyId(id))
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput(kms.enableKeyAsync)))
      .map(_.getKeyId)
      .named("KMS.keyEnabler")

  /** An Akka flow that enables a key given an ID, emitting the ID of the enabled key. */
  override val keyDisabler =
    Flow[String]
      .map(id ⇒ new aws.DisableKeyRequest().withKeyId(id))
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput(kms.disableKeyAsync)))
      .map(_.getKeyId)
      .named("KMS.keyDisabler")

  override val keyDescriber =
    Flow[String]
      .map(asKeyIdentifier)
      .map(id ⇒ new aws.DescribeKeyRequest().withKeyId(id))
      .via[aws.DescribeKeyResult,NotUsed](AWSFlow.simple(kms.describeKeyAsync))
      .map(result ⇒ Some(result.getKeyMetadata.asScala))
      .recover { case e: AmazonServiceException if e.getErrorCode == "NotFoundException" ⇒ None }
      .named("KMS.keyDescriber")

  private val keyLister =
    Source.single(new aws.ListKeysRequest)
      .via[aws.ListKeysResult,NotUsed](AWSFlow.pagedByNextMarker(kms.listKeysAsync))
      .mapConcat(_.getKeys.asScala.toList)
      .named("KMS.keyLister")

  override val aliasCreator =
    Flow[(String, String)]
      .map { case (keyId, alias) ⇒
        new aws.CreateAliasRequest()
          .withTargetKeyId(keyId)
          .withAliasName(withAliasPrefix(alias))
      }
      .via(AWSFlow.simple(AWSFlowAdapter.returnInput(kms.createAliasAsync)))
      .map(_.getAliasName.substring(6))
      .named("KMS.aliasCreator")

  private def withAliasPrefix(str: String): String =
    if (str.startsWith("alias/")) {
      str
    } else {
      s"alias/$str"
    }

  override val keyWithAliasCreator =
    Flow.fromGraph(
      GraphDSL.create() { implicit b ⇒
        import GraphDSL.Implicits._

        val inputCopy = b.add(Broadcast[CreateKeyWithAliasRequest](2))
        val metadataCopy = b.add(Broadcast[KeyMetadata](2))
        val aliasInputs = b.add(Zip[String,String])
        val outputSync = b.add(ZipWith[KeyMetadata, String, KeyMetadata]((a,b) ⇒ a))

        // create the key
        inputCopy.out(0) ~> keyCreator ~> metadataCopy.in
        // extract key ID from one copy of the metadata to create the alias
        metadataCopy.out(0).map(_.id) ~> aliasInputs.in0
        // send the alias directly to the alias creator
        inputCopy.out(1).map(_.alias) ~> aliasInputs.in1
        // create the alias and send its output to the sync
        aliasInputs.out ~> aliasCreator ~> outputSync.in1
        // the second copy of the metadata will be output
        metadataCopy.out(1) ~> outputSync.in0

        FlowShape(inputCopy.in, outputSync.out)
      }
    )
    .named("KMS.keyWithAliasCreator")

  private val aliasMapper =
    Source.single(new aws.ListAliasesRequest)
      .via[aws.ListAliasesResult,NotUsed](AWSFlow.pagedByNextMarker(kms.listAliasesAsync))
      .mapConcat(_.getAliases.asScala.toList)
      .fold(Map.empty[String,aws.AliasListEntry])((map, alias) ⇒ map + (alias.getTargetKeyId → alias))
      .named("KMS.aliasMapper")

  override val lister =
    aliasMapper
      .flatMapConcat { aliasMappings ⇒
        keyLister
          .map { keyEntry ⇒
            aliasMappings.get(keyEntry.getKeyId) match {
              case Some(aliasEntry) ⇒ ListEntry(keyEntry, aliasEntry)
              case None ⇒ ListEntry(keyEntry)
            }
          }
      }
      .named("KMS.lister")

  private val withoutPlaintextDataKeyGenerator =
    Flow[GenerateDataKeyRequest]
      .map(_.toAws[aws.GenerateDataKeyWithoutPlaintextRequest])
      .via[aws.GenerateDataKeyWithoutPlaintextResult, NotUsed](AWSFlow.simple(kms.generateDataKeyWithoutPlaintextAsync))
      .map(DataKey.apply)
      .named("KMS.GenerateDataKeyWithoutPlaintext")

  private val withPlaintextDataKeyGenerator =
    Flow[GenerateDataKeyRequest]
      .map(_.toAws[aws.GenerateDataKeyRequest])
      .via[aws.GenerateDataKeyResult, NotUsed](AWSFlow.simple(kms.generateDataKeyAsync))
      .map(DataKey.apply)
      .named("KMS.GenerateDataKey")

  override val dataKeyGenerator =
    Flow[GenerateDataKeyRequest]
      .flatMapConcat { request ⇒
        Source.single(request)
          .via(
            if (request.includePlaintext) withPlaintextDataKeyGenerator
            else withoutPlaintextDataKeyGenerator
          )
      }
      .named("KMS.DataKeyGenerator")

  override val encryptor =
    Flow[EncryptRequest]
      .map(_.toAws)
      .via[aws.EncryptResult, NotUsed](AWSFlow.simple(kms.encryptAsync))
      .map(r ⇒ toBytes(r.getCiphertextBlob))
      .named("KMS.encryptor")

  override def decryptor =
    Flow[DecryptRequest]
      .map(_.toAws)
      .via[aws.DecryptResult, NotUsed](AWSFlow.simple(kms.decryptAsync))
      .map(r ⇒ toBytes(r.getPlaintext))
      .named("KMS.decryptor")
}
