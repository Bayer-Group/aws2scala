package com.monsanto.arch.awsutil.auth.policy.action

import com.amazonaws.auth.policy.actions.SNSActions
import com.monsanto.arch.awsutil.auth.policy.Action

/** Type for all AWS access control policy actions for Amazon SNS. */
sealed abstract class SNSAction(_name: String) extends Action(s"sns:${_name}")

object SNSAction {
  /** Represents any action executed on Amazon SNS. */
  case object AllSNSActions extends SNSAction("*")
  /** Action for the AddPermission operation. */
  case object AddPermission extends SNSAction("AddPermission")
  /** Action for the CheckIfPhoneNumberIsOptedOut operation. */
  case object CheckIfPhoneNumberIsOptedOut extends SNSAction("CheckIfPhoneNumberIsOptedOut")
  /** Action for the ConfirmSubscription operation. */
  case object ConfirmSubscription extends SNSAction("ConfirmSubscription")
  /** Action for the CreatePlatformApplication operation. */
  case object CreatePlatformApplication extends SNSAction("CreatePlatformApplication")
  /** Action for the CreatePlatformEndpoint operation. */
  case object CreatePlatformEndpoint extends SNSAction("CreatePlatformEndpoint")
  /** Action for the CreateTopic operation. */
  case object CreateTopic extends SNSAction("CreateTopic")
  /** Action for the DeleteEndpoint operation. */
  case object DeleteEndpoint extends SNSAction("DeleteEndpoint")
  /** Action for the DeletePlatformApplication operation. */
  case object DeletePlatformApplication extends SNSAction("DeletePlatformApplication")
  /** Action for the DeleteTopic operation. */
  case object DeleteTopic extends SNSAction("DeleteTopic")
  /** Action for the GetEndpointAttributes operation. */
  case object GetEndpointAttributes extends SNSAction("GetEndpointAttributes")
  /** Action for the GetPlatformApplicationAttributes operation. */
  case object GetPlatformApplicationAttributes extends SNSAction("GetPlatformApplicationAttributes")
  /** Action for the GetSubscriptionAttributes operation. */
  case object GetSubscriptionAttributes extends SNSAction("GetSubscriptionAttributes")
  /** Action for the GetSMSAttributes operation. */
  case object GetSMSAttributes extends SNSAction("GetSMSAttributes")
  /** Action for the GetTopicAttributes operation. */
  case object GetTopicAttributes extends SNSAction("GetTopicAttributes")
  /** Action for the ListEndpointsByPlatformApplication operation. */
  case object ListEndpointsByPlatformApplication extends SNSAction("ListEndpointsByPlatformApplication")
  /** Action for the ListPhoneNumbersOptedOut operation. */
  case object ListPhoneNumbersOptedOut extends SNSAction("ListPhoneNumbersOptedOut")
  /** Action for the ListPlatformApplications operation. */
  case object ListPlatformApplications extends SNSAction("ListPlatformApplications")
  /** Action for the ListSubscriptions operation. */
  case object ListSubscriptions extends SNSAction("ListSubscriptions")
  /** Action for the ListSubscriptionsByTopic operation. */
  case object ListSubscriptionsByTopic extends SNSAction("ListSubscriptionsByTopic")
  /** Action for the ListTopics operation. */
  case object ListTopics extends SNSAction("ListTopics")
  /** Action for the OptInPhoneNumber operation. */
  case object OptInPhoneNumber extends SNSAction("OptInPhoneNumber")
  /** Action for the Publish operation. */
  case object Publish extends SNSAction("Publish")
  /** Action for the RemovePermission operation. */
  case object RemovePermission extends SNSAction("RemovePermission")
  /** Action for the SetEndpointAttributes operation. */
  case object SetEndpointAttributes extends SNSAction("SetEndpointAttributes")
  /** Action for the SetPlatformApplicationAttributes operation. */
  case object SetPlatformApplicationAttributes extends SNSAction("SetPlatformApplicationAttributes")
  /** Action for the SetSMSAttributes operation. */
  case object SetSMSAttributes extends SNSAction("SetSMSAttributes")
  /** Action for the SetSubscriptionAttributes operation. */
  case object SetSubscriptionAttributes extends SNSAction("SetSubscriptionAttributes")
  /** Action for the SetTopicAttributes operation. */
  case object SetTopicAttributes extends SNSAction("SetTopicAttributes")
  /** Action for the Subscribe operation. */
  case object Subscribe extends SNSAction("Subscribe")
  /** Action for the Unsubscribe operation. */
  case object Unsubscribe extends SNSAction("Unsubscribe")

  val values: Seq[SNSAction] = Seq(
    AllSNSActions, AddPermission, CheckIfPhoneNumberIsOptedOut, ConfirmSubscription, CreatePlatformApplication,
    CreatePlatformEndpoint, CreateTopic, DeleteEndpoint, DeletePlatformApplication, DeleteTopic, GetEndpointAttributes,
    GetPlatformApplicationAttributes, GetSMSAttributes, GetSubscriptionAttributes, GetTopicAttributes,
    ListEndpointsByPlatformApplication, ListPhoneNumbersOptedOut, ListPlatformApplications, ListSubscriptions,
    ListSubscriptionsByTopic, ListTopics, OptInPhoneNumber, Publish, RemovePermission, SetEndpointAttributes,
    SetPlatformApplicationAttributes, SetSMSAttributes, SetSubscriptionAttributes, SetTopicAttributes, Subscribe,
    Unsubscribe
  )

  private[awsutil] def registerActions(): Unit =
    Action.registerActions(
      SNSActions.AllSNSActions → SNSAction.AllSNSActions,
      SNSActions.AddPermission → SNSAction.AddPermission,
      SNSActions.CheckIfPhoneNumberIsOptedOut -> SNSAction.CheckIfPhoneNumberIsOptedOut,
      SNSActions.ConfirmSubscription → SNSAction.ConfirmSubscription,
      SNSActions.CreatePlatformApplication → SNSAction.CreatePlatformApplication,
      SNSActions.CreatePlatformEndpoint → SNSAction.CreatePlatformEndpoint,
      SNSActions.CreateTopic → SNSAction.CreateTopic,
      SNSActions.DeleteEndpoint → SNSAction.DeleteEndpoint,
      SNSActions.DeletePlatformApplication → SNSAction.DeletePlatformApplication,
      SNSActions.DeleteTopic → SNSAction.DeleteTopic,
      SNSActions.GetEndpointAttributes → SNSAction.GetEndpointAttributes,
      SNSActions.GetPlatformApplicationAttributes → SNSAction.GetPlatformApplicationAttributes,
      SNSActions.GetSMSAttributes → SNSAction.GetSMSAttributes,
      SNSActions.GetSubscriptionAttributes → SNSAction.GetSubscriptionAttributes,
      SNSActions.GetTopicAttributes → SNSAction.GetTopicAttributes,
      SNSActions.ListEndpointsByPlatformApplication → SNSAction.ListEndpointsByPlatformApplication,
      SNSActions.ListPhoneNumbersOptedOut → SNSAction.ListPhoneNumbersOptedOut,
      SNSActions.ListPlatformApplications → SNSAction.ListPlatformApplications,
      SNSActions.ListSubscriptions → SNSAction.ListSubscriptions,
      SNSActions.ListSubscriptionsByTopic → SNSAction.ListSubscriptionsByTopic,
      SNSActions.ListTopics → SNSAction.ListTopics,
      SNSActions.OptInPhoneNumber → SNSAction.OptInPhoneNumber,
      SNSActions.Publish → SNSAction.Publish,
      SNSActions.RemovePermission → SNSAction.RemovePermission,
      SNSActions.SetEndpointAttributes → SNSAction.SetEndpointAttributes,
      SNSActions.SetPlatformApplicationAttributes → SNSAction.SetPlatformApplicationAttributes,
      SNSActions.SetSMSAttributes → SNSAction.SetSMSAttributes,
      SNSActions.SetSubscriptionAttributes → SNSAction.SetSubscriptionAttributes,
      SNSActions.SetTopicAttributes → SNSAction.SetTopicAttributes,
      SNSActions.Subscribe → SNSAction.Subscribe,
      SNSActions.Unsubscribe → SNSAction.Unsubscribe
    )
}
