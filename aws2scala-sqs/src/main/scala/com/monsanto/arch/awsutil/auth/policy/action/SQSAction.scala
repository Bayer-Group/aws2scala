package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.SQSActions
import com.monsanto.arch.awsutil.auth.policy.Action

/** Type for all AWS access control policy actions for Amazon SQS. */
sealed abstract class SQSAction(_name: String) extends Action(s"sqs:${_name}")

object SQSAction {
  /** Represents any action executed on Amazon SQS. */
  case object AllSQSActions extends SQSAction("*")

  /** Action for the AddPermission operation. */
  case object AddPermission extends SQSAction("AddPermission")
  /** Action for the ChangeMessageVisibility operation. */
  case object ChangeMessageVisibility extends SQSAction("ChangeMessageVisibility")
  /** Action for the ChangeMessageVisibilityBatch operation. */
  case object ChangeMessageVisibilityBatch extends SQSAction("ChangeMessageVisibilityBatch")
  /** Action for the CreateQueue operation. */
  case object CreateQueue extends SQSAction("CreateQueue")
  /** Action for the DeleteMessage operation. */
  case object DeleteMessage extends SQSAction("DeleteMessage")
  /** Action for the DeleteMessageBatch operation. */
  case object DeleteMessageBatch extends SQSAction("DeleteMessageBatch")
  /** Action for the DeleteQueue operation. */
  case object DeleteQueue extends SQSAction("DeleteQueue")
  /** Action for the GetQueueAttributes operation. */
  case object GetQueueAttributes extends SQSAction("GetQueueAttributes")
  /** Action for the GetQueueUrl operation. */
  case object GetQueueUrl extends SQSAction("GetQueueUrl")
  /** Action for the ListDeadLetterSourceQueues operation. */
  case object ListDeadLetterSourceQueues extends SQSAction("ListDeadLetterSourceQueues")
  /** Action for the ListQueues operation. */
  case object ListQueues extends SQSAction("ListQueues")
  /** Action for the ListQueueTags operation. */
  case object ListQueueTags extends SQSAction("ListQueueTags")
  /** Action for the PurgeQueue operation. */
  case object PurgeQueue extends SQSAction("PurgeQueue")
  /** Action for the ReceiveMessage operation. */
  case object ReceiveMessage extends SQSAction("ReceiveMessage")
  /** Action for the RemovePermission operation. */
  case object RemovePermission extends SQSAction("RemovePermission")
  /** Action for the SendMessage operation. */
  case object SendMessage extends SQSAction("SendMessage")
  /** Action for the SendMessageBatch operation. */
  case object SendMessageBatch extends SQSAction("SendMessageBatch")
  /** Action for the SetQueueAttributes operation. */
  case object SetQueueAttributes extends SQSAction("SetQueueAttributes")
  /** Action for the TagQueue operation. */
  case object TagQueue extends SQSAction("TagQueue")
  /** Action for the UntagQueue operation. */
  case object UntagQueue extends SQSAction("UntagQueue")

  val values: Seq[SQSAction] =
    Seq(
      AllSQSActions, AddPermission, ChangeMessageVisibility, ChangeMessageVisibilityBatch, CreateQueue, DeleteMessage,
      DeleteMessageBatch, DeleteQueue, GetQueueAttributes, GetQueueUrl, ListDeadLetterSourceQueues, ListQueues,
      PurgeQueue, ReceiveMessage, RemovePermission, SendMessage, SendMessageBatch, SetQueueAttributes, ListQueueTags,
      TagQueue, UntagQueue
    )

  private[awsutil] def registerActions(): Unit =
    Action.registerActions(
      SQSActions.AllSQSActions → AllSQSActions,
      SQSActions.AddPermission → AddPermission,
      SQSActions.ChangeMessageVisibility → ChangeMessageVisibility,
      SQSActions.ChangeMessageVisibilityBatch → ChangeMessageVisibilityBatch,
      SQSActions.CreateQueue → CreateQueue,
      SQSActions.DeleteMessage → DeleteMessage,
      SQSActions.DeleteMessageBatch → DeleteMessageBatch,
      SQSActions.DeleteQueue → DeleteQueue,
      SQSActions.GetQueueAttributes → GetQueueAttributes,
      SQSActions.GetQueueUrl → GetQueueUrl,
      SQSActions.ListDeadLetterSourceQueues → ListDeadLetterSourceQueues,
      SQSActions.ListQueues → ListQueues,
      SQSActions.ListQueueTags → ListQueueTags,
      SQSActions.PurgeQueue → PurgeQueue,
      SQSActions.ReceiveMessage → ReceiveMessage,
      SQSActions.RemovePermission → RemovePermission,
      SQSActions.SendMessage → SendMessage,
      SQSActions.SendMessageBatch → SendMessageBatch,
      SQSActions.SetQueueAttributes → SetQueueAttributes,
      SQSActions.TagQueue → TagQueue,
      SQSActions.UntagQueue → UntagQueue
    )
}


