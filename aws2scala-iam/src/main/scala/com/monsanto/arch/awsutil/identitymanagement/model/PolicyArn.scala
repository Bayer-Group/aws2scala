package com.monsanto.arch.awsutil.identitymanagement.model

import com.monsanto.arch.awsutil.{Account, Arn}

/** Represents the ARN of of an IAM policy.
  *
  * @param account the account to which the policy belongs
  * @param name the name of the policy
  * @param path the path of the policy
  */
case class PolicyArn(account: Account, name: String, path: Path = Path.empty) extends Arn(Arn.Namespace.IAM, None, account) {
  override val resource = s"policy${path.pathString}$name"
}

object PolicyArn {
  /** Builds a policy ARN object from the given ARN string. */
  def apply(arnString: String): PolicyArn = {
    arnString match {
      case Arn(policyArn: PolicyArn) ⇒ policyArn
      case _ ⇒ throw new IllegalArgumentException(s"’$arnString‘ is not a valid policy ARN")
    }
  }

  private[identitymanagement] val policyArnPF: PartialFunction[Arn.ArnParts, PolicyArn] = {
    case (_, Arn.Namespace.IAM, None, Some(account), PolicyResourceRegex(Path.fromPathString(path), name)) ⇒
      PolicyArn(account, name, path)
  }

  private val PolicyResourceRegex = "^policy(/|/.*/)([^/]+)$".r
}
