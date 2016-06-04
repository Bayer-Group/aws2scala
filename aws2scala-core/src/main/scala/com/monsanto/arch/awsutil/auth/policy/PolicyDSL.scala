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
      .addComponent(statements)(PolicyComponent.statementsComponent)
      .result
      .addDefaultVersion

  /** Allows construction of a policy with two components, at least one of which must be a statement list. */
  def policy[T: PolicyComponent, U: PolicyComponent](t: T, u: U): Policy =
    PolicyBuilder.newBuilder
      .addComponent(t)
      .addComponent(u)
      .result
      .addDefaultVersion

  /** Allows construction of a policy with a statement list, a version, and an identifier. */
  def policy[T: PolicyComponent, U: PolicyComponent, V: PolicyComponent](t: T, u: U, v: V): Policy =
    PolicyBuilder.newBuilder
      .addComponent(t)
      .addComponent(u)
      .addComponent(v)
      .result
      .addDefaultVersion

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
  def allow[T: StatementComponent](t: T): Statement =
    StatementBuilder.newBuilder
      .withEffect(Statement.Effect.Allow)
      .addComponent(t)
      .result

  /** Creates a statement with the effect `Allow` and the two given components.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def allow[T: StatementComponent, U: StatementComponent](t: T, u: U): Statement =
    StatementBuilder.newBuilder
      .withEffect(Statement.Effect.Allow)
      .addComponent(t)
      .addComponent(u)
      .result

  /** Creates a statement with the effect `Allow` and the three given components.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def allow[T: StatementComponent, U: StatementComponent, V: StatementComponent](t: T, u: U, v: V): Statement =
    StatementBuilder.newBuilder
      .withEffect(Statement.Effect.Allow)
      .addComponent(t)
      .addComponent(u)
      .addComponent(v)
      .result

  /** Creates a statement with the effect `Allow` and the four given components.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def allow[T: StatementComponent, U: StatementComponent, V: StatementComponent, W: StatementComponent](t: T, u: U, v: V, w: W): Statement =
    StatementBuilder.newBuilder
      .withEffect(Statement.Effect.Allow)
      .addComponent(t)
      .addComponent(u)
      .addComponent(v)
      .addComponent(w)
      .result

  /** Creates a statement with the effect `Allow` and all of the five component types.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def allow[T: StatementComponent, U: StatementComponent, V: StatementComponent, W: StatementComponent, X: StatementComponent](t: T, u: U, v: V, w: W, x: X): Statement =
    StatementBuilder.newBuilder
      .withEffect(Statement.Effect.Allow)
      .addComponent(t)
      .addComponent(u)
      .addComponent(v)
      .addComponent(w)
      .addComponent(x)
      .result

  /** Creates a statement with the effect `Deny` and the given component.  A statement component may be a statement
    * identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def deny[T: StatementComponent](t: T): Statement =
    StatementBuilder.newBuilder
      .withEffect(Statement.Effect.Deny)
      .addComponent(t)
      .result

  /** Creates a statement with the effect `Allow` and the two given components.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def deny[T: StatementComponent, U: StatementComponent](t: T, u: U): Statement =
    StatementBuilder.newBuilder
      .withEffect(Statement.Effect.Deny)
      .addComponent(t)
      .addComponent(u)
      .result

  /** Creates a statement with the effect `Allow` and the three given components.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def deny[T: StatementComponent, U: StatementComponent, V: StatementComponent](t: T, u: U, v: V): Statement =
    StatementBuilder.newBuilder
      .withEffect(Statement.Effect.Deny)
      .addComponent(t)
      .addComponent(u)
      .addComponent(v)
      .result

  /** Creates a statement with the effect `Allow` and the four given components.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def deny[T: StatementComponent, U: StatementComponent, V: StatementComponent, W: StatementComponent](t: T, u: U, v: V, w: W): Statement =
    StatementBuilder.newBuilder
      .withEffect(Statement.Effect.Deny)
      .addComponent(t)
      .addComponent(u)
      .addComponent(v)
      .addComponent(w)
      .result

  /** Creates a statement with the effect `Deny` and all of the five component types.  A statement component may be a
    * statement identifier (Sid), principal list, action list, resource list, or a condition list.
    */
  def deny[T: StatementComponent, U: StatementComponent, V: StatementComponent, W: StatementComponent, X: StatementComponent](t: T, u: U, v: V, w: W, x: X): Statement =
    StatementBuilder.newBuilder
      .withEffect(Statement.Effect.Deny)
      .addComponent(t)
      .addComponent(u)
      .addComponent(v)
      .addComponent(w)
      .addComponent(x)
      .result

  /** Used within an `allow` or `deny` to set the principals in a statement. */
  def principals(principals: Principal*): Set[Principal] = principals.toSet

  /** Used within an `allow` or `deny` to set the actions in a statement. */
  def actions(actions: Action*): Seq[Action] = actions

  /** Used within an `allow` or `deny` to set the resources in a statement. */
  def resources(resources: Resource*): Seq[Resource] = resources

  /** Used within an `allow` or `deny` to set the conditions in a statement. */
  def conditions(conditions: Condition*): Set[Condition] = conditions.toSet

  private implicit class PolicyBuilderComponentSupport(val builder: PolicyBuilder) extends AnyVal {
    def addComponent[T: PolicyComponent](component: T): PolicyBuilder =
      implicitly[PolicyComponent[T]].addTo(builder, component)
  }

  /** Type class for setting a value on the policy builder. */
  trait PolicyComponent[T] {
    def addTo(builder: PolicyBuilder, component: T): PolicyBuilder
  }

  object PolicyComponent {
    /** Helper for setting a statement list on a policy builder. */
    implicit val statementsComponent: PolicyComponent[Seq[Statement]] =
      new PolicyComponent[Seq[Statement]] {
        override def addTo(builder: PolicyBuilder, statements: Seq[Statement]): PolicyBuilder =
          builder.withStatements(statements)
      }

    /** Helper for setting a policy identifier on a policy builder. */
    implicit val idComponent: PolicyComponent[Identifier] =
      new PolicyComponent[Identifier] {
        override def addTo(builder: PolicyBuilder, id: Identifier): PolicyBuilder = builder.withId(id.value)
      }

    /** Helper for setting a version on a policy builder. */
    implicit val versionComponent: PolicyComponent[Policy.Version] =
      new PolicyComponent[Policy.Version] {
        override def addTo(builder: PolicyBuilder, policyVersion: Policy.Version): PolicyBuilder =
          builder.withVersion(policyVersion)
      }
  }

  private implicit class StatementBuilderComponentSupport(val builder: StatementBuilder) extends AnyVal {
    /** Adds a component to the statement builder. */
    def addComponent[T: StatementComponent](component: T): StatementBuilder =
      implicitly[StatementComponent[T]].addTo(builder, component)
  }

  /** Type class for adding state to the statement builder. */
  trait StatementComponent[T] {
    def addTo(builder: StatementBuilder, component: T): StatementBuilder
  }

  object StatementComponent {
    /** Helper for setting a statement identifier on a statement builder. */
    implicit val idComponent: StatementComponent[Identifier] = new StatementComponent[Identifier] {
      override def addTo(builder: StatementBuilder, id: Identifier): StatementBuilder = builder.withSid(id.value)
    }

    /** Helper for setting a principal list on a statement builder. */
    implicit val principalsComponent: StatementComponent[Set[Principal]] =
      new StatementComponent[Set[Principal]] {
        override def addTo(builder: StatementBuilder, principals: Set[Principal]): StatementBuilder =
          builder.withPrincipals(principals)
      }

    /** Helper for setting an action list on a statement builder. */
    implicit val actionsComponent: StatementComponent[Seq[Action]] =
      new StatementComponent[Seq[Action]] {
        override def addTo(builder: StatementBuilder, actions: Seq[Action]): StatementBuilder =
          builder.withActions(actions)
      }

    /** Helper for setting a resource list on a statement builder. */
    implicit val resourcesComponent: StatementComponent[Seq[Resource]] =
      new StatementComponent[Seq[Resource]] {
        override def addTo(builder: StatementBuilder, resources: Seq[Resource]): StatementBuilder =
          builder.withResources(resources)
      }

    /** Helper for setting a condition list on a statement builder. */
    implicit val conditionsComponent: StatementComponent[Set[Condition]] =
      new StatementComponent[Set[Condition]] {
        override def addTo(builder: StatementBuilder, conditions: Set[Condition]): StatementBuilder =
          builder.withConditions(conditions)
      }
  }

  /** Simple type-safe wrapper for a statement or policy identifier. */
  class Identifier private[policy] (private[policy] val value: String)

  private implicit class PolicyWithDefaults(val policy: Policy) extends AnyVal {
    def addDefaultVersion: Policy =
      if (policy.version.isEmpty) {
        policy.copy(version = Some(Policy.Version.`2012-10-17`))
      } else {
        policy
      }
  }
}
