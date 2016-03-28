package com.monsanto.arch.awsutil.cloudformation.model

/** A request to delete the specified tack.
  *
  * @param stackNameOrId the name or the unique stack ID that is associated with the stack
  * @param retainResources For stacks in the `DELETE_FAILED` state, a list of resource logical IDs that are associated
  *                        with the resources you want to retain.  During deletion, AWS CloudFormation deletes the
  *                        stack but does not delete the retained resources.
  */
case class DeleteStackRequest(stackNameOrId: String, retainResources: Seq[String])
