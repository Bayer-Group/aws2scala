package com.monsanto.arch.awsutil.identitymanagement.model

/** Configures a request for listing managed policies.
  *
  * @param onlyAttached a flag to filter the results to only the attached policies
  * @param prefix the path prefix for filtering the results
  * @param scope the scope for filtering the results
  */
case class ListPoliciesRequest(onlyAttached: Boolean,
                               prefix: Path,
                               scope: ListPoliciesRequest.Scope)

object ListPoliciesRequest {
  /** Constant for a request that will list all possible policies. */
  val allPolicies: ListPoliciesRequest = ListPoliciesRequest(onlyAttached = false, Path.empty, Scope.All)

  /** Returns a request that will list all policies with the given prefix. */
  def withPrefix(prefix: Path): ListPoliciesRequest = ListPoliciesRequest(onlyAttached = false, prefix, Scope.All)

  /** Constant for a request that will list only customer-managed policies. */
  val localPolicies: ListPoliciesRequest = ListPoliciesRequest(onlyAttached = false, Path.empty, Scope.Local)

  sealed abstract class Scope(val name: String)
  object Scope {
    /** Lists all managed policies. */
    case object All extends Scope("All")
    /** Lists only AWS-managed policies. */
    case object AWS extends Scope("AWS")
    /** Lists only customer-managed policies. */
    case object Local extends Scope("Local")

    /** All possible scope values. */
    val values: Seq[Scope] = Seq(All, AWS, Local)

    object fromName {
      /** Given a name, returns the corresponding `Scope`.
        *
        * @throws java.lang.IllegalArgumentException if the name does not correspond to a scope
        */
      def apply(name: String): Scope =
        unapply(name)
          .getOrElse(throw new IllegalArgumentException(s"‘$name’ is not a valid scope name."))

      /** Extracts a `Scope` from a name string. */
      def unapply(name: String): Option[Scope] = values.find(_.name == name)
    }
  }
}
