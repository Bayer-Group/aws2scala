package com.monsanto.arch.awsutil.auth.policy

/** Allows programmatic construction of `Policy` objects. */
class PolicyBuilder private(version: Option[Policy.Version],
                            id: Option[String],
                            statements: Option[Seq[Statement]]) {
  /** Returns a new builder with the version set.  It is an error to invoke this on a builder that already has a
    * version.
    */
  def withVersion(version: Policy.Version): PolicyBuilder =
    if (this.version.isDefined) {
      throw new IllegalStateException("A policy may only have one version.")
    } else {
      new PolicyBuilder(Some(version),id, statements)
    }

  /** Returns a new builder with the policy identifier set.  It is an error to invoke this on a builder that
    * already has a policy identifier.
    */
  def withId(id: String): PolicyBuilder =
    if (this.id.isDefined) {
      throw new IllegalStateException("A policy may only have one identifier.")
    } else {
      new PolicyBuilder(version, Some(id), statements)
    }

  /** Returns a new builder with the statement list set.  It is an error to invoke this on a builder that
    * already has a statement list or if the statement list is empty.
    */
  def withStatements(statements: Seq[Statement]): PolicyBuilder =
    if (this.statements.isDefined) {
      throw new IllegalStateException("A policy may only have one list of statements.")
    } else if (statements.isEmpty) {
      throw new IllegalArgumentException("The policy’s statement list may not be empty.")
    } else {
      new PolicyBuilder(version, id, Some(statements))
    }

  /** Builds the policy.  Note that the policy must have a policy must have a statement list. */
  def result: Policy =
    if (statements.isEmpty) {
      throw new IllegalStateException("A policy should have a list of statements.")
    } else {
      Policy(
        version,
        id,
        statements.get)
    }
}

object PolicyBuilder {
  /** Handy constant for getting a new builder instance. */
  val newBuilder: PolicyBuilder = new PolicyBuilder(None, None, None)
}
