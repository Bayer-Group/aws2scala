package com.monsanto.arch.awsutil.auth.policy

/** Used to programmatically build `Statement` objects. */
class StatementBuilder private (sid: Option[String],
                                principals: Option[Set[Principal]],
                                effect: Option[Statement.Effect],
                                actions: Option[Seq[Action]],
                                resources: Option[Seq[Resource]],
                                conditions: Option[Set[Condition]]) {
  /** Returns a new builder with the statement identifier set.  It is an error to invoke this on a builder that
    * already has a statement identifier.
    */
  def withSid(identifier: String): StatementBuilder =
    if (sid.isDefined) {
      throw new IllegalStateException("A statement may only have one identifier.")
    } else {
      new StatementBuilder(Some(identifier), principals, effect, actions, resources, conditions)
    }

  /** Returns a new builder with the principals set.  It is an error to invoke this on a builder that
    * already has principals.
    */
  def withPrincipals(principals: Set[Principal]): StatementBuilder =
    if (this.principals.isDefined) {
      throw new IllegalStateException("A statement may only have one set of principals.")
    } else {
      new StatementBuilder(sid, Some(principals), effect, actions, resources, conditions)
    }

  /** Returns a new builder with the effect set.  It is an error to invoke this on a builder that
    * already has an effect.
    */
  def withEffect(effect: Statement.Effect): StatementBuilder =
    if (this.effect.isDefined) {
      throw new IllegalStateException("A statement may only have one effect.")
    } else {
      new StatementBuilder(sid, principals, Some(effect), actions, resources, conditions)
    }

  /** Returns a new builder with the actions set.  It is an error to invoke this on a builder that
    * already has actions.
    */
  def withActions(actions: Seq[Action]): StatementBuilder =
    if (this.actions.isDefined) {
      throw new IllegalStateException("A statement may only have one list of actions.")
    } else {
      new StatementBuilder(sid, principals, effect, Some(actions), resources, conditions)
    }

  /** Returns a new builder with the resources set.  It is an error to invoke this on a builder that
    * already has resources.
    */
  def withResources(resources: Seq[Resource]): StatementBuilder =
    if (this.resources.isDefined) {
      throw new IllegalStateException("A statement may only have one list of resources.")
    } else {
      new StatementBuilder(sid, principals, effect, actions, Some(resources), conditions)
    }

  /** Returns a new builder with the conditions set.  It is an error to invoke this on a builder that
    * already has conditions.
    */
  def withConditions(conditions: Set[Condition]): StatementBuilder =
    if (this.conditions.isDefined) {
      throw new IllegalStateException("A statement may only have one set of conditions.")
    } else {
      new StatementBuilder(sid, principals, effect, actions, resources, Some(conditions))
    }

  /** Builds a `Statement` instance from the accumulated state. */
  def result: Statement =
    if (effect.isEmpty) {
      throw new IllegalStateException("A statement should have an effect.")
    } else {
      Statement(
        sid,
        principals.getOrElse(Set.empty),
        effect.get,
        actions.getOrElse(Seq.empty),
        resources.getOrElse(Seq.empty),
        conditions.getOrElse(Set.empty))
    }
}

object StatementBuilder {
  /** Handy constant to start building a statement with no accumulated state. */
  val newBuilder: StatementBuilder =
    new StatementBuilder(None, None, None, None, None, None)
}
