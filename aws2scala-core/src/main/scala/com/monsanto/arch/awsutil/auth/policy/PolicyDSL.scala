package com.monsanto.arch.awsutil.auth.policy

/** Provides a DSL for building AWS IAM policies in code.
  *
  * ==Creating a policy==
  *
  * A policy is composed of:
  *
  *  1. A [[PolicyDSL.version version]], which defaults to [[Policy.Version.2012-10-17 2012-10-17]]
  *  1. An optional identifier ([[PolicyDSL.id id]])
  *  1. A non-empty list of [[PolicyDSL.statements(head* statements]]
  *
  * '''Example'''
  * {{{
  *   import PolicyDSL._
  *
  *   // Produces the equivalent of Policy(Policy.Version.`2012-10-17`, Some("my-policy"), /* statements value */ )
  *   policy (
  *     id("my-policy"),
  *     statements(
  *       // add statements here…
  *     )
  *   )
  * }}}
  *
  * Note that the DSL can accept a `Seq[Statement]` value:
  *
  * {{{
  *   val someStatements: Seq[Statement] = // Some value
  *
  *   // Using case classes
  *   val policy1 = Policy(Policy.Version.`2012-10-17`, None, someStatements)
  *
  *   // Using the DSL
  *   val policy2 = policy(someStatements)
  *
  *   // both approaches generate equivalent values
  *   assert(policy1 == policy2)
  * }}}
  *
  * Also note that components may be applied in any order, but may not be duplicated.
  *
  *
  * ==Creating a statement==
  *
  * A statement is composed of:
  *
  *  1. An optional identifier ([[PolicyDSL.id id]])
  *  1. A list of zero or more [[PolicyDSL.principals principals]]
  *  1. An effect, either [[Statement.Effect.Allow allow]] or [[Statement.Effect.Deny deny]]
  *  1. A list of zero or more [[PolicyDSL.actions actions]]
  *  1. A list of zero or more [[PolicyDSL.resources resources]]
  *  1. A list of zero or more [[PolicyDSL.conditions conditions]]
  *
  * To use the DSL, you will need to decide whether to [[PolicyDSL.allow[T](t* allow]] or [[PolicyDSL.deny[T](* deny]]
  * access and provide the necessary details.  The following is an example of creating a policy that will allow the
  * account 11122233344 to assume a role.
  *
  * {{{
  *   import PolicyDSL._
  *   import Principal._
  *   import STSAction._
  *
  *   policy(
  *     statements(
  *       allow(
  *         principals(
  *           account(Account("111222333444")
  *         ),
  *         actions(AssumeRole)
  *       )
  *     )
  *   )
  * }}}
  *
  * As with building policies, you can use `Seq[Principal]`, `Seq[Action]`, `Seq[Resource]`, and `Seq[Condition]`
  * objects directly within the DSL.  Also, components may be applied in any order, but may not be duplicated.
  */
object PolicyDSL {
  /** Allows construction of a policy using only a list of statements. */
  def policy(statements: Seq[Statement]): Policy =
    PolicyBuilder.newBuilder
      .addComponent(statements)(PolicyBuilder.Component.statementsComponent)
      .build()

  /** Allows construction of a policy with two components, at least one of which must be a statement list. */
  def policy[T: PolicyBuilder.Component, U: PolicyBuilder.Component](t: T, u: U): Policy =
    PolicyBuilder.newBuilder
      .addComponent(t)
      .addComponent(u)
      .build()

  /** Allows construction of a policy with a statement list, a version, and an identifier. */
  def policy[T: PolicyBuilder.Component, U: PolicyBuilder.Component, V: PolicyBuilder.Component](t: T, u: U, v: V): Policy =
    PolicyBuilder.newBuilder
      .addComponent(t)
      .addComponent(u)
      .addComponent(v)
      .build()

  /** Used as an argument to `policy`, `allow`, and `deny` to set a policy’s or statement’s identifier. */
  def id(str: String): Identifier = new Identifier(str)

  /** Used as an argument to `policy` to set the policy’s version. */
  def version(policyVersion: Policy.Version): Policy.Version = policyVersion

  /** Used as an argument to `policy` to create a statement list with a single statement. */
  def statements(statement: Statement): Seq[Statement] = Seq(statement)

  /** Used as an argument to `policy` to create a statement list with more than one statement. */
  def statements(head: Statement, tail: Statement*): Seq[Statement] = head +: tail

  /** Creates a statement with the effect `Allow` and the given component.  A statement component may be a statement
    * identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def allow[T: StatementBuilder.Component](t: T): Statement =
    StatementBuilder.allow
      .addComponent(t)
      .build()

  /** Creates a statement with the effect `Allow` and the two given components.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def allow[T: StatementBuilder.Component, U: StatementBuilder.Component](t: T, u: U): Statement =
    StatementBuilder.allow
      .addComponent(t)
      .addComponent(u)
      .build()

  /** Creates a statement with the effect `Allow` and the three given components.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def allow[T: StatementBuilder.Component, U: StatementBuilder.Component, V: StatementBuilder.Component](t: T, u: U, v: V): Statement =
    StatementBuilder.allow
      .addComponent(t)
      .addComponent(u)
      .addComponent(v)
      .build()

  /** Creates a statement with the effect `Allow` and the four given components.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def allow[T: StatementBuilder.Component, U: StatementBuilder.Component, V: StatementBuilder.Component, W: StatementBuilder.Component](t: T, u: U, v: V, w: W): Statement =
    StatementBuilder.allow
      .addComponent(t)
      .addComponent(u)
      .addComponent(v)
      .addComponent(w)
      .build()

  /** Creates a statement with the effect `Allow` and all of the five component types.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def allow[T: StatementBuilder.Component, U: StatementBuilder.Component, V: StatementBuilder.Component, W: StatementBuilder.Component, X: StatementBuilder.Component](t: T, u: U, v: V, w: W, x: X): Statement =
    StatementBuilder.allow
      .addComponent(t)
      .addComponent(u)
      .addComponent(v)
      .addComponent(w)
      .addComponent(x)
      .build()

  /** Creates a statement with the effect `Deny` and the given component.  A statement component may be a statement
    * identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def deny[T: StatementBuilder.Component](t: T): Statement =
    StatementBuilder.deny
      .addComponent(t)
      .build()

  /** Creates a statement with the effect `Allow` and the two given components.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def deny[T: StatementBuilder.Component, U: StatementBuilder.Component](t: T, u: U): Statement =
    StatementBuilder.deny
      .addComponent(t)
      .addComponent(u)
      .build()

  /** Creates a statement with the effect `Allow` and the three given components.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def deny[T: StatementBuilder.Component, U: StatementBuilder.Component, V: StatementBuilder.Component](t: T, u: U, v: V): Statement =
    StatementBuilder.deny
      .addComponent(t)
      .addComponent(u)
      .addComponent(v)
      .build()

  /** Creates a statement with the effect `Allow` and the four given components.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def deny[T: StatementBuilder.Component, U: StatementBuilder.Component, V: StatementBuilder.Component, W: StatementBuilder.Component](t: T, u: U, v: V, w: W): Statement =
    StatementBuilder.deny
      .addComponent(t)
      .addComponent(u)
      .addComponent(v)
      .addComponent(w)
      .build()

  /** Creates a statement with the effect `Deny` and all of the five component types.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def deny[T: StatementBuilder.Component, U: StatementBuilder.Component, V: StatementBuilder.Component, W: StatementBuilder.Component, X: StatementBuilder.Component](t: T, u: U, v: V, w: W, x: X): Statement =
    StatementBuilder.deny
      .addComponent(t)
      .addComponent(u)
      .addComponent(v)
      .addComponent(w)
      .addComponent(x)
      .build()

  /** Used within an `allow` or `deny` to set the principals in a statement. */
  def principals(principals: Principal*): Seq[Principal] = principals

  /** Used within an `allow` or `deny` to set the actions in a statement. */
  def actions(actions: Action*): Seq[Action] = actions

  /** Used within an `allow` or `deny` to set the resources in a statement. */
  def resources(resources: Resource*): Seq[Resource] = resources

  /** Used within an `allow` or `deny` to set the conditions in a statement. */
  def conditions(conditions: Condition*): Seq[Condition] = conditions

  /** Used by the DSL to build policy objects. */
  class PolicyBuilder private(version: Option[Policy.Version],
                              id: Option[String],
                              statements: Option[Seq[Statement]]) {
    /** Adds a component to the policy. */
    private[policy] def addComponent[T: PolicyBuilder.Component](component: T): PolicyBuilder =
      implicitly[PolicyBuilder.Component[T]].applyTo(this, component)

    /** Builds the policy.  Note that the policy must have a policy must have a statement list. */
    private[policy] def build(): Policy = {
      require(statements.isDefined, "A policy should have a list of statements.")
      Policy(
        version.getOrElse(Policy.Version.`2012-10-17`),
        id,
        statements.get)
    }

    /** Returns a new builder with the version set.  It is an error to invoke this on a builder that already has a
      * version.
      */
    private def withVersion(version: Policy.Version): PolicyBuilder = {
      if (this.version.isDefined) {
        throw new IllegalArgumentException("A policy may only have one version.")
      }
      new PolicyBuilder(Some(version),id, statements)
    }

    /** Returns a new builder with the policy identifier set.  It is an error to invoke this on a builder that
      * already has a policy identifier.
      */
    private def withId(id: String): PolicyBuilder = {
      if (this.id.isDefined) {
        throw new IllegalArgumentException("A policy may only have one identifier.")
      }
      new PolicyBuilder(version, Some(id), statements)
    }

    /** Returns a new builder with the statement list set.  It is an error to invoke this on a builder that
      * already has a statement list or if the statement list is empty.
      */
    private def withStatements(statements: Seq[Statement]): PolicyBuilder = {
      if (this.statements.isDefined) {
        throw new IllegalArgumentException("A policy may only have one list of statements.")
      }
      if (statements.isEmpty) {
        throw new IllegalArgumentException("The policy’s statement list may not be empty.")
      }
      new PolicyBuilder(version, id, Some(statements))
    }
  }

  object PolicyBuilder {
    /** Handy constant for getting a new builder instance. */
    private[policy] val newBuilder: PolicyBuilder = new PolicyBuilder(None, None, None)

    /** Type class for setting a value on the policy builder. */
    trait Component[T] {
      def applyTo(builder: PolicyBuilder, component: T): PolicyBuilder
    }

    object Component {
      /** Helper for setting a statement list on a policy builder. */
      implicit val statementsComponent: Component[Seq[Statement]] =
        new Component[Seq[Statement]] {
          override def applyTo(builder: PolicyBuilder, statements: Seq[Statement]): PolicyBuilder =
            builder.withStatements(statements)
        }

      /** Helper for setting a policy identifier on a policy builder. */
      implicit val idComponent: Component[Identifier] =
        new Component[Identifier] {
          override def applyTo(builder: PolicyBuilder, id: Identifier): PolicyBuilder = builder.withId(id.value)
        }

      /** Helper for setting a version on a policy builder. */
      implicit val versionComponent: Component[Policy.Version] =
        new Component[Policy.Version] {
          override def applyTo(builder: PolicyBuilder, policyVersion: Policy.Version): PolicyBuilder =
            builder.withVersion(policyVersion)
        }
    }

    /** Type-safe wrapper for a policy identifier. */
    class Id private[policy] (private[policy] val value: String)
  }

  /** Used by the DSL to build `Statement` objects. */
  class StatementBuilder private (sid: Option[String],
                                  principals: Option[Seq[Principal]],
                                  effect: Statement.Effect,
                                  actions: Option[Seq[Action]],
                                  resources: Option[Seq[Resource]],
                                  conditions: Option[Seq[Condition]]) {
    /** Adds a component to the statement builder. */
    private[policy] def addComponent[T: StatementBuilder.Component](component: T): StatementBuilder =
      implicitly[StatementBuilder.Component[T]].addTo(this, component)

    /** Returns a new builder with the statement identifier set.  It is an error to invoke this on a builder that
      * already has a statement identifier.
      */
    private def withSid(identifier: String): StatementBuilder = {
      if (sid.isDefined) {
        throw new IllegalArgumentException("A statement may only have one identifier.")
      }
      new StatementBuilder(Some(identifier), principals, effect, actions, resources, conditions)
    }

    /** Returns a new builder with the principals set.  It is an error to invoke this on a builder that
      * already has principals.
      */
    private def withPrincipals(principals: Seq[Principal]): StatementBuilder = {
      if (this.principals.isDefined) {
        throw new IllegalArgumentException("A statement may only have one principal list.")
      }
      new StatementBuilder(sid, Some(principals), effect, actions, resources, conditions)
    }

    /** Returns a new builder with the actions set.  It is an error to invoke this on a builder that
      * already has actions.
      */
    private def withActions(actions: Seq[Action]): StatementBuilder = {
      if (this.actions.isDefined) {
        throw new IllegalArgumentException("A statement may only have one action list.")
      }
      new StatementBuilder(sid, principals, effect, Some(actions), resources, conditions)
    }

    /** Returns a new builder with the resources set.  It is an error to invoke this on a builder that
      * already has resources.
      */
    private def withResources(resources: Seq[Resource]): StatementBuilder = {
      if (this.resources.isDefined) {
        throw new IllegalArgumentException("A statement may only have one resource list.")
      }
      new StatementBuilder(sid, principals, effect, actions, Some(resources), conditions)
    }

    /** Returns a new builder with the conditions set.  It is an error to invoke this on a builder that
      * already has conditions.
      */
    private def withConditions(conditions: Seq[Condition]): StatementBuilder = {
      if (this.conditions.isDefined) {
        throw new IllegalArgumentException("A statement may only have one condition list.")
      }
      new StatementBuilder(sid, principals, effect, actions, resources, Some(conditions))
    }

    /** Builds a `Statement` instance from the accumulated state. */
    private[policy] def build(): Statement =
      Statement(
        sid,
        principals.getOrElse(Seq.empty),
        effect,
        actions.getOrElse(Seq.empty),
        resources.getOrElse(Seq.empty),
        conditions.getOrElse(Seq.empty))
  }

  object StatementBuilder {
    /** Handy constant to start building a statement with an `Allow` effect. */
    private[policy] val allow: StatementBuilder =
      new StatementBuilder(None, None, Statement.Effect.Allow, None, None, None)

    /** Handy constant to start building a statement with an `Deny` effect. */
    private[policy] val deny: StatementBuilder =
      new StatementBuilder(None, None, Statement.Effect.Deny, None, None, None)

    /** Type class for adding state to the statement builder. */
    trait Component[T] {
      def addTo(builder: StatementBuilder, component: T): StatementBuilder
    }

    object Component {
      /** Helper for setting a statement identifier on a statement builder. */
      implicit val idComponent: Component[Identifier] = new Component[Identifier] {
        override def addTo(builder: StatementBuilder, id: Identifier): StatementBuilder = builder.withSid(id.value)
      }

      /** Helper for setting a principal list on a statement builder. */
      implicit val principalsComponent: Component[Seq[Principal]] =
        new Component[Seq[Principal]] {
          override def addTo(builder: StatementBuilder, principals: Seq[Principal]): StatementBuilder =
            builder.withPrincipals(principals)
        }

      /** Helper for setting an action list on a statement builder. */
      implicit val actionsComponent: Component[Seq[Action]] =
        new Component[Seq[Action]] {
          override def addTo(builder: StatementBuilder, actions: Seq[Action]): StatementBuilder =
            builder.withActions(actions)
        }

      /** Helper for setting a resource list on a statement builder. */
      implicit val resourcesComponent: Component[Seq[Resource]] =
        new Component[Seq[Resource]] {
          override def addTo(builder: StatementBuilder, resources: Seq[Resource]): StatementBuilder =
            builder.withResources(resources)
        }

      /** Helper for setting a condition list on a statement builder. */
      implicit val conditionsComponent: Component[Seq[Condition]] =
        new Component[Seq[Condition]] {
          override def addTo(builder: StatementBuilder, conditions: Seq[Condition]): StatementBuilder =
            builder.withConditions(conditions)
        }
    }
  }

  /** Simple type-safe wrapper for a statement or policy identifier. */
  class Identifier private[policy] (private[policy] val value: String)
}
