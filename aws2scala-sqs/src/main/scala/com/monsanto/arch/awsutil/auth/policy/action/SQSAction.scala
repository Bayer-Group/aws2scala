package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.SQSActions
import com.monsanto.arch.awsutil.auth.policy.Action

/** Type for all AWS access control policy actions for Amazon SQS. */
sealed trait SQSAction extends Action

object SQSAction {
  /** Represents any action executed on Amazon SQS. */
  case object AllSQSActions extends SQSAction

  /** Action for the AddPermission operation. */
  case object AddPermission extends SQSAction
  /** Action for the ChangeMessageVisibility operation. */
  case object ChangeMessageVisibility extends SQSAction
  /** Action for the ChangeMessageVisibilityBatch operation. */
  case object ChangeMessageVisibilityBatch extends SQSAction
  /** Action for the CreateQueue operation. */
  case object CreateQueue extends SQSAction
  /** Action for the DeleteMessage operation. */
  case object DeleteMessage extends SQSAction
  /** Action for the DeleteMessageBatch operation. */
  case object DeleteMessageBatch extends SQSAction
  /** Action for the DeleteQueue operation. */
  case object DeleteQueue extends SQSAction
  /** Action for the GetQueueAttributes operation. */
  case object GetQueueAttributes extends SQSAction
  /** Action for the GetQueueUrl operation. */
  case object GetQueueUrl extends SQSAction
  /** Action for the ListDeadLetterSourceQueues operation. */
  case object ListDeadLetterSourceQueues extends SQSAction
  /** Action for the ListQueues operation. */
  case object ListQueues extends SQSAction
  /** Action for the PurgeQueue operation. */
  case object PurgeQueue extends SQSAction
  /** Action for the ReceiveMessage operation. */
  case object ReceiveMessage extends SQSAction
  /** Action for the RemovePermission operation. */
  case object RemovePermission extends SQSAction
  /** Action for the SendMessage operation. */
  case object SendMessage extends SQSAction
  /** Action for the SendMessageBatch operation. */
  case object SendMessageBatch extends SQSAction
  /** Action for the SetQueueAttributes operation. */
  case object SetQueueAttributes extends SQSAction

  val values: Seq[SQSAction] =
    Seq(
      AllSQSActions, AddPermission, ChangeMessageVisibility, ChangeMessageVisibilityBatch, CreateQueue, DeleteMessage,
      DeleteMessageBatch, DeleteQueue, GetQueueAttributes, GetQueueUrl, ListDeadLetterSourceQueues, ListQueues,
      PurgeQueue, ReceiveMessage, RemovePermission, SendMessage, SendMessageBatch, SetQueueAttributes
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
      SQSActions.PurgeQueue → PurgeQueue,
      SQSActions.ReceiveMessage → ReceiveMessage,
      SQSActions.RemovePermission → RemovePermission,
      SQSActions.SendMessage → SendMessage,
      SQSActions.SendMessageBatch → SendMessageBatch,
      SQSActions.SetQueueAttributes → SetQueueAttributes
    )
}


