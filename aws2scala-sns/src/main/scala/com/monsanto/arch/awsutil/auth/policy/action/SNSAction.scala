package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.SNSActions
import com.monsanto.arch.awsutil.auth.policy.Action

/** Type for all AWS access control policy actions for Amazon SNS. */
sealed trait SNSAction extends Action

object SNSAction {
  /** Represents any action executed on Amazon SNS. */
  case object AllSNSActions extends SNSAction
  /** Action for the AddPermission operation. */
  case object AddPermission extends SNSAction
  /** Action for the ConfirmSubscription operation. */
  case object ConfirmSubscription extends SNSAction
  /** Action for the CreatePlatformApplication operation. */
  case object CreatePlatformApplication extends SNSAction
  /** Action for the CreatePlatformEndpoint operation. */
  case object CreatePlatformEndpoint extends SNSAction
  /** Action for the CreateTopic operation. */
  case object CreateTopic extends SNSAction
  /** Action for the DeleteEndpoint operation. */
  case object DeleteEndpoint extends SNSAction
  /** Action for the DeletePlatformApplication operation. */
  case object DeletePlatformApplication extends SNSAction
  /** Action for the DeleteTopic operation. */
  case object DeleteTopic extends SNSAction
  /** Action for the GetEndpointAttributes operation. */
  case object GetEndpointAttributes extends SNSAction
  /** Action for the GetPlatformApplicationAttributes operation. */
  case object GetPlatformApplicationAttributes extends SNSAction
  /** Action for the GetSubscriptionAttributes operation. */
  case object GetSubscriptionAttributes extends SNSAction
  /** Action for the GetTopicAttributes operation. */
  case object GetTopicAttributes extends SNSAction
  /** Action for the ListEndpointsByPlatformApplication operation. */
  case object ListEndpointsByPlatformApplication extends SNSAction
  /** Action for the ListPlatformApplications operation. */
  case object ListPlatformApplications extends SNSAction
  /** Action for the ListSubscriptions operation. */
  case object ListSubscriptions extends SNSAction
  /** Action for the ListSubscriptionsByTopic operation. */
  case object ListSubscriptionsByTopic extends SNSAction
  /** Action for the ListTopics operation. */
  case object ListTopics extends SNSAction
  /** Action for the Publish operation. */
  case object Publish extends SNSAction
  /** Action for the RemovePermission operation. */
  case object RemovePermission extends SNSAction
  /** Action for the SetEndpointAttributes operation. */
  case object SetEndpointAttributes extends SNSAction
  /** Action for the SetPlatformApplicationAttributes operation. */
  case object SetPlatformApplicationAttributes extends SNSAction
  /** Action for the SetSubscriptionAttributes operation. */
  case object SetSubscriptionAttributes extends SNSAction
  /** Action for the SetTopicAttributes operation. */
  case object SetTopicAttributes extends SNSAction
  /** Action for the Subscribe operation. */
  case object Subscribe extends SNSAction
  /** Action for the Unsubscribe operation. */
  case object Unsubscribe extends SNSAction

  val values: Seq[SNSAction] = Seq(
    AllSNSActions, AddPermission, ConfirmSubscription, CreatePlatformApplication, CreatePlatformEndpoint, CreateTopic,
    DeleteEndpoint, DeletePlatformApplication, DeleteTopic, GetEndpointAttributes, GetPlatformApplicationAttributes,
    GetSubscriptionAttributes, GetTopicAttributes, ListEndpointsByPlatformApplication, ListPlatformApplications,
    ListSubscriptions, ListSubscriptionsByTopic, ListTopics, Publish, RemovePermission, SetEndpointAttributes,
    SetPlatformApplicationAttributes, SetSubscriptionAttributes, SetTopicAttributes, Subscribe, Unsubscribe
  )

  private[awsutil] def registerActions(): Unit =
    Action.registerActions(
      SNSActions.AllSNSActions → SNSAction.AllSNSActions,
      SNSActions.AddPermission → SNSAction.AddPermission,
      SNSActions.ConfirmSubscription → SNSAction.ConfirmSubscription,
      SNSActions.CreatePlatformApplication → SNSAction.CreatePlatformApplication,
      SNSActions.CreatePlatformEndpoint → SNSAction.CreatePlatformEndpoint,
      SNSActions.CreateTopic → SNSAction.CreateTopic,
      SNSActions.DeleteEndpoint → SNSAction.DeleteEndpoint,
      SNSActions.DeletePlatformApplication → SNSAction.DeletePlatformApplication,
      SNSActions.DeleteTopic → SNSAction.DeleteTopic,
      SNSActions.GetEndpointAttributes → SNSAction.GetEndpointAttributes,
      SNSActions.GetPlatformApplicationAttributes → SNSAction.GetPlatformApplicationAttributes,
      SNSActions.GetSubscriptionAttributes → SNSAction.GetSubscriptionAttributes,
      SNSActions.GetTopicAttributes → SNSAction.GetTopicAttributes,
      SNSActions.ListEndpointsByPlatformApplication → SNSAction.ListEndpointsByPlatformApplication,
      SNSActions.ListPlatformApplications → SNSAction.ListPlatformApplications,
      SNSActions.ListSubscriptions → SNSAction.ListSubscriptions,
      SNSActions.ListSubscriptionsByTopic → SNSAction.ListSubscriptionsByTopic,
      SNSActions.ListTopics → SNSAction.ListTopics,
      SNSActions.Publish → SNSAction.Publish,
      SNSActions.RemovePermission → SNSAction.RemovePermission,
      SNSActions.SetEndpointAttributes → SNSAction.SetEndpointAttributes,
      SNSActions.SetPlatformApplicationAttributes → SNSAction.SetPlatformApplicationAttributes,
      SNSActions.SetSubscriptionAttributes → SNSAction.SetSubscriptionAttributes,
      SNSActions.SetTopicAttributes → SNSAction.SetTopicAttributes,
      SNSActions.Subscribe → SNSAction.Subscribe,
      SNSActions.Unsubscribe → SNSAction.Unsubscribe
    )
}
