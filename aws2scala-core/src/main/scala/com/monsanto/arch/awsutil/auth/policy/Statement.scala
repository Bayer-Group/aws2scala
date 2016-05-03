package com.monsanto.arch.awsutil.auth.policy

case class Statement(id: Option[String],
                     principals: Seq[Principal],
                     effect: Statement.Effect,
                     actions: Seq[Action],
                     resources: Seq[Resource],
                     conditions: Seq[Condition])

object Statement {
  /** Enumeration type for statement effects. */
  sealed trait Effect
  object Effect {
    /** Explicitly allow access. */
    case object Allow extends Effect
    /** Explicitly deny access. */
    case object Deny extends Effect

    val values: Seq[Effect] = Seq(Allow,Deny)
  }
}
