package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.SNSActions
import com.monsanto.arch.awsutil.auth.policy.Action
import com.monsanto.arch.awsutil.util.{AwsEnumeration, AwsEnumerationCompanion}

/** Type for all AWS access control policy actions for Amazon SNS. */
sealed abstract class SNSAction(val toAws: SNSActions) extends Action with AwsEnumeration[SNSActions]

object SNSAction extends AwsEnumerationCompanion[SNSAction,SNSActions] {
  /** Represents any action executed on Amazon SNS. */
  case object AllSNSActions extends SNSAction(SNSActions.AllSNSActions)
  /** Action for the AddPermission operation. */
  case object AddPermission extends SNSAction(SNSActions.AddPermission)
  /** Action for the ConfirmSubscription operation. */
  case object ConfirmSubscription extends SNSAction(SNSActions.ConfirmSubscription)
  /** Action for the CreatePlatformApplication operation. */
  case object CreatePlatformApplication extends SNSAction(SNSActions.CreatePlatformApplication)
  /** Action for the CreatePlatformEndpoint operation. */
  case object CreatePlatformEndpoint extends SNSAction(SNSActions.CreatePlatformEndpoint)
  /** Action for the CreateTopic operation. */
  case object CreateTopic extends SNSAction(SNSActions.CreateTopic)
  /** Action for the DeleteEndpoint operation. */
  case object DeleteEndpoint extends SNSAction(SNSActions.DeleteEndpoint)
  /** Action for the DeletePlatformApplication operation. */
  case object DeletePlatformApplication extends SNSAction(SNSActions.DeletePlatformApplication)
  /** Action for the DeleteTopic operation. */
  case object DeleteTopic extends SNSAction(SNSActions.DeleteTopic)
  /** Action for the GetEndpointAttributes operation. */
  case object GetEndpointAttributes extends SNSAction(SNSActions.GetEndpointAttributes)
  /** Action for the GetPlatformApplicationAttributes operation. */
  case object GetPlatformApplicationAttributes extends SNSAction(SNSActions.GetPlatformApplicationAttributes)
  /** Action for the GetSubscriptionAttributes operation. */
  case object GetSubscriptionAttributes extends SNSAction(SNSActions.GetSubscriptionAttributes)
  /** Action for the GetTopicAttributes operation. */
  case object GetTopicAttributes extends SNSAction(SNSActions.GetTopicAttributes)
  /** Action for the ListEndpointsByPlatformApplication operation. */
  case object ListEndpointsByPlatformApplication extends SNSAction(SNSActions.ListEndpointsByPlatformApplication)
  /** Action for the ListPlatformApplications operation. */
  case object ListPlatformApplications extends SNSAction(SNSActions.ListPlatformApplications)
  /** Action for the ListSubscriptions operation. */
  case object ListSubscriptions extends SNSAction(SNSActions.ListSubscriptions)
  /** Action for the ListSubscriptionsByTopic operation. */
  case object ListSubscriptionsByTopic extends SNSAction(SNSActions.ListSubscriptionsByTopic)
  /** Action for the ListTopics operation. */
  case object ListTopics extends SNSAction(SNSActions.ListTopics)
  /** Action for the Publish operation. */
  case object Publish extends SNSAction(SNSActions.Publish)
  /** Action for the RemovePermission operation. */
  case object RemovePermission extends SNSAction(SNSActions.RemovePermission)
  /** Action for the SetEndpointAttributes operation. */
  case object SetEndpointAttributes extends SNSAction(SNSActions.SetEndpointAttributes)
  /** Action for the SetPlatformApplicationAttributes operation. */
  case object SetPlatformApplicationAttributes extends SNSAction(SNSActions.SetPlatformApplicationAttributes)
  /** Action for the SetSubscriptionAttributes operation. */
  case object SetSubscriptionAttributes extends SNSAction(SNSActions.SetSubscriptionAttributes)
  /** Action for the SetTopicAttributes operation. */
  case object SetTopicAttributes extends SNSAction(SNSActions.SetTopicAttributes)
  /** Action for the Subscribe operation. */
  case object Subscribe extends SNSAction(SNSActions.Subscribe)
  /** Action for the Unsubscribe operation. */
  case object Unsubscribe extends SNSAction(SNSActions.Unsubscribe)

  override val values: Seq[SNSAction] = Seq(
    AllSNSActions, AddPermission, ConfirmSubscription, CreatePlatformApplication, CreatePlatformEndpoint, CreateTopic,
    DeleteEndpoint, DeletePlatformApplication, DeleteTopic, GetEndpointAttributes, GetPlatformApplicationAttributes,
    GetSubscriptionAttributes, GetTopicAttributes, ListEndpointsByPlatformApplication, ListPlatformApplications,
    ListSubscriptions, ListSubscriptionsByTopic, ListTopics, Publish, RemovePermission, SetEndpointAttributes,
    SetPlatformApplicationAttributes, SetSubscriptionAttributes, SetTopicAttributes, Subscribe, Unsubscribe
  )
}
