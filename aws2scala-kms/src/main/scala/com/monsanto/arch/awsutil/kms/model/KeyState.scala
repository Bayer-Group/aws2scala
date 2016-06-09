package com.monsanto.arch.awsutil.kms.model

/** Parent class for all key state types.
  *
  * @param name the unique name for the key state
  */
sealed abstract class KeyState(val name: String)

object KeyState {
  /** Indicates that the key is enabled. */
  case object Enabled extends KeyState("Enabled")

  /** Indicates that the key is disabled. */
  case object Disabled extends KeyState("Disabled")

  /** Indicates that the key is pending deletion. */
  case object PendingDeletion extends KeyState("PendingDeletion")

  /** All possible key state values. */
  val values: Seq[KeyState] = Seq(Enabled, Disabled, PendingDeletion)

  /** Utility for building/extracting a `KeyUsage` instance from an identifier string. */
  object fromName {
    /** Returns a `KeyUsage` instance from a string containing its identifier. */
    def apply(name: String): KeyState =
      unapply(name).getOrElse(throw new IllegalArgumentException(s"’$name‘ is not a valid key state."))

    /** Extracts a `KeyUsage` instance from a string containing its identifier. */
    def unapply(name: String): Option[KeyState] = values.find(_.name == name)
  }
}
